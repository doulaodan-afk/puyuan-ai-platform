package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.RechargeOrder;
import com.puyuanmaoshan.platform.mapper.RechargeOrderMapper;
import com.puyuanmaoshan.platform.service.RechargeOrderService;
import org.springframework.stereotype.Service;

@Service
public class RechargeOrderServiceImpl extends ServiceImpl<RechargeOrderMapper, RechargeOrder> implements RechargeOrderService {
}
