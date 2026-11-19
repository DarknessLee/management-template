package com.mgmt.module.profile;

import com.mgmt.common.BizException;
import com.mgmt.common.UserContext;
import com.mgmt.module.profile.dto.ChangePasswordRequest;
import com.mgmt.module.profile.dto.ProfileVO;
import com.mgmt.module.profile.dto.UpdateProfileRequest;
import com.mgmt.module.auth.AuthService;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final String AVATAR_URL_PREFIX = "/uploads/avatar/";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public ProfileVO get() {
        return toVo(currentUser());
    }

    @Transactional
    public ProfileVO update(UpdateProfileRequest request) {
        SysUser user = currentUser();
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        userMapper.updateById(user);
        return toVo(user);
    }

    /**
     * 修改密码后让该用户所有已签发 token 失效：
     * 密码是身份凭证，改了密码却让旧会话继续有效，等于密码没有真正"换掉"。
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        SysUser user = currentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BizException(400, "新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        authService.invalidateAllSessions(user.getId());
    }

    @Transactional
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException(400, "头像大小不能超过 2MB");
        }
        // 只按服务端识别的 MIME 决定后缀，不使用客户端文件名：
        // 客户端文件名可能带 ../ 或伪装扩展名，直接拼路径会造成目录穿越或存储型 XSS
        String extension = switch (file.getContentType() == null ? "" : file.getContentType()) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> throw new BizException(400, "仅支持 png、jpg、gif、webp 格式的图片");
        };

        try {
            Path dir = Paths.get(uploadDir, "avatar").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            file.transferTo(dir.resolve(filename).toFile());

            SysUser user = currentUser();
            deleteOldAvatar(user.getAvatar());
            String url = AVATAR_URL_PREFIX + filename;
            user.setAvatar(url);
            userMapper.updateById(user);
            return url;
        } catch (IOException e) {
            throw new BizException(500, "头像保存失败，请稍后重试");
        }
    }

    private void deleteOldAvatar(String oldAvatar) {
        if (oldAvatar == null || !oldAvatar.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }
        // 取纯文件名再拼接，避免数据库里的历史值被篡改成 ../ 路径
        String filename = Paths.get(oldAvatar).getFileName().toString();
        Path oldFile = Paths.get(uploadDir, "avatar").toAbsolutePath().normalize().resolve(filename);
        try {
            Files.deleteIfExists(oldFile);
        } catch (IOException ignored) {
            // 旧文件删不掉不影响新头像生效，最多留下一个孤儿文件
        }
    }

    /**
     * 当前用户永远从 token 解析，不接受请求参数里的 id。
     * 一旦让前端传 userId，攻击者改个数字就能改别人的密码或资料（越权 / IDOR）。
     */
    private SysUser currentUser() {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BizException(401, "登录已过期，请重新登录");
        }
        return user;
    }

    private ProfileVO toVo(SysUser user) {
        ProfileVO vo = new ProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
