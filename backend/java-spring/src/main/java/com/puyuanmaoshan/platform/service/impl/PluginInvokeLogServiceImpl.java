package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.PluginInvokeLog;
import com.puyuanmaoshan.platform.mapper.PluginInvokeLogMapper;
import com.puyuanmaoshan.platform.service.PluginInvokeLogService;
import org.springframework.stereotype.Service;

@Service
public class PluginInvokeLogServiceImpl extends ServiceImpl<PluginInvokeLogMapper, PluginInvokeLog> implements PluginInvokeLogService {
}
