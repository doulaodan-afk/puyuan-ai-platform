package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.Plugin;
import com.puyuanmaoshan.platform.mapper.PluginMapper;
import com.puyuanmaoshan.platform.service.PluginService;
import org.springframework.stereotype.Service;

@Service
public class PluginServiceImpl extends ServiceImpl<PluginMapper, Plugin> implements PluginService {
}
