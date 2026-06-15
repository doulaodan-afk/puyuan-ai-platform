package com.puyuanmaoshan.platform.controller;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.dto.TenantDtos;
import com.puyuanmaoshan.platform.dto.UserDtos;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.mapper.TenantMapper;
import com.puyuanmaoshan.platform.service.TenantMemberService;
import com.puyuanmaoshan.platform.util.RequestContextUtil;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户成员管理 Controller
 */
@RestController
@RequestMapping("/api/tenant")
public class TenantMemberController {
    private final TenantMemberService tenantMemberService;
    private final TenantMapper tenantMapper;

    public TenantMemberController(TenantMemberService tenantMemberService, TenantMapper tenantMapper) {
        this.tenantMemberService = tenantMemberService;
        this.tenantMapper = tenantMapper;
    }

    /**
     * 获取当前租户成员列表
     * GET /api/tenant/members
     */
    @GetMapping("/members")
    public ApiResponse<List<TenantDtos.MemberInfo>> getMembers(
            @RequestHeader("X-Tenant-Id") String tenantIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            List<TenantDtos.MemberInfo> members = tenantMemberService.getTenantMembers(tenantId);

            return ApiResponse.ok(members, RequestContextUtil.resolveRequestId(null, "req-get-members"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "获取成员列表失败: " + e.getMessage(), null);
        }
    }

    /**
     * 邀请成员
     * POST /api/tenant/invite
     */
    @PostMapping("/invite")
    public ApiResponse<Object> inviteMember(
            @RequestHeader("X-Tenant-Id") String tenantIdHeader,
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody TenantDtos.InviteMemberRequest request) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            long userId = RequestContextUtil.parseUserId(userIdHeader);

            TenantDtos.CommonResponse result = tenantMemberService.inviteMember(
                    tenantId, userId, request.mobile(), request.role());

