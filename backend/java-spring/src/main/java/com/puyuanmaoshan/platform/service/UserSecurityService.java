package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.UserDtos.*;

public interface UserSecurityService {

    void changePassword(Long userId, ChangePasswordRequest request);

    AccountSecurityResponse getAccountSecurity(Long userId);

    void updateSecuritySettings(Long userId, SecuritySettingsRequest request);

    LoginLogListResponse getLoginLogs(Long userId, Integer page, Integer pageSize);
}
