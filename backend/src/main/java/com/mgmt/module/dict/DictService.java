package com.mgmt.module.dict;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.module.dict.dto.DictItemRequest;
import com.mgmt.module.dict.dto.DictTypeRequest;
import com.mgmt.module.dict.entity.SysDictItem;
import com.mgmt.module.dict.entity.SysDictType;
import com.mgmt.module.dict.mapper.DictItemMapper;
import com.mgmt.module.dict.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 数据字典：类型用于分组，字典项提供业务下拉数据。
 */
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictTypeMapper typeMapper;
    private final DictItemMapper itemMapper;

    public PageResult<SysDictType> pageTypes(long page, long size, String keyword) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysDictType::getName, keyword)
                    .or().like(SysDictType::getCode, keyword));
        }
        wrapper.orderByAsc(SysDictType::getId);

        IPage<SysDictType> result = typeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result, result.getRecords());
    }

    public SysDictType createType(DictTypeRequest request) {
        validateType(request);
        ensureCodeAvailable(request.getCode().trim(), null);

        SysDictType type = new SysDictType();
        type.setName(request.getName().trim());
        type.setCode(request.getCode().trim());
        type.setRemark(request.getRemark());
        typeMapper.insert(type);
        return type;
    }

    public SysDictType updateType(Long id, DictTypeRequest request) {
        SysDictType type = requireType(id);
        validateType(request);
        // code 会被业务代码直接引用，改了会让已有下拉数据取不到
        if (!type.getCode().equals(request.getCode().trim())) {
            throw new BizException(I18n.get("dict.codeImmutable"));
        }
        type.setName(request.getName().trim());
        type.setRemark(request.getRemark());
        typeMapper.updateById(type);
        return type;
    }

    @Transactional
    public void deleteType(Long id) {
        requireType(id);
        long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getTypeId, id));
        if (itemCount > 0) {
            throw new BizException(I18n.get("dict.hasItems", itemCount));
        }
        typeMapper.deleteById(id);
    }

    public List<SysDictItem> items(Long typeId) {
        requireType(typeId);
        return listItems(typeId);
    }

    /**
     * 按字典编码取字典项，供任意登录用户使用（如页面下拉框）。
     * 编码不存在时返回空列表，避免调用方额外处理 404。
     */
    public List<SysDictItem> itemsByCode(String code) {
        SysDictType type = typeMapper.selectOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getCode, code));
        if (type == null) {
            return List.of();
        }
        return listItems(type.getId());
    }

    public SysDictItem createItem(DictItemRequest request) {
        if (request.getTypeId() == null) {
            throw new BizException(I18n.get("dict.typeRequired"));
        }
        requireType(request.getTypeId());
        validateItem(request);
        ensureValueAvailable(request.getTypeId(), request.getValue().trim(), null);

        SysDictItem item = new SysDictItem();
        item.setTypeId(request.getTypeId());
        item.setLabel(request.getLabel().trim());
        item.setValue(request.getValue().trim());
        item.setSort(request.getSort() == null ? 0 : request.getSort());
        itemMapper.insert(item);
        return item;
    }

    public SysDictItem updateItem(Long id, DictItemRequest request) {
        SysDictItem item = requireItem(id);
        validateItem(request);
        ensureValueAvailable(item.getTypeId(), request.getValue().trim(), id);

        item.setLabel(request.getLabel().trim());
        item.setValue(request.getValue().trim());
        item.setSort(request.getSort() == null ? 0 : request.getSort());
        itemMapper.updateById(item);
        return item;
    }

    public void deleteItem(Long id) {
        requireItem(id);
        itemMapper.deleteById(id);
    }

    private List<SysDictItem> listItems(Long typeId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getTypeId, typeId)
                .orderByAsc(SysDictItem::getSort)
                .orderByAsc(SysDictItem::getId));
    }

    private void validateType(DictTypeRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(I18n.get("dict.nameRequired"));
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new BizException(I18n.get("dict.codeRequired"));
        }
        if (!request.getCode().trim().matches("^[a-z][a-z0-9_]*$")) {
            throw new BizException(I18n.get("dict.codeFormat"));
        }
    }

    private void validateItem(DictItemRequest request) {
        if (!StringUtils.hasText(request.getLabel())) {
            throw new BizException(I18n.get("dict.labelRequired"));
        }
        if (!StringUtils.hasText(request.getValue())) {
            throw new BizException(I18n.get("dict.valueRequired"));
        }
    }

    private void ensureCodeAvailable(String code, Long excludeId) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysDictType::getId, excludeId);
        }
        if (typeMapper.selectCount(wrapper) > 0) {
            throw new BizException(I18n.get("dict.codeExists"));
        }
    }

    private void ensureValueAvailable(Long typeId, String value, Long excludeId) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getTypeId, typeId)
                .eq(SysDictItem::getValue, value);
        if (excludeId != null) {
            wrapper.ne(SysDictItem::getId, excludeId);
        }
        if (itemMapper.selectCount(wrapper) > 0) {
            throw new BizException(I18n.get("dict.valueExists"));
        }
    }

    private SysDictType requireType(Long id) {
        SysDictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new BizException(404, I18n.get("dict.typeNotFound"));
        }
        return type;
    }

    private SysDictItem requireItem(Long id) {
        SysDictItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BizException(404, I18n.get("dict.itemNotFound"));
        }
        return item;
    }
}
