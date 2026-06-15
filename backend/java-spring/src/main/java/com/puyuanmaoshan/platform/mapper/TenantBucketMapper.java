package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.TenantBucket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TenantBucketMapper extends BaseMapper<TenantBucket> {

    @Select("SELECT tb.* FROM tenant_bucket tb WHERE tb.tenant_id = #{tenantId} AND tb.status != 'deleted'")
    List<TenantBucket> findByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM tenant_bucket WHERE tenant_id = #{tenantId} AND status IN ('active', 'creating')")
    int countActiveByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM tenant_bucket WHERE status = 'active'")
    Long countActiveBuckets();

    @Select("SELECT tb.* FROM tenant_bucket tb WHERE tb.bucket_name = #{bucketName}")
    TenantBucket findByBucketName(@Param("bucketName") String bucketName);
}
