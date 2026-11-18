package com.mgmt.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应体，前端表格直接消费 { list, total, page, size }。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(long page, long size, List<T> list, long total) {
        PageResult<T> r = new PageResult<>();
        r.page = page;
        r.size = size;
        r.list = list;
        r.total = total;
        return r;
    }

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        return of(page.getCurrent(), page.getSize(), list, page.getTotal());
    }
}
