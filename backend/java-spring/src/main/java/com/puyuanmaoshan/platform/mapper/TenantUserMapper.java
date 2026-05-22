package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.TenantUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 租户用户关联表 Mapper
 */
@Mapper
public interface TenantUserMapper extends BaseMapper<TenantUser> {

    /**
     * 查询用户所属的所有活跃租户
     */
    @Select("SELECT * FROM tenant_user WHERE user_id = #{userId} AND status = 'active' ORDER BY created_at")
    List<TenantUser> selectActiveTenantsByUserId(@Param("userId") Long userId);

    /**
     * 查询租户的所有活跃成员
     */
    @Select("SELECT * FROM tenant_user WHERE tenant_id = #{tenantId} AND status = 'active' ORDER BY role DESC, created_at")
    List<TenantUser> selectActiveMembersByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询租户的成员数量
     */
    @Select("SELECT COUNT(*) FROM tenant_user WHERE tenant_id = #{tenantId} AND status = 'active'")
    int countActiveMembersByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询用户在租户中的角色
     */
    @Select("SELECT * FROM tenant_user WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND status = 'active'")
    TenantUser selectByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 检查用户是否为租户成员
     */
    @Select("SELECT COUNT(*) FROM tenant_user WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    int existsByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 查询租户下的指定角色的成员
     */
    @Select("SELECT * FROM tenant_user WHERE tenant_id = #{tenantId} AND role = #{role} AND status = 'active'")
    List<TenantUser> selectByTenantIdAndRole(@Param("tenantId") Long tenantId, @Param("role") String role);

    /**
     * 删除用户在租户中的关系
     */
    @Select("UPDATE tenant_user SET status = 'inactive' WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    int deactivateByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 查询邀请成员详情（包含用户信息）
     */
    @Select("SELECT tu.*, ua.mobile, ua.nickname, ua.avatar_url " +
            "FROM tenant_user tu " +
            "JOIN user_account ua ON tu.user_id = ua.id " +
            "WHERE tu.tenant_id = #{tenantId} AND tu.status = 'active' " +
            "ORDER BY CASE tu.role " +
            "  WHEN 'boss' THEN 1 " +
            "  WHEN 'designer' THEN 2 " +
            "  WHEN 'design_assistant' THEN 3 " +
            "  WHEN 'pattern_maker' THEN 4 " +
            "  ELSE 5 " +
            "END, tu.created_at")
    List<TenantUser> selectMemberDetailsByTenantId(@Param("tenantId") Long tenantId);
}