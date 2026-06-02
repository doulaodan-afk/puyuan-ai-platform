package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}