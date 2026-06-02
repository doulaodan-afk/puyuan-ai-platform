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
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("type")
    private String type; // system/task/remind

    @TableField("is_read")
    private Integer isRead;

    @TableField("related_id")
    private Long relatedId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}