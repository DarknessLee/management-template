package com.mgmt.module.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.common.UserContext;
import com.mgmt.module.role.entity.SysRole;
import com.mgmt.module.role.mapper.SysRoleMapper;
import com.mgmt.module.user.dto.UserCreateRequest;
import com.mgmt.module.user.dto.UserUpdateRequest;
import com.mgmt.module.user.dto.UserVO;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // 包一层 and(...)，避免 or 条件把后面的查询条件"短路"掉
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        wrapper.orderByAsc(SysUser::getId);

        IPage<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> roleCodes = roleCodeMap();
        List<UserVO> list = result.getRecords().stream()
                .map(user -> toVO(user, roleCodes))
                .toList();
        return PageResult.of(result, list);
    }

    public UserVO get(Long id) {
        return toVO(requireUser(id), roleCodeMap());
    }

    @Transactional
    public UserVO create(UserCreateRequest req) {
        long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BizException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setRealName(req.getRealName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRoleId(requireRole(req.getRole()).getId());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.insert(user);

        return toVO(user, roleCodeMap());
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest req) {
        SysUser user = requireUser(id);

        if (StringUtils.hasText(req.getUsername()) && !Objects.equals(req.getUsername(), user.getUsername())) {
            long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername())
                    .ne(SysUser::getId, id));
            if (exists > 0) {
                throw new BizException("用户名已存在");
            }
            user.setUsername(req.getUsername());
        }
        if (req.getRealName() != null) {
            user.setRealName(req.getRealName());
        }
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        if (StringUtils.hasText(req.getRole())) {
            user.setRoleId(requireRole(req.getRole()).getId());
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }

        userMapper.updateById(user);
        return toVO(user, roleCodeMap());
    }

    @Transactional
    public void delete(Long id) {
        if (Objects.equals(id, UserContext.getUserId())) {
            throw new BizException("不能删除当前登录用户");
        }
        requireUser(id);
        // @TableLogic 生效：这里执行的是 UPDATE ... SET deleted = 1
        userMapper.deleteById(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private SysRole requireRole(String code) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
        if (role == null) {
            throw new BizException("角色不存在：" + code);
        }
        return role;
    }

    private Map<Long, String> roleCodeMap() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getCode));
    }

    private UserVO toVO(SysUser user, Map<Long, String> roleCodes) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRole(roleCodes.getOrDefault(user.getRoleId(), ""));
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
