package com.mgmt.module.user;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.common.UserContext;
import com.mgmt.module.dept.entity.SysDept;
import com.mgmt.module.dept.mapper.SysDeptMapper;
import com.mgmt.module.role.DataScopeService;
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
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeService dataScopeService;

    public PageResult<UserVO> page(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = searchWrapper(keyword);
        wrapper.orderByAsc(SysUser::getId);

        IPage<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> roleCodes = roleCodeMap();
        Map<Long, String> deptNames = deptNameMap();
        List<UserVO> list = result.getRecords().stream()
                .map(user -> toVO(user, roleCodes, deptNames))
                .toList();
        return PageResult.of(result, list);
    }

    /**
     * 导出用：不分页，条件与列表页保持一致。
     */
    public List<UserVO> listForExport(String keyword) {
        Map<Long, String> roleCodes = roleCodeMap();
        Map<Long, String> deptNames = deptNameMap();
        return userMapper.selectList(searchWrapper(keyword).orderByAsc(SysUser::getId))
                .stream()
                .map(user -> toVO(user, roleCodes, deptNames))
                .toList();
    }

    private LambdaQueryWrapper<SysUser> searchWrapper(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // 包一层 and(...)，避免 or 条件把后面的查询条件"短路"掉
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        // 列表和导出共用这里，数据权限只需维护一处
        DataScopeService.DataScope scope = dataScopeService.current();
        if (scope.selfOnly()) {
            wrapper.eq(SysUser::getId, scope.userId() == null ? -1L : scope.userId());
        } else if (!scope.all()) {
            wrapper.in(SysUser::getDeptId, scope.deptIds().isEmpty() ? List.of(-1L) : scope.deptIds());
        }
        return wrapper;
    }

    public UserVO get(Long id) {
        return toVO(requireUser(id), roleCodeMap(), deptNameMap());
    }

    @Transactional
    public UserVO create(UserCreateRequest req) {
        long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BizException(I18n.get("user.usernameExists"));
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setRealName(req.getRealName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRoleId(requireRole(req.getRole()).getId());
        Long deptId = requireDeptId(req.getDeptId());
        ensureDeptVisible(deptId);
        user.setDeptId(deptId);
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.insert(user);

        return toVO(user, roleCodeMap(), deptNameMap());
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest req) {
        SysUser user = requireUser(id);

        if (StringUtils.hasText(req.getUsername()) && !Objects.equals(req.getUsername(), user.getUsername())) {
            long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername())
                    .ne(SysUser::getId, id));
            if (exists > 0) {
                throw new BizException(I18n.get("user.usernameExists"));
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
        if (req.getDeptId() != null) {
            Long deptId = requireDeptId(req.getDeptId());
            ensureDeptVisible(deptId);
            user.setDeptId(deptId);
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }

        userMapper.updateById(user);
        return toVO(user, roleCodeMap(), deptNameMap());
    }

    @Transactional
    public void delete(Long id) {
        if (Objects.equals(id, UserContext.getUserId())) {
            throw new BizException(I18n.get("user.cannotDeleteSelf"));
        }
        requireUser(id);
        // @TableLogic 生效：这里执行的是 UPDATE ... SET deleted = 1
        userMapper.deleteById(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, I18n.get("user.notFound"));
        }
        if (!dataScopeService.current().canSeeUser(user.getId(), user.getDeptId())) {
            throw new BizException(403, I18n.get("user.forbidden"));
        }
        return user;
    }

    /**
     * 数据范围受限的用户只能把用户放到自己可见的部门下。
     */
    private void ensureDeptVisible(Long deptId) {
        DataScopeService.DataScope scope = dataScopeService.current();
        if (scope.all()) {
            return;
        }
        if (!scope.deptLimited() || !scope.deptIds().contains(deptId)) {
            throw new BizException(403, I18n.get("user.deptForbidden"));
        }
    }

    private SysRole requireRole(String code) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
        if (role == null) {
            throw new BizException(I18n.get("user.roleNotFound", code));
        }
        return role;
    }

    private Map<Long, String> roleCodeMap() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getCode));
    }

    private Map<Long, String> deptNameMap() {
        return deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
    }

    private Long requireDeptId(Long deptId) {
        if (deptId == null || deptId == 0) {
            return 0L;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BizException(I18n.get("dept.notFound"));
        }
        return deptId;
    }

    private UserVO toVO(SysUser user, Map<Long, String> roleCodes, Map<Long, String> deptNames) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRole(roleCodes.getOrDefault(user.getRoleId(), ""));
        vo.setDeptId(user.getDeptId() == null ? 0L : user.getDeptId());
        vo.setDeptName(deptNames.getOrDefault(vo.getDeptId(), ""));
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
