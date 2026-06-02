package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puyuanmaoshan.platform.dto.DesignAssistantDtos;
import com.puyuanmaoshan.platform.entity.Message;
import com.puyuanmaoshan.platform.mapper.MessageMapper;
import com.puyuanmaoshan.platform.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public DesignAssistantDtos.MessageListResponse getMessageList(Long userId, String type, boolean unreadOnly, int page, int size) {
        try {
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getReceiverId, userId);

            if (type != null && !type.isEmpty()) {
                wrapper.eq(Message::getType, type);
            }
            if (unreadOnly) {
                wrapper.eq(Message::getIsRead, 0);
            }
            wrapper.orderByDesc(Message::getCreatedAt);

            IPage<Message> pageObj = messageMapper.selectPage(new Page<>(page, size), wrapper);

            List<DesignAssistantDtos.MessageInfo> result = new ArrayList<>();
            for (Message msg : pageObj.getRecords()) {
                result.add(new DesignAssistantDtos.MessageInfo(
                    msg.getId(), msg.getReceiverId(), msg.getSenderId(),
                    msg.getTitle(), msg.getContent(), msg.getType(),
                    msg.getIsRead() == 1, msg.getRelatedId(), msg.getCreatedAt()
                ));
            }

            return new DesignAssistantDtos.MessageListResponse(result, pageObj.getTotal(), page, size);

        } catch (Exception e) {
            logger.error("Get message list failed", e);
            throw new RuntimeException("获取消息列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DesignAssistantDtos.CommonResponse markMessageRead(Long messageId, Long userId) {
        try {
            Message msg = messageMapper.selectById(messageId);
            if (msg == null || !msg.getReceiverId().equals(userId)) {
                return DesignAssistantDtos.ResponseHelper.error("消息不存在或无权操作");
            }

            msg.setIsRead(1);
            messageMapper.updateById(msg);

            return DesignAssistantDtos.ResponseHelper.success();

        } catch (Exception e) {
            logger.error("Mark message read failed", e);
            return DesignAssistantDtos.ResponseHelper.error("操作失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void sendMessage(Long receiverId, String title, String content, String type, Long relatedId) {
        try {
            Message msg = Message.builder()
                    .receiverId(receiverId)
                    .senderId(0L) // 系统消息
                    .title(title)
                    .content(content)
                    .type(type)
                    .isRead(0)
                    .relatedId(relatedId != null ? relatedId : 0L)
                    .createdAt(LocalDateTime.now())
                    .build();

            messageMapper.insert(msg);
            logger.info("Sent message to user {}: {}", receiverId, title);

        } catch (Exception e) {
            logger.error("Send message failed", e);
        }
    }

    @Override
    @Transactional
    public void sendBatchMessages(List<Long> receiverIds, String title, String content, String type, Long relatedId) {
        try {
            for (Long receiverId : receiverIds) {
                sendMessage(receiverId, title, content, type, relatedId);
            }
        } catch (Exception e) {
            logger.error("Send batch messages failed", e);
        }
    }

    @Override
    public int getUnreadCount(Long userId) {
        try {
            Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                    .eq(Message::getReceiverId, userId)
                    .eq(Message::getIsRead, 0)
            );
            return count != null ? count.intValue() : 0;

        } catch (Exception e) {
            logger.error("Get unread count failed", e);
            return 0;
        }
    }
}