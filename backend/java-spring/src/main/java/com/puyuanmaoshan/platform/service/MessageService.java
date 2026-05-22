package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;

import java.util.List;

public interface MessageService {

    // 获取消息列表
    DesignAssistantDtos.MessageListResponse getMessageList(Long userId, String type, boolean unreadOnly, int page, int size);

    // 标记消息已读
    DesignAssistantDtos.CommonResponse markMessageRead(Long messageId, Long userId);

    // 发送消息
    void sendMessage(Long receiverId, String title, String content, String type, Long relatedId);

    // 发送批量消息
    void sendBatchMessages(List<Long> receiverIds, String title, String content, String type, Long relatedId);

    // 获取未读消息数
    int getUnreadCount(Long userId);
}