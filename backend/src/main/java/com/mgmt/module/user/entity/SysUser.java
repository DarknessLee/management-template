package com.mgmt.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 永远不返回给前端，防止密码哈希泄露 */
    @JsonIgnore
    private String password;

    /** 密码最近修改时间，密码策略有效期从这里开始计算 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime passwordUpdateTime;

    /** 1 表示下次登录必须先修改密码（如管理员批量重置临时密码） */
    private Integer mustChangePassword;

    private String realName;

    private String email;

    private String phone;

    /** 头像相对路径，如 /uploads/avatar/xxx.png */
    private String avatar;

    private Long roleId;

    /** 所属部门ID，0 表示未分配 */
    private Long deptId;

    /** 1 启用 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
