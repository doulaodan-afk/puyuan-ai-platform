package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.TenantDtos;
import com.puyuanmaoshan.platform.dto.UserDtos;
import com.puyuanmaoshan.platform.entity.MemberRoleAuditLog;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.TenantRoleConfig;
import com.puyuanmaoshan.platform.entity.TenantUser;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import com.puyuanmaoshan.platform.exception.AppException;
import com.puyuanmaoshan.platform.mapper.MemberRoleAuditLogMapper;
import com.puyuanmaoshan.platform.mapper.TenantMapper;
import com.puyuanmaoshan.platform.mapper.TenantRoleConfigMapper;
import com.puyuanmaoshan.platform.mapper.TenantUserMapper;
import com.puyuanmaoshan.platform.mapper.UserAccountMapper;
import com.puyuanmaoshan.platform.service.MessageService;
import com.puyuanmaoshan.platform.service.TenantMemberService;
import com.puyuanmaoshan.platform.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TenantMemberServiceImpl implements TenantMemberService {
    private static final Logger logger = LoggerFactory.getLogger(TenantMemberServiceImpl.class);

    private final TenantUserMapper tenantUserMapper;
    private final UserAccountMapper userAccountMapper;
    private final TenantMapper tenantMapper;
    private final MessageService messageService;
    private final MemberRoleAuditLogMapper memberRoleAuditLogMapper;
    private final TenantRoleConfigMapper tenantRoleConfigMapper;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public TenantMemberServiceImpl(TenantUserMapper tenantUserMapper,
                                   UserAccountMapper userAccountMapper,
                                   TenantMapper tenantMapper,
                                   MessageService messageService,
                                   MemberRoleAuditLogMapper memberRoleAuditLogMapper,
                                   TenantRoleConfigMapper tenantRoleConfigMapper,
                                   UserProfileService userProfileService,
                                   ObjectMapper objectMapper) {
        this.tenantUserMapper = tenantUserMapper;
        this.userAccountMapper = userAccountMapper;
        this.tenantMapper = tenantMapper;
        this.messageService = messageService;
        this.memberRoleAuditLogMapper = memberRoleAuditLogMapper;
        this.tenantRoleConfigMapper = tenantRoleConfigMapper;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TenantDtos.MemberInfo> getTenantMembers(Long tenantId) {
        try {
            List<TenantUser> tenantUsers = tenantUserMapper.selectMemberDetailsByTenantId(tenantId);

            List<TenantDtos.MemberInfo> result = new ArrayList<>();
            for (TenantUser tu : tenantUsers) {
                UserAccount user = userAccountMapper.selectById(tu.getUserId());
                if (user != null) {
                    String inviterName = null;
                    if (tu.getInvitedBy() != null) {
                        UserAccount inviter = userAccountMapper.selectById(tu.getInvitedBy());
                        inviterName = inviter != null ? inviter.getNickname() : null;
                    }

                    result.add(new TenantDtos.MemberInfo(
                            user.getId(),
                            user.getMobile(),
                            user.getNickname(),
                            null, // avatarUrl - 需要从用户表获取
                            tu.getRole(),
                            tu.getStatus(),
                            tu.getInvitedBy(),
                            inviterName,
                            tu.getCreatedAt()
                    ));
                }
            }

            return result;
        } catch (Exception e) {
            logger.error("获取租户成员列表失败", e);
            throw new com.puyuanmaoshan.platform.exception.AppException(ErrorCode.INTERNAL_ERROR, "获取成员列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TenantDtos.CommonResponse inviteMember(Long tenantId, Long inviterId, String mobile, String role) {
        try {
            // 验证租户存在
            Tenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                return TenantDtos.ResponseHelper.error("工作室不存在");
            }

            // 验证邀请人是否为该租户的老板
            TenantUser inviterTu = tenantUserMapper.selectByUserIdAndTenantId(inviterId, tenantId);
            if (inviterTu == null || !inviterTu.isBoss()) {
                return TenantDtos.ResponseHelper.error("只有老板可以邀请成员");
            }

            // 不能邀请老板角色（租户只能有一个老板）
            if (TenantUser.Role.BOSS.getCode().equals(role)) {
                return TenantDtos.ResponseHelper.error("不能邀请老板角色");
            }

            // 获取或创建用户
            UserAccount user = getOrCreateUser(mobile);
            Long userId = user.getId();

            // 检查用户是否已在租户中
            int exists = tenantUserMapper.existsByUserIdAndTenantId(userId, tenantId);
            if (exists > 0) {
                return TenantDtos.ResponseHelper.error("该成员已在工作室中");
            }

            // 创建租户用户关联
            TenantUser tenantUser = TenantUser.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .role(role)
                    .invitedBy(inviterId)
                    .status(TenantUser.Status.ACTIVE.getCode())
                    .createdAt(LocalDateTime.now())
                    .build();

            tenantUserMapper.insert(tenantUser);

            // 发送通知
            UserAccount inviter = userAccountMapper.selectById(inviterId);
            String message = String.format("您已被 %s 邀请加入 %s 工作室，角色为：%s",
                    inviter.getNickname(), tenant.getName(), TenantUser.Role.fromCode(role).getName());
            messageService.sendMessage(userId, "加入工作室邀请", message, "task", tenantId);

            logger.info("用户 {} 邀请 {} 加入租户 {}, 角色: {}", inviterId, mobile, tenantId, role);

            return TenantDtos.ResponseHelper.success("邀请成功，用户已加入工作室");

        } catch (Exception e) {
            logger.error("邀请成员失败", e);
            return TenantDtos.ResponseHelper.error("邀请失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TenantDtos.CommonResponse updateMemberRole(Long tenantId, Long operatorId, Long targetUserId, String newRole) {
        try {
            // 验证操作人是否为老板
            TenantUser operatorTu = tenantUserMapper.selectByUserIdAndTenantId(operatorId, tenantId);
            if (operatorTu == null || !operatorTu.isBoss()) {
                return TenantDtos.ResponseHelper.error("只有老板可以修改成员角色");
            }

            // 不能将老板角色改为其他角色（移除老板需要单独操作）
            TenantUser targetTu = tenantUserMapper.selectByUserIdAndTenantId(targetUserId, tenantId);
            if (targetTu == null) {
                return TenantDtos.ResponseHelper.error("成员不存在");
            }

            if (targetTu.isBoss()) {
                return TenantDtos.ResponseHelper.error("不能修改老板的角色");
            }

            // 不能将成员设为老板
            if (TenantUser.Role.BOSS.getCode().equals(newRole)) {
                return TenantDtos.ResponseHelper.error("不能将成员设为老板角色");
            }

            // 更新角色
            targetTu.setRole(newRole);
            targetTu.setUpdatedAt(LocalDateTime.now());
            tenantUserMapper.updateById(targetTu);

            logger.info("用户 {} 修改了用户 {} 在租户 {} 的角色为 {}", operatorId, targetUserId, tenantId, newRole);

            return TenantDtos.ResponseHelper.success("角色修改成功");

        } catch (Exception e) {
            logger.error("修改成员角色失败", e);
            return TenantDtos.ResponseHelper.error("修改角色失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TenantDtos.CommonResponse removeMember(Long tenantId, Long operatorId, Long targetUserId) {
        try {
            // 验证操作人是否为老板
            TenantUser operatorTu = tenantUserMapper.selectByUserIdAndTenantId(operatorId, tenantId);
            if (operatorTu == null || !operatorTu.isBoss()) {
                return TenantDtos.ResponseHelper.error("只有老板可以移除成员");
            }

            // 不能移除自己
            if (operatorId.equals(targetUserId)) {
                return TenantDtos.ResponseHelper.error("不能移除自己");
            }

            // 获取目标成员
            TenantUser targetTu = tenantUserMapper.selectByUserIdAndTenantId(targetUserId, tenantId);
            if (targetTu == null) {
                return TenantDtos.ResponseHelper.error("成员不存在");
            }

            // 不能移除老板（如果只有一个老板）
            if (targetTu.isBoss()) {
                int bossCount = tenantUserMapper.selectByTenantIdAndRole(tenantId, TenantUser.Role.BOSS.getCode()).size();
                if (bossCount <= 1) {
                    return TenantDtos.ResponseHelper.error("工作室至少需要保留一个老板");
                }
            }

            // 停用成员（软删除）
            targetTu.setStatus(TenantUser.Status.INACTIVE.getCode());
            targetTu.setUpdatedAt(LocalDateTime.now());
            tenantUserMapper.updateById(targetTu);

            logger.info("用户 {} 将用户 {} 从租户 {} 移除", operatorId, targetUserId, tenantId);

            return TenantDtos.ResponseHelper.success("成员已移除");

        } catch (Exception e) {
            logger.error("移除成员失败", e);
            return TenantDtos.ResponseHelper.error("移除成员失败: " + e.getMessage());
        }
    }

    @Override
    public List<TenantDtos.UserTenant> getUserTenants(Long userId) {
        try {
            List<TenantUser> tenantUsers = tenantUserMapper.selectActiveTenantsByUserId(userId);

            List<TenantDtos.UserTenant> result = new ArrayList<>();
            for (TenantUser tu : tenantUsers) {
                Tenant tenant = tenantMapper.selectById(tu.getTenantId());
                if (tenant != null) {
                    result.add(new TenantDtos.UserTenant(
                            tenant.getId(),
                            tenant.getName(),
                            tenant.getTenantCode(),
                            tu.getRole(),
                            false // isDefault - 需要根据逻辑设置
                    ));
                }
            }

            return result;
        } catch (Exception e) {
            logger.error("获取用户租户列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public TenantUser getUserRoleInTenant(Long userId, Long tenantId) {
        return tenantUserMapper.selectByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public boolean hasPermission(Long userId, Long tenantId, String requiredRole) {
        TenantUser tenantUser = getUserRoleInTenant(userId, tenantId);
        if (tenantUser == null || !tenantUser.isActive()) {
            return false;
        }

        // 老板拥有所有权限
        if (tenantUser.isBoss()) {
            return true;
        }

        // 检查角色是否匹配
        return tenantUser.getRole().equals(requiredRole);
    }

    @Override
    @Transactional
    public TenantUser saveTenantUser(Long tenantId, Long userId, String role, Long invitedBy) {
        // 检查是否已存在
        TenantUser existing = tenantUserMapper.selectByUserIdAndTenantId(userId, tenantId);
        if (existing != null) {
            existing.setRole(role);
            existing.setStatus(TenantUser.Status.ACTIVE.getCode());
            existing.setUpdatedAt(LocalDateTime.now());
            tenantUserMapper.updateById(existing);
            return existing;
        }

        // 创建新关联
        TenantUser tenantUser = TenantUser.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(role)
                .invitedBy(invitedBy)
                .status(TenantUser.Status.ACTIVE.getCode())
                .createdAt(LocalDateTime.now())
                .build();

        tenantUserMapper.insert(tenantUser);
        return tenantUser;
    }

    @Override
    @Transactional
    public UserAccount getOrCreateUser(String mobile) {
        // 查找用户
        UserAccount user = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getMobile, mobile)
        );

        if (user != null) {
            return user;
        }

        // 创建新用户
        user = UserAccount.builder()
                .mobile(mobile)
                .nickname("用户" + mobile.substring(7)) // 使用手机号后4位作为默认昵称
                .roleCode("merchant_viewer") // 默认角色
                .status(1)
                .tenantId(0L) // 暂不绑定租户
                .createdAt(LocalDateTime.now())
                .build();

        userAccountMapper.insert(user);
        logger.info("创建新用户: {}", mobile);

        return user;
    }

    @Override
    public UserDtos.TenantMemberListResponse getTenantMembers(Long tenantId, UserDtos.PageRequest pageRequest) {
        try {
            LambdaQueryWrapper<TenantUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TenantUser::getTenantId, tenantId);

            if (StringUtils.hasText(pageRequest.status())) {
                wrapper.eq(TenantUser::getStatus, pageRequest.status());
            }

            if (StringUtils.hasText(pageRequest.roleCode())) {
                wrapper.eq(TenantUser::getRole, pageRequest.roleCode());
            }

            wrapper.orderByDesc(TenantUser::getCreatedAt);

            Page<TenantUser> page = tenantUserMapper.selectPage(
                    new Page<>(pageRequest.page(), pageRequest.pageSize()),
                    wrapper
            );

            List<UserDtos.TenantMemberResponse> members = new ArrayList<>();
            for (TenantUser tu : page.getRecords()) {
                UserAccount user = userAccountMapper.selectById(tu.getUserId());
                if (user != null) {
                    String creatorName = null;
                    if (tu.getInvitedBy() != null) {
                        UserAccount inviter = userAccountMapper.selectById(tu.getInvitedBy());
                        creatorName = inviter != null ? inviter.getNickname() : null;
                    }

                    members.add(new UserDtos.TenantMemberResponse(
                            tu.getId(),
                            user.getId(),
                            user.getNickname(),
                            user.getAvatarUrl(),
                            user.getMobile(),
                            user.getPhone(),
                            user.getEmail(),
                            tu.getRole(),
                            getRoleName(tu.getRole()),
                            tu.getStatus(),
                            tu.getInvitedBy(),
                            creatorName,
                            tu.getCreatedAt().toString()
                    ));
                }
            }

            return new UserDtos.TenantMemberListResponse(members, page.getTotal());

        } catch (Exception e) {
            logger.error("获取租户成员列表失败", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "获取成员列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void inviteMemberV2(Long tenantId, Long operatorId, UserDtos.InviteMemberRequest request) {
        // 验证租户存在
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "工作室不存在");
        }

        // 验证操作人权限（必须是老板或管理员）
        TenantUser operatorTu = tenantUserMapper.selectByUserIdAndTenantId(operatorId, tenantId);
        if (operatorTu == null || !(operatorTu.isBoss() || isAdmin(operatorTu.getRole()))) {
            throw new AppException(ErrorCode.FORBIDDEN, "只有老板或管理员可以邀请成员");
        }

        // 不能邀请老板角色
        if (TenantUser.Role.BOSS.getCode().equals(request.roleCode())) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "不能邀请老板角色");
        }

        // 根据联系方式查找用户
        UserAccount user;
        if (PHONE_PATTERN.matcher(request.contact()).matches()) {
            user = userProfileService.findByPhone(request.contact());
        } else if (EMAIL_PATTERN.matcher(request.contact()).matches()) {
            user = userProfileService.findByEmail(request.contact());
        } else {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "联系方式格式错误");
        }

        // 如果用户不存在，可以创建（这里简化处理，要求用户已存在）
        if (user == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "用户不存在，请先注册");
        }

        Long userId = user.getId();

        // 检查用户是否已在租户中
        TenantUser existing = tenantUserMapper.selectByUserIdAndTenantId(userId, tenantId);
        if (existing != null && existing.getStatus().equals(TenantUser.Status.ACTIVE.getCode())) {
            throw new AppException(ErrorCode.CONFLICT, "该成员已在工作室中");
        }

        // 创建租户用户关联
        TenantUser tenantUser = TenantUser.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(request.roleCode())
                .invitedBy(operatorId)
                .status(TenantUser.Status.ACTIVE.getCode())
                .createdAt(LocalDateTime.now())
                .build();

        tenantUserMapper.insert(tenantUser);

        // 记录审计日志
        recordAuditLog(tenantId, userId, operatorId,
                MemberRoleAuditLog.Action.INVITE, null, request.roleCode(),
                null, TenantUser.Status.ACTIVE.getCode(),
                request.remark());

        // 发送通知
        UserAccount operator = userAccountMapper.selectById(operatorId);
        String message = String.format("您已被 %s 邀请加入 %s 工作室，角色为：%s",
                operator.getNickname(), tenant.getName(), getRoleName(request.roleCode()));
        messageService.sendMessage(userId, "加入工作室邀请", message, "task", tenantId);

        logger.info("用户 {} 邀请 {} 加入租户 {}, 角色: {}", operatorId, request.contact(), tenantId, request.roleCode());
    }

    @Override
    @Transactional
    public void updateMemberRoleV2(Long tenantId, Long operatorId, Long memberUserId, UserDtos.UpdateMemberRoleRequest request) {
        TenantUser operatorTu = tenantUserMapper.selectByUserIdAndTenantId(operatorId, tenantId);
        if (operatorTu == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "您不在此租户中");
        }

        // 验证权限
        if (!operatorTu.isBoss() && !isAdmin(operatorTu.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN, "只有老板或管理员可以修改成员角色");
        }

        TenantUser targetTu = tenantUserMapper.selectByUserIdAndTenantId(memberUserId, tenantId);
        if (targetTu == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        // 不能修改老板的角色
        if (targetTu.isBoss() && !operatorTu.isBoss()) {
            throw new AppException(ErrorCode.FORBIDDEN, "只有老板可以修改老板的角色");
        }

        String oldRole = targetTu.getRole();
        targetTu.setRole(request.roleCode());
        targetTu.setUpdatedAt(LocalDateTime.now());
        tenantUserMapper.updateById(targetTu);

        recordAuditLog(tenantId, memberUserId, operatorId,
                MemberRoleAuditLog.Action.REMOVE_ROLE, oldRole, request.roleCode(),
                null, null, "修改角色");

        logger.info("用户 {} 修改了用户 {} 在租户 {} 的角色从 {} 到 {}",
                operatorId, memberUserId, tenantId, oldRole, request.roleCode());
    }

    @Override
    @Transactional
    public void removeMemberV2(Long tenantId, Long operatorId, Long memberUserId) {
        TenantUser operatorTu = tenantUserMapper.selectByUserIdAndTenantId(operatorId, tenantId);
        if (operatorTu == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "您不在此租户中");
        }

        // 不能移除自己
        if (operatorId.equals(memberUserId)) {
            throw new AppException(ErrorCode.BUSINESS_ERROR, "不能移除自己");
        }

        // 验证权限
        if (!operatorTu.isBoss() && !isAdmin(operatorTu.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN, "只有老板或管理员可以移除成员");
        }

        TenantUser targetTu = tenantUserMapper.selectByUserIdAndTenantId(memberUserId, tenantId);
        if (targetTu == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        // 不能移除老板（如果不是老板操作）
        if (targetTu.isBoss() && !operatorTu.isBoss()) {
            throw new AppException(ErrorCode.FORBIDDEN, "只有老板可以移除老板");
        }

        // 检查是否是最后一个老板
        if (targetTu.isBoss()) {
            int bossCount = tenantUserMapper.selectByTenantIdAndRole(tenantId, TenantUser.Role.BOSS.getCode()).size();
            if (bossCount <= 1) {
                throw new AppException(ErrorCode.BUSINESS_ERROR, "工作室至少需要保留一个老板");
            }
        }

        String oldStatus = targetTu.getStatus();
        targetTu.setStatus(TenantUser.Status.INACTIVE.getCode());
        targetTu.setUpdatedAt(LocalDateTime.now());
        tenantUserMapper.updateById(targetTu);

        recordAuditLog(tenantId, memberUserId, operatorId,
                MemberRoleAuditLog.Action.REMOVE_MEMBER, null, null,
                oldStatus, TenantUser.Status.INACTIVE.getCode(),
                "移除成员");

        logger.info("用户 {} 将用户 {} 从租户 {} 移除", operatorId, memberUserId, tenantId);
    }

    @Override
    @Transactional
    public void updateMemberStatus(Long tenantId, Long operatorId, Long memberUserId, UserDtos.UpdateMemberStatusRequest request) {
        TenantUser operatorTu = tenantUserMapper.selectByUserIdAndTenantId(operatorId, tenantId);
        if (operatorTu == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "您不在此租户中");
        }

        // 验证权限
        if (!operatorTu.isBoss() && !isAdmin(operatorTu.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN, "只有老板或管理员可以修改成员状态");
        }

        TenantUser targetTu = tenantUserMapper.selectByUserIdAndTenantId(memberUserId, tenantId);
        if (targetTu == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "成员不存在");
        }

        // 不能禁用老板
        if (targetTu.isBoss() && TenantUser.Status.INACTIVE.getCode().equals(request.status())) {
            throw new AppException(ErrorCode.FORBIDDEN, "不能禁用老板");
        }

        String oldStatus = targetTu.getStatus();
        targetTu.setStatus(request.status());
        targetTu.setUpdatedAt(LocalDateTime.now());
        tenantUserMapper.updateById(targetTu);

        MemberRoleAuditLog.Action action = TenantUser.Status.ACTIVE.getCode().equals(request.status())
                ? MemberRoleAuditLog.Action.ENABLE : MemberRoleAuditLog.Action.DISABLE;

        recordAuditLog(tenantId, memberUserId, operatorId,
                action, null, null, oldStatus, request.status(),
                "修改状态");

        logger.info("用户 {} {}了用户 {} 在租户 {}",
                operatorId, action.getName(), memberUserId, tenantId);
    }

    @Override
    public UserAccount getOrCreateUserByEmail(String email) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getEmail, email);
        UserAccount user = userAccountMapper.selectOne(wrapper);

        if (user != null) {
            return user;
        }

        user = UserAccount.builder()
                .email(email)
                .nickname("用户" + email.substring(0, email.indexOf('@')))
                .roleCode("merchant_viewer")
                .status(1)
                .tenantId(0L)
                .createdAt(LocalDateTime.now())
                .build();

        userAccountMapper.insert(user);
        logger.info("创建新用户: {}", email);

        return user;
    }

    @Override
    public boolean isTenantAdmin(Long userId, Long tenantId) {
        TenantUser tenantUser = getUserRoleInTenant(userId, tenantId);
        if (tenantUser == null || !tenantUser.isActive()) {
            return false;
        }
        return tenantUser.isBoss() || isAdmin(tenantUser.getRole());
    }

    @Override
    public boolean isTenantOwner(Long userId, Long tenantId) {
        TenantUser tenantUser = getUserRoleInTenant(userId, tenantId);
        return tenantUser != null && tenantUser.isBoss();
    }

    @Override
    public List<UserDtos.RoleConfigResponse> getTenantRoles() {
        List<TenantRoleConfig> configs = tenantRoleConfigMapper.selectList(
                new LambdaQueryWrapper<TenantRoleConfig>()
                        .orderByAsc(TenantRoleConfig::getSortOrder)
        );

        return configs.stream().map(config -> {
            List<String> permissions = new ArrayList<>();
            if (StringUtils.hasText(config.getPermissions())) {
                try {
                    permissions = objectMapper.readValue(config.getPermissions(), new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    logger.warn("Failed to parse permissions for role: {}", config.getRoleCode());
                }
            }
            return new UserDtos.RoleConfigResponse(
                    config.getId(),
                    config.getRoleCode(),
                    config.getRoleName(),
                    config.getDescription(),
                    permissions,
                    config.getSortOrder(),
                    config.getIsSystem() == 1
            );
        }).collect(Collectors.toList());
    }

    @Override
    public UserDtos.MemberAuditLogListResponse getMemberAuditLogs(Long tenantId, Long memberUserId, UserDtos.PageRequest pageRequest) {
        LambdaQueryWrapper<MemberRoleAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberRoleAuditLog::getTenantId, tenantId);

        if (memberUserId != null) {
            wrapper.eq(MemberRoleAuditLog::getMemberUserId, memberUserId);
        }

        wrapper.orderByDesc(MemberRoleAuditLog::getCreatedAt);

        Page<MemberRoleAuditLog> page = memberRoleAuditLogMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.pageSize()),
                wrapper
        );

        List<UserDtos.MemberAuditLogResponse> logs = page.getRecords().stream().map(log -> {
            UserAccount member = userAccountMapper.selectById(log.getMemberUserId());
            UserAccount operator = userAccountMapper.selectById(log.getOperatorUserId());

            return new UserDtos.MemberAuditLogResponse(
                    log.getId(),
                    log.getMemberUserId(),
                    member != null ? member.getNickname() : null,
                    log.getOperatorUserId(),
                    operator != null ? operator.getNickname() : null,
                    log.getAction(),
                    getActionName(log.getAction()),
                    log.getOldRole(),
                    log.getNewRole(),
                    log.getOldStatus(),
                    log.getNewStatus(),
                    log.getRemark(),
                    log.getCreatedAt().toString()
            );
        }).collect(Collectors.toList());

        return new UserDtos.MemberAuditLogListResponse(logs, page.getTotal());
    }

    @Override
    @Transactional
    public void recordAuditLog(Long tenantId, Long memberUserId, Long operatorUserId,
                               MemberRoleAuditLog.Action action, String oldRole, String newRole,
                               String oldStatus, String newStatus, String remark) {
        MemberRoleAuditLog log = MemberRoleAuditLog.builder()
                .tenantId(tenantId)
                .memberUserId(memberUserId)
                .operatorUserId(operatorUserId)
                .action(action.getCode())
                .oldRole(oldRole)
                .newRole(newRole)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .remark(remark)
                .createdAt(LocalDateTime.now())
                .build();

        memberRoleAuditLogMapper.insert(log);
    }

    private String getRoleName(String roleCode) {
        TenantRoleConfig config = tenantRoleConfigMapper.selectOne(
                new LambdaQueryWrapper<TenantRoleConfig>()
                        .eq(TenantRoleConfig::getRoleCode, roleCode)
        );
        return config != null ? config.getRoleName() : roleCode;
    }

    private String getActionName(String actionCode) {
        for (MemberRoleAuditLog.Action action : MemberRoleAuditLog.Action.values()) {
            if (action.getCode().equals(actionCode)) {
                return action.getName();
            }
        }
        return actionCode;
    }

    private boolean isAdmin(String roleCode) {
        return "tenant_admin".equals(roleCode) || "boss".equals(roleCode);
    }
}