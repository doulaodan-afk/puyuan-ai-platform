package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecord> {
}
