package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.puyuanmaoshan.platform.dto.UserDtos;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.mapper.UserAccountMapper;
import com.puyuanmaoshan.platform.service.OssService;
import com.puyuanmaoshan.platform.service.WxLoginService;
import com.puyuanmaoshan.platform.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserAccountMapper userAccountMapper;
    private final OssService ossService;
    private final WxLoginService wxLoginService;

    public UserProfileServiceImpl(UserAccountMapper userAccountMapper,
                                  OssService ossService,
                                  @Lazy WxLoginService wxLoginService) {
        this.userAccountMapper = userAccountMapper;
        this.ossService = ossService;
        this.wxLoginService = wxLoginService;
    }

    @Override
    public UserDtos.UserProfileResponse getUserProfile(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        return new UserDtos.UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getMobile(),
                user.getPhone(),
                user.getEmail(),
                user.getWechatOpenid() != null && !user.getWechatOpenid().isEmpty(),
                user.getWechatOpenid(),
                user.getWechatUnionid(),
                user.getRoleCode(),
                user.getStatus()
        );
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserDtos.UpdateProfileRequest request) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        user.setNickname(request.nickname());
        if (request.avatarUrl() != null && !request.avatarUrl().isEmpty()) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.email() != null && !request.email().isEmpty()) {
            if (isEmailUsedByOther(request.email(), userId)) {
                throw new AppException(ErrorCode.CONFLICT, "Email already used by another user");
            }
            user.setEmail(request.email());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        logger.info("User {} updated profile", userId);
    }

    @Override
    @Transactional
    public void bindPhone(Long userId, UserDtos.BindPhoneRequest request) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        // TODO: Verify SMS code
        // verifySmsCode(request.phone(), request.verifyCode());

        if (isPhoneUsedByOther(request.phone(), userId)) {
            throw new AppException(ErrorCode.CONFLICT, "Phone already used by another user");
        }

        user.setPhone(request.phone());
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        logger.info("User {} bound phone {}", userId, request.phone());
    }

    @Override
    @Transactional
    public void bindWechat(Long userId, UserDtos.BindWechatRequest request) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        // 通过 WxLoginService 使用 code 换取真实 openId
        String openid = wxLoginService.getOpenIdByCode(request.code());
        if (openid == null || openid.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "获取微信 openId 失败，请重新授权");
        }

        // Check if this openid is bound to another user
        UserAccount existingUser = findByWechatOpenid(openid);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new AppException(ErrorCode.CONFLICT, "WeChat account already bound to another user");
        }

        user.setWechatOpenid(openid);
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        logger.info("User {} bound WeChat with openId {}", userId, openid);
    }

    @Override
    @Transactional
    public void unbindWechat(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        user.setWechatOpenid(null);
        user.setWechatUnionid(null);
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        logger.info("User {} unbound WeChat", userId);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, byte[] imageBytes, String fileName) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        // Upload to OSS
        String objectKey = "avatars/" + userId + "/" + System.currentTimeMillis() + "_" + fileName;
        String url = ossService.uploadBytes(imageBytes, objectKey);

        // Update user avatar
        user.setAvatarUrl(url);
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);

        logger.info("User {} uploaded avatar: {}", userId, url);
        return url;
    }

    @Override
    public UserAccount findByPhone(String phone) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getPhone, phone);
        wrapper.eq(UserAccount::getStatus, 1);
        return userAccountMapper.selectOne(wrapper);
    }

    @Override
    public UserAccount findByEmail(String email) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getEmail, email);
        wrapper.eq(UserAccount::getStatus, 1);
        return userAccountMapper.selectOne(wrapper);
    }

    @Override
    public UserAccount findByWechatOpenid(String openid) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getWechatOpenid, openid);
        wrapper.eq(UserAccount::getStatus, 1);
        return userAccountMapper.selectOne(wrapper);
    }

    @Override
    public boolean isPhoneUsedByOther(String phone, Long excludeUserId) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getPhone, phone);
        wrapper.eq(UserAccount::getStatus, 1);
        wrapper.ne(UserAccount::getId, excludeUserId);
        return userAccountMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isEmailUsedByOther(String email, Long excludeUserId) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getEmail, email);
        wrapper.eq(UserAccount::getStatus, 1);
        wrapper.ne(UserAccount::getId, excludeUserId);
        return userAccountMapper.selectCount(wrapper) > 0;
    }
}