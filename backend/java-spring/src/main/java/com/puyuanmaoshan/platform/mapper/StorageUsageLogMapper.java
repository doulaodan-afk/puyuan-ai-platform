package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.StorageUsageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StorageUsageLogMapper extends BaseMapper<StorageUsageLog> {

    @Select("SELECT sul.* FROM storage_usage_log sul " +
            "WHERE sul.tenant_bucket_id = #{bucketId} AND sul.snapshot_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY sul.snapshot_date ASC")
    List<StorageUsageLog> findByBucketIdAndDateRange(@Param("bucketId") Long bucketId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select("SELECT sul.* FROM storage_usage_log sul " +
            "WHERE sul.tenant_id = #{tenantId} AND sul.snapshot_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY sul.snapshot_date ASC")
    List<StorageUsageLog> findByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select("SELECT sul.* FROM storage_usage_log sul " +
            "WHERE sul.tenant_bucket_id = #{bucketId} AND sul.snapshot_date = #{date} LIMIT 1")
    StorageUsageLog findByBucketIdAndDate(@Param("bucketId") Long bucketId, @Param("date") LocalDate date);
}
