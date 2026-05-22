package com.puyuanmaoshan.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_login_log")
public class UserLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("login_time")
    private LocalDateTime loginTime;

    @TableField("login_ip")
    private String loginIp;

    @TableField("device_type")
    private String deviceType;

    @TableField("device_info")
    private String deviceInfo;

    @TableField("location")
    private String location;

    @TableField("is_success")
    private Boolean isSuccess;

    @TableField("fail_reason")
    private String failReason;
}
