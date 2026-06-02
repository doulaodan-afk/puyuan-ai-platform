package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.UserDtos;
import com.puyuanmaoshan.platform.entity.UserAccount;

public interface UserProfileService {

    /**
     * 获取用户个人信息
     */
    UserDtos.UserProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户个人信息
     */
    void updateProfile(Long userId, UserDtos.UpdateProfileRequest request);

    /**
     * 绑定/更换手机号
     */
    void bindPhone(Long userId, UserDtos.BindPhoneRequest request);

    /**
     * 绑定微信
     */
    void bindWechat(Long userId, UserDtos.BindWechatRequest request);

    /**
     * 解绑微信
     */
    void unbindWechat(Long userId);

    /**
     * 上传头像
     */
    String uploadAvatar(Long userId, byte[] imageBytes, String fileName);

    /**
     * 根据手机号查找用户
     */
    UserAccount findByPhone(String phone);

    /**
     * 根据邮箱查找用户
     */
    UserAccount findByEmail(String email);

    /**
     * 根据微信 OpenID 查找用户
     */
    UserAccount findByWechatOpenid(String openid);

    /**
     * 检查手机号是否已被其他用户使用
     */
    boolean isPhoneUsedByOther(String phone, Long excludeUserId);

    /**
     * 检查邮箱是否已被其他用户使用
     */
    boolean isEmailUsedByOther(String email, Long excludeUserId);
}