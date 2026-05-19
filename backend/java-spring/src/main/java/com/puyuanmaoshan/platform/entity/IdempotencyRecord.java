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
@TableName("idempotency_record")
public class IdempotencyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("scope")
    private String scope;

    @TableField("request_hash")
    private String requestHash;

    @TableField("response_body")
    private String responseBody;

    @TableField("status")
    private String status;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
