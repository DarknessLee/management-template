package com.mgmt.module.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.common.UserContext;
import com.mgmt.module.file.dto.FileVO;
import com.mgmt.module.file.entity.SysFile;
import com.mgmt.module.file.mapper.SysFileMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传、下载与删除。
 * 上传文件统一落在 uploads/files/yyyyMM/ 下，数据库只保存相对路径，
 * 下载时再做一次路径归属校验，防止历史脏数据造成目录穿越。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    private static final String STORAGE_DIR = "files";
    private static final String URL_PREFIX = "/uploads/files/";
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 只放行常见文档与图片，明确拒绝 html/svg/js 等可被浏览器执行的文件，
     * 避免上传后通过静态地址访问形成存储型 XSS。
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "csv", "zip", "rar", "7z");

    private final SysFileMapper fileMapper;
    private final SysUserMapper userMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public PageResult<FileVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysFile::getOriginalName, keyword.trim());
        }
        wrapper.orderByDesc(SysFile::getId);

        IPage<SysFile> result = fileMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result, result.getRecords().stream().map(this::toVO).toList());
    }

    public FileVO store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(400, "文件大小不能超过 20MB");
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(400, "不支持的文件类型，仅支持图片、PDF、Office 文档、文本和压缩包");
        }

        String month = MONTH_FORMAT.format(LocalDate.now());
        String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Path dir = Paths.get(uploadDir, STORAGE_DIR, month).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            // 存储文件名完全由服务端生成，不使用客户端文件名，避免目录穿越与同名覆盖
            file.transferTo(dir.resolve(storedFilename).toFile());

            SysFile entity = new SysFile();
            entity.setOriginalName(originalName);
            entity.setStoredName(month + "/" + storedFilename);
            entity.setUrl(URL_PREFIX + month + "/" + storedFilename);
            entity.setSize(file.getSize());
            entity.setContentType(file.getContentType());
            fillUploader(entity);
            fileMapper.insert(entity);
            return toVO(entity);
        } catch (IOException e) {
            throw new BizException(500, "文件保存失败，请稍后重试");
        }
    }

    public FileDownload download(Long id) {
        SysFile file = requireFile(id);
        Path path = resolveStoredPath(file.getStoredName());
        if (!Files.exists(path)) {
            throw new BizException(404, "文件已不存在");
        }
        try {
            String contentType = file.getContentType() == null
                    ? "application/octet-stream" : file.getContentType();
            return new FileDownload(Files.readAllBytes(path), file.getOriginalName(), contentType);
        } catch (IOException e) {
            throw new BizException(500, "文件读取失败，请稍后重试");
        }
    }

    public void delete(Long id) {
        SysFile file = requireFile(id);
        fileMapper.deleteById(id);
        try {
            // 物理文件删除失败不影响记录删除，最多留下一个孤儿文件
            Files.deleteIfExists(resolveStoredPath(file.getStoredName()));
        } catch (IOException ignored) {
            // ignored
        }
    }

    public record FileDownload(byte[] content, String filename, String contentType) {
    }

    private void fillUploader(SysFile entity) {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            return;
        }
        entity.setUploaderId(user.getId());
        entity.setUploaderName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
    }

    private SysFile requireFile(Long id) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BizException(404, "文件不存在");
        }
        return file;
    }

    /**
     * 只取文件名并限制在 files 根目录内，数据库里的 storedName 被篡改也不会读到其他目录。
     */
    private Path resolveStoredPath(String storedName) {
        Path base = Paths.get(uploadDir, STORAGE_DIR).toAbsolutePath().normalize();
        Path path = base.resolve(storedName).normalize();
        if (!path.startsWith(base)) {
            throw new BizException(400, "文件路径非法");
        }
        return path;
    }

    private String sanitizeFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "未命名文件";
        }
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            return "未命名文件";
        }
        String extension = extensionOf(name);
        if (name.length() > 200) {
            name = name.substring(0, 200 - extension.length() - 1) + "." + extension;
        }
        return name;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    private FileVO toVO(SysFile file) {
        FileVO vo = new FileVO();
        vo.setId(file.getId());
        vo.setOriginalName(file.getOriginalName());
        vo.setUrl(file.getUrl());
        vo.setSize(file.getSize());
        vo.setContentType(file.getContentType());
        vo.setUploaderName(file.getUploaderName());
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }
}
