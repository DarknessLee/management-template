package com.mgmt.module.dept;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.BizException;
import com.mgmt.module.dept.dto.DeptRequest;
import com.mgmt.module.dept.dto.DeptVO;
import com.mgmt.module.dept.entity.SysDept;
import com.mgmt.module.dept.mapper.SysDeptMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门树维护：parentId 为 0 表示顶级部门。
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public List<DeptVO> tree() {
        Map<Long, DeptVO> voMap = new LinkedHashMap<>();
        listAll().forEach(dept -> voMap.put(dept.getId(), toVO(dept)));

        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO vo : voMap.values()) {
            DeptVO parent = voMap.get(vo.getParentId());
            if (vo.getParentId() == null || vo.getParentId() == 0 || parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    /**
     * 扁平列表，供用户表单的部门下拉使用，登录用户均可读取。
     */
    public List<DeptVO> options() {
        return listAll().stream().map(this::toVO).toList();
    }

    public DeptVO create(DeptRequest request) {
        String name = requireName(request.getName());
        long parentId = normalizeParent(request.getParentId());
        requireParent(parentId);

        SysDept dept = new SysDept();
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setLeader(trimToNull(request.getLeader()));
        dept.setPhone(trimToNull(request.getPhone()));
        dept.setSort(request.getSort() == null ? 0 : request.getSort());
        deptMapper.insert(dept);
        return toVO(dept);
    }

    public DeptVO update(Long id, DeptRequest request) {
        SysDept dept = requireDept(id);
        String name = requireName(request.getName());
        long parentId = normalizeParent(request.getParentId());

        if (parentId == id) {
            throw new BizException(I18n.get("dept.parentSelf"));
        }
        requireParent(parentId);
        ensureNotDescendant(id, parentId);

        dept.setParentId(parentId);
        dept.setName(name);
        dept.setLeader(trimToNull(request.getLeader()));
        dept.setPhone(trimToNull(request.getPhone()));
        dept.setSort(request.getSort() == null ? 0 : request.getSort());
        deptMapper.updateById(dept);
        return toVO(dept);
    }

    @Transactional
    public void delete(Long id) {
        requireDept(id);
        long children = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BizException(I18n.get("dept.deleteChildrenFirst"));
        }
        long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BizException(I18n.get("dept.hasUsers", users));
        }
        deptMapper.deleteById(id);
    }

    private List<SysDept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId));
    }

    private SysDept requireDept(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(404, I18n.get("dept.notFound"));
        }
        return dept;
    }

    private void requireParent(long parentId) {
        if (parentId != 0 && deptMapper.selectById(parentId) == null) {
            throw new BizException(I18n.get("dept.parentNotFound"));
        }
    }

    /**
     * 逐级向上检查，避免把部门挂到自己的子孙节点下形成环。
     */
    private void ensureNotDescendant(Long id, long parentId) {
        long cursor = parentId;
        int depth = 0;
        while (cursor != 0 && depth++ < 100) {
            if (cursor == id) {
                throw new BizException(I18n.get("dept.parentDescendant"));
            }
            SysDept parent = deptMapper.selectById(cursor);
            if (parent == null) {
                return;
            }
            cursor = normalizeParent(parent.getParentId());
        }
    }

    private long normalizeParent(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(I18n.get("dept.nameRequired"));
        }
        return name.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private DeptVO toVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setName(dept.getName());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setSort(dept.getSort());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}
