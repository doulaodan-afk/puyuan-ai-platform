package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.mapper.UserAccountMapper;
import com.puyuanmaoshan.platform.service.UserAccountService;
import org.springframework.stereotype.Service;

@Service
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {
}
