package com.mgmt.module.setting.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_setting")
public class SysSetting {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    /** 配置内容以 JSON 字符串整块存储，前端按分组读写 */
    private String settingJson;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
