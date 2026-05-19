package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.mapper.TenantMapper;
import com.puyuanmaoshan.platform.service.TenantService;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
}
