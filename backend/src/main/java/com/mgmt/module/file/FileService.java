package com.mgmt.module.file;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.common.UserContext;
import com.mgmt.module.file.dto.FileStatsVO;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /** 列表筛选使用的文件分类 */
    private static final Map<String, List<String>> TYPE_EXTENSIONS = Map.of(
            "image", List.of("png", "jpg", "jpeg", "gif", "webp", "bmp"),
            "pdf", List.of("pdf"),
            "office", List.of("doc", "docx", "xls", "xlsx", "ppt", "pptx"),
            "text", List.of("txt", "md", "csv"),
            "archive", List.of("zip", "rar", "7z"));

    private final SysFileMapper fileMapper;
    private final SysUserMapper userMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public PageResult<FileVO> page(long page, long size, String keyword, String type) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysFile::getOriginalName, keyword.trim());
        }
        applyTypeFilter(wrapper, type);
        wrapper.orderByDesc(SysFile::getId);

        IPage<SysFile> result = fileMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result, result.getRecords().stream().map(this::toVO).toList());
    }

    public FileStatsVO stats() {
        FileStatsVO vo = new FileStatsVO();

        QueryWrapper<SysFile> sumWrapper = new QueryWrapper<>();
        sumWrapper.select("COUNT(*) AS cnt", "COALESCE(SUM(size), 0) AS total_size");
        Map<String, Object> row = fileMapper.selectMaps(sumWrapper).get(0);
        long total = ((Number) row.get("cnt")).longValue();
        vo.setTotalFiles(total);
        vo.setTotalSize(((Number) row.get("total_size")).longValue());

        List<FileStatsVO.TypeCount> types = new ArrayList<>();
        long categorized = 0;
        for (Map.Entry<String, List<String>> entry : TYPE_EXTENSIONS.entrySet()) {
            LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
            applyTypeFilter(wrapper, entry.getKey());
            long count = fileMapper.selectCount(wrapper);
            if (count > 0) {
                types.add(new FileStatsVO.TypeCount(entry.getKey(), count));
                categorized += count;
            }
        }
        long other = Math.max(0, total - categorized);
        if (other > 0) {
            types.add(new FileStatsVO.TypeCount("other", other));
        }
        vo.setTypes(types.stream()
                .sorted(Comparator.comparingLong(FileStatsVO.TypeCount::getCount).reversed())
                .limit(3)
                .toList());
        return vo;
    }

    public FileVO store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, I18n.get("file.uploadRequired"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(400, I18n.get("file.tooLarge"));
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(400, I18n.get("file.typeUnsupported"));
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
            throw new BizException(500, I18n.get("file.saveFailed"));
        }
    }

    public FileDownload download(Long id) {
        SysFile file = requireFile(id);
        Path path = resolveStoredPath(file.getStoredName());
        if (!Files.exists(path)) {
            throw new BizException(404, I18n.get("file.gone"));
        }
        try {
            String contentType = file.getContentType() == null
                    ? "application/octet-stream" : file.getContentType();
            return new FileDownload(Files.readAllBytes(path), file.getOriginalName(), contentType);
        } catch (IOException e) {
            throw new BizException(500, I18n.get("file.readFailed"));
        }
    }

    public void delete(Long id) {
        remove(requireFile(id));
    }

    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(400, I18n.get("file.deleteRequired"));
        }
        // 已被其他管理员删除的记录直接跳过，不让整批操作失败
        ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(fileMapper::selectById)
                .filter(Objects::nonNull)
                .forEach(this::remove);
    }

    private void remove(SysFile file) {
        fileMapper.deleteById(file.getId());
        try {
            // 物理文件删除失败不影响记录删除，最多留下一个孤儿文件
            Files.deleteIfExists(resolveStoredPath(file.getStoredName()));
        } catch (IOException ignored) {
            // ignored
        }
    }

    private void applyTypeFilter(LambdaQueryWrapper<SysFile> wrapper, String type) {
        if (!StringUtils.hasText(type)) {
            return;
        }
        List<String> extensions = TYPE_EXTENSIONS.get(type.trim());
        if (extensions == null) {
            return;
        }
        wrapper.and(condition -> {
            for (int i = 0; i < extensions.size(); i++) {
                if (i == 0) {
                    condition.like(SysFile::getOriginalName, "." + extensions.get(0));
                } else {
                    condition.or().like(SysFile::getOriginalName, "." + extensions.get(i));
                }
            }
        });
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
            throw new BizException(404, I18n.get("file.notFound"));
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
            throw new BizException(400, I18n.get("file.pathInvalid"));
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