            if (result.success()) {
                return ApiResponse.ok(null, RequestContextUtil.resolveRequestId(null, "req-invite-member"));
            } else {
                return ApiResponse.error(ErrorCode.BUSINESS_ERROR.code(),
                        result.message(), null);
            }

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "邀请成员失败: " + e.getMessage(), null);
        }
    }

    /**
     * 创建新工作室/租户
     * POST /api/tenant/create
     */
    @PostMapping("/create")
    public ApiResponse<TenantDtos.CreateTenantResponse> createTenant(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody TenantDtos.CreateTenantRequest request) {
        try {
            long userId = RequestContextUtil.parseUserId(userIdHeader);

            TenantDtos.CreateTenantResponse result = tenantMemberService.createTenant(
                    userId, request.tenantName());

            return ApiResponse.ok(result, RequestContextUtil.resolveRequestId(null, "req-create-tenant"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "创建工作室失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取用户所属的所有租户
     * GET /api/tenant/user/tenants
     */
    @GetMapping("/user/tenants")
    public ApiResponse<List<TenantDtos.UserTenant>> getUserTenants(
            @RequestHeader("X-User-Id") String userIdHeader) {
        try {
            long userId = RequestContextUtil.parseUserId(userIdHeader);
            List<TenantDtos.UserTenant> tenants = tenantMemberService.getUserTenants(userId);

            return ApiResponse.ok(tenants, RequestContextUtil.resolveRequestId(null, "req-get-tenants"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "获取租户列表失败: " + e.getMessage(), null);
        }
    }

    /**
     * 切换租户
     * POST /api/tenant/switch
     */
    @PostMapping("/switch")
    public ApiResponse<TenantDtos.SwitchTenantResponse> switchTenant(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody TenantDtos.SwitchTenantRequest request) {
        try {
            long userId = RequestContextUtil.parseUserId(userIdHeader);
            long targetTenantId = request.tenantId();

            // 验证用户是否在该租户中
            var tenantUser = tenantMemberService.getUserRoleInTenant(userId, targetTenantId);
            if (tenantUser == null || !tenantUser.isActive()) {
                return ApiResponse.error(ErrorCode.FORBIDDEN.code(),
                        "您不在该工作室中或已被移除", null);
            }

            // 获取租户信息
            Tenant tenant = tenantMapper.selectById(targetTenantId);
            String tenantName = tenant != null ? tenant.getName() : "";

            return ApiResponse.ok(new TenantDtos.SwitchTenantResponse(
                    null, // token - 前端继续使用当前 token
                    3600, // expiresIn
                    userId,
                    targetTenantId,
                    tenantName,
                    tenantUser.getRole()
            ), RequestContextUtil.resolveRequestId(null, "req-switch-tenant"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "切换工作室失败: " + e.getMessage(), null);
        }
    }

    // ========== 新增成员管理接口（V2版本）==========

    /**
     * 获取当前租户成员列表（分页）
     * GET /api/tenant/members/v2
     */
    @GetMapping("/members/v2")
    public ApiResponse<UserDtos.TenantMemberListResponse> getMembersV2(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            UserDtos.PageRequest pageRequest = new UserDtos.PageRequest(page, pageSize, keyword, status, roleCode);
            UserDtos.TenantMemberListResponse response = tenantMemberService.getTenantMembers(tenantId, pageRequest);

            return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(null, "req-get-members-v2"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "获取成员列表失败: " + e.getMessage(), null);
        }
    }

    /**
     * 邀请成员（V2版本）
     * POST /api/tenant/members
     */
    @PostMapping("/members")
    public ApiResponse<Void> inviteMemberV2(
            @Valid @RequestBody UserDtos.InviteMemberRequest request,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            long operatorId = RequestContextUtil.getCurrentUserId();

            tenantMemberService.inviteMemberV2(tenantId, operatorId, request);

            return ApiResponse.ok(null, RequestContextUtil.resolveRequestId(null, "req-invite-member-v2"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "邀请成员失败: " + e.getMessage(), null);
        }
    }

    /**
     * 修改成员角色（V2版本）
     * PUT /api/tenant/members/{memberId}/role
     */
    @PutMapping("/members/{memberId}/role")
    public ApiResponse<Void> updateMemberRoleV2(
            @PathVariable("memberId") Long memberUserId,
            @Valid @RequestBody UserDtos.UpdateMemberRoleRequest request,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            long operatorId = RequestContextUtil.getCurrentUserId();

            tenantMemberService.updateMemberRoleV2(tenantId, operatorId, memberUserId, request);

            return ApiResponse.ok(null, RequestContextUtil.resolveRequestId(null, "req-update-role-v2"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "修改角色失败: " + e.getMessage(), null);
        }
    }

    /**
     * 移除成员（V2版本）
     * DELETE /api/tenant/members/{memberId}
     */
    @DeleteMapping("/members/{memberId}")
    public ApiResponse<Void> removeMemberV2(
            @PathVariable("memberId") Long memberUserId,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            // 优先使用 X-User-Id header，fallback 到上下文
            long operatorId;
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                operatorId = RequestContextUtil.parseUserId(userIdHeader);
            } else {
                operatorId = RequestContextUtil.getCurrentUserId();
            }

            tenantMemberService.removeMemberV2(tenantId, operatorId, memberUserId);

            return ApiResponse.ok(null, RequestContextUtil.resolveRequestId(null, "req-remove-member-v2"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "移除成员失败: " + e.getMessage(), null);
        }
    }

    /**
     * 启用/禁用成员
     * PUT /api/tenant/members/{memberId}/status
     */
    @PutMapping("/members/{memberId}/status")
    public ApiResponse<Void> updateMemberStatus(
            @PathVariable("memberId") Long memberUserId,
            @Valid @RequestBody UserDtos.UpdateMemberStatusRequest request,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            long operatorId = RequestContextUtil.getCurrentUserId();

            tenantMemberService.updateMemberStatus(tenantId, operatorId, memberUserId, request);

            return ApiResponse.ok(null, RequestContextUtil.resolveRequestId(null, "req-update-status"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "修改成员状态失败: " + e.getMessage(), null);
        }
    }

    /**
     * 删除工作室/租户（仅 boss 可操作）
     * DELETE /api/tenant/{tenantId}
     */
    @DeleteMapping("/{tenantId}")
    public ApiResponse<Object> deleteTenant(
            @PathVariable("tenantId") Long tenantId,
            @RequestHeader("X-User-Id") String userIdHeader) {
        try {
            long userId = RequestContextUtil.parseUserId(userIdHeader);

            TenantDtos.CommonResponse result = tenantMemberService.deleteTenant(userId, tenantId);

            if (result.success()) {
                return ApiResponse.ok(null, RequestContextUtil.resolveRequestId(null, "req-delete-tenant"));
            } else {
                return ApiResponse.error(ErrorCode.BUSINESS_ERROR.code(),
                        result.message(), null);
            }

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "删除工作室失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取租户角色列表
     * GET /api/tenant/roles
     */
    @GetMapping("/roles")
    public ApiResponse<List<UserDtos.RoleConfigResponse>> getRoles() {
        try {
            List<UserDtos.RoleConfigResponse> roles = tenantMemberService.getTenantRoles();

            return ApiResponse.ok(roles, RequestContextUtil.resolveRequestId(null, "req-get-roles"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "获取角色列表失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取成员审计日志
     * GET /api/tenant/members/{memberId}/audit-logs
     */
    @GetMapping("/members/{memberId}/audit-logs")
    public ApiResponse<UserDtos.MemberAuditLogListResponse> getMemberAuditLogs(
            @PathVariable("memberId") Long memberUserId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestHeader("X-Tenant-Id") String tenantIdHeader) {
        try {
            long tenantId = RequestContextUtil.parseTenantId(tenantIdHeader);
            UserDtos.PageRequest pageRequest = new UserDtos.PageRequest(page, pageSize, null, null, null);
            UserDtos.MemberAuditLogListResponse response = tenantMemberService.getMemberAuditLogs(tenantId, memberUserId, pageRequest);

            return ApiResponse.ok(response, RequestContextUtil.resolveRequestId(null, "req-get-audit-logs"));

        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(),
                    "获取审计日志失败: " + e.getMessage(), null);
        }
    }
}