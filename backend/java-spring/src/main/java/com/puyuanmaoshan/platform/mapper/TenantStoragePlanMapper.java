package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.TenantStoragePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TenantStoragePlanMapper extends BaseMapper<TenantStoragePlan> {

    @Select("SELECT tsp.* FROM tenant_storage_plan tsp " +
            "WHERE tsp.tenant_id = #{tenantId} AND tsp.plan_status = 'active' " +
            "ORDER BY tsp.effective_date DESC LIMIT 1")
    TenantStoragePlan findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT tsp.* FROM tenant_storage_plan tsp " +
            "WHERE tsp.tenant_bucket_id = #{bucketId} AND tsp.plan_status = 'active' " +
            "ORDER BY tsp.effective_date DESC LIMIT 1")
    TenantStoragePlan findActiveByBucketId(@Param("bucketId") Long bucketId);
}
