package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.TenantPlugin;
import com.puyuanmaoshan.platform.mapper.TenantPluginMapper;
import com.puyuanmaoshan.platform.service.TenantPluginService;
import org.springframework.stereotype.Service;

@Service
public class TenantPluginServiceImpl extends ServiceImpl<TenantPluginMapper, TenantPlugin> implements TenantPluginService {
}
