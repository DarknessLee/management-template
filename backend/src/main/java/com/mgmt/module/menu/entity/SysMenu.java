package com.mgmt.module.menu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 侧边栏菜单：permission 为空表示登录即可见，否则需具备对应权限。
 * component 为空表示目录（只用于分组），有值表示可访问的页面菜单。
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级菜单ID，0 表示顶级 */
    private Long parentId;

    private String title;

    /** 英文名称，语言切换为英文时展示，为空则回退中文名称 */
    private String titleEn;

    private String path;

    /** 前端视图路径，如 views/users/index.vue；目录为空 */
    private String component;

    /** Element Plus 图标组件名，如 User */
    private String icon;

    private String permission;

    private Integer sort;

    /** 1 显示 0 隐藏 */
    private Integer visible;

    /** 子菜单，不入库，仅用于接口返回树形结构 */
    @TableField(exist = false)
    private List<SysMenu> children;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
