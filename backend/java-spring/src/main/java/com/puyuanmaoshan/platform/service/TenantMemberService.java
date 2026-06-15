package com.puyuanmaoshan.platform.service;

import com.puyuanmaoshan.platform.dto.TenantDtos;
import com.puyuanmaoshan.platform.dto.UserDtos;
import com.puyuanmaoshan.platform.entity.MemberRoleAuditLog;
import com.puyuanmaoshan.platform.entity.TenantUser;

import java.util.List;

/**
 * 租户成员服务
 */
public interface TenantMemberService {

    /**
     * 获取当前租户成员列表（分页）
     */
    UserDtos.TenantMemberListResponse getTenantMembers(Long tenantId, UserDtos.PageRequest pageRequest);

    /**
     * 获取当前租户成员列表（不分页，兼容旧接口）
     */
    List<TenantDtos.MemberInfo> getTenantMembers(Long tenantId);

    /**
     * 邀请成员到租户
     */
    TenantDtos.CommonResponse inviteMember(Long tenantId, Long inviterId, String mobile, String role);

    /**
     * 邀请成员（新版）
     */
    void inviteMemberV2(Long tenantId, Long operatorId, UserDtos.InviteMemberRequest request);

    /**
     * 修改成员角色
     */
    TenantDtos.CommonResponse updateMemberRole(Long tenantId, Long operatorId, Long targetUserId, String newRole);

    /**
     * 修改成员角色（新版）
     */
    void updateMemberRoleV2(Long tenantId, Long operatorId, Long memberUserId, UserDtos.UpdateMemberRoleRequest request);

    /**
     * 移除成员
     */
    TenantDtos.CommonResponse removeMember(Long tenantId, Long operatorId, Long targetUserId);

    /**
     * 移除成员（新版）
     */
    void removeMemberV2(Long tenantId, Long operatorId, Long memberUserId);

    /**
     * 启用/禁用成员
     */
    void updateMemberStatus(Long tenantId, Long operatorId, Long memberUserId, UserDtos.UpdateMemberStatusRequest request);

    /**
     * 获取用户所属的所有租户
     */
    List<TenantDtos.UserTenant> getUserTenants(Long userId);

    /**
     * 获取用户在指定租户中的角色信息
     */
    TenantUser getUserRoleInTenant(Long userId, Long tenantId);

    /**
     * 验证用户是否有指定租户的权限
     */
    boolean hasPermission(Long userId, Long tenantId, String requiredRole);

    /**
     * 创建或更新租户用户关联
     */
    TenantUser saveTenantUser(Long tenantId, Long userId, String role, Long invitedBy);

    /**
     * 根据手机号获取或创建用户
     */
    com.puyuanmaoshan.platform.entity.UserAccount getOrCreateUser(String mobile);

    /**
     * 根据邮箱获取或创建用户
     */
    com.puyuanmaoshan.platform.entity.UserAccount getOrCreateUserByEmail(String email);

    /**
     * 检查用户是否为租户管理员或更高权限
     */
    boolean isTenantAdmin(Long userId, Long tenantId);

    /**
     * 检查用户是否为租户创建者（boss）
     */
    boolean isTenantOwner(Long userId, Long tenantId);

    /**
     * 获取租户角色列表
     */
    List<UserDtos.RoleConfigResponse> getTenantRoles();

    /**
     * 获取成员审计日志
     */
    UserDtos.MemberAuditLogListResponse getMemberAuditLogs(Long tenantId, Long memberUserId, UserDtos.PageRequest pageRequest);

    /**
     * 记录成员操作审计日志
     */
    void recordAuditLog(Long tenantId, Long memberUserId, Long operatorUserId,
                        MemberRoleAuditLog.Action action, String oldRole, String newRole,
                        String oldStatus, String newStatus, String remark);

    /**
     * 创建新工作室/租户
     * @param userId 创建者用户 ID
     * @param tenantName 工作室名称
     * @return 创建结果（包含租户ID、名称、编码、角色）
     */
    TenantDtos.CreateTenantResponse createTenant(Long userId, String tenantName);

    /**
     * 删除工作室/租户（仅创建者 boss 可操作）
     * @param userId 操作者用户 ID
     * @param tenantId 要删除的租户 ID
     * @return 操作结果
     */
    TenantDtos.CommonResponse deleteTenant(Long userId, Long tenantId);
}