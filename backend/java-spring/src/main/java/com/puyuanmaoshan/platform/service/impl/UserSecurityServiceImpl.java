package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puyuanmaoshan.platform.dto.UserDtos.*;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.entity.UserLoginLog;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.mapper.UserAccountMapper;
import com.puyuanmaoshan.platform.mapper.UserLoginLogMapper;
import com.puyuanmaoshan.platform.service.UserSecurityService;
import com.puyuanmaoshan.platform.util.PasswordEncoderUtil;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class UserSecurityServiceImpl implements UserSecurityService {

    private final UserAccountMapper userAccountMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final PasswordEncoderUtil passwordEncoder;

    public UserSecurityServiceImpl(UserAccountMapper userAccountMapper,
                                  UserLoginLogMapper userLoginLogMapper) {
        this.userAccountMapper = userAccountMapper;
        this.userLoginLogMapper = userLoginLogMapper;
        this.passwordEncoder = new PasswordEncoderUtil();
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "原密码错误");
            }
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "两次输入的密码不一致");
        }

        if (request.newPassword().length() < 6) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "新密码长度不能少于6位");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userAccountMapper.updateById(user);
    }

    @Override
    public AccountSecurityResponse getAccountSecurity(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        LambdaQueryWrapper<UserLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLoginLog::getUserId, userId)
               .eq(UserLoginLog::getIsSuccess, true)
               .orderByDesc(UserLoginLog::getLoginTime);
        List<UserLoginLog> successLogs = userLoginLogMapper.selectList(wrapper);

        UserLoginLog lastLogin = successLogs.isEmpty() ? null : successLogs.get(0);

        return new AccountSecurityResponse(
                user.getPassword() != null && !user.getPassword().isBlank(),
                user.getPhone() != null && !user.getPhone().isBlank(),
                user.getWechatOpenid() != null && !user.getWechatOpenid().isBlank(),
                user.getEmail() != null && !user.getEmail().isBlank(),
                lastLogin != null ? lastLogin.getLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null,
                lastLogin != null ? lastLogin.getLoginIp() : null,
                successLogs.size()
        );
    }

    @Override
    public void updateSecuritySettings(Long userId, SecuritySettingsRequest request) {
        // 占位实现 - 可以后续扩展为存储到用户设置表
    }

    @Override
    public LoginLogListResponse getLoginLogs(Long userId, Integer page, Integer pageSize) {
        if (page == null) page = 1;
        if (pageSize == null) pageSize = 20;

        Page<UserLoginLog> pageResult = userLoginLogMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserLoginLog>()
                        .eq(UserLoginLog::getUserId, userId)
                        .orderByDesc(UserLoginLog::getLoginTime)
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<LoginLogResponse> logs = pageResult.getRecords().stream()
                .map(log -> new LoginLogResponse(
                        log.getId(),
                        log.getLoginTime() != null ? log.getLoginTime().format(formatter) : null,
                        log.getLoginIp(),
                        log.getDeviceType(),
                        log.getDeviceInfo(),
                        log.getLocation(),
                        log.getIsSuccess(),
                        log.getFailReason()
                ))
                .toList();

        return new LoginLogListResponse(logs, pageResult.getTotal());
    }

    public void recordLoginLog(Long userId, String loginIp, String deviceType,
                               String deviceInfo, String location, boolean isSuccess, String failReason) {
        UserLoginLog log = UserLoginLog.builder()
                .userId(userId)
                .loginIp(loginIp)
                .deviceType(deviceType)
                .deviceInfo(deviceInfo)
                .location(location)
                .isSuccess(isSuccess)
                .failReason(failReason)
                .build();
        userLoginLogMapper.insert(log);
    }
}
