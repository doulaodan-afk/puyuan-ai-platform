package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.IdempotencyRecord;
import com.puyuanmaoshan.platform.mapper.IdempotencyRecordMapper;
import com.puyuanmaoshan.platform.service.IdempotencyRecordService;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyRecordServiceImpl extends ServiceImpl<IdempotencyRecordMapper, IdempotencyRecord> implements IdempotencyRecordService {
}
