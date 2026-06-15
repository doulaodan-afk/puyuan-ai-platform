package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.StorageBillingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StorageBillingRecordMapper extends BaseMapper<StorageBillingRecord> {

    @Select("SELECT sbr.* FROM storage_billing_record sbr WHERE sbr.tenant_id = #{tenantId} ORDER BY sbr.bill_period DESC")
    List<StorageBillingRecord> findByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT sbr.* FROM storage_billing_record sbr WHERE sbr.tenant_bucket_id = #{bucketId} ORDER BY sbr.bill_period DESC")
    List<StorageBillingRecord> findByBucketId(@Param("bucketId") Long bucketId);

    @Select("SELECT sbr.* FROM storage_billing_record sbr WHERE sbr.bill_period = #{period} ORDER BY sbr.tenant_id")
    List<StorageBillingRecord> findByPeriod(@Param("period") String period);

    @Select("SELECT sbr.* FROM storage_billing_record sbr " +
            "WHERE sbr.tenant_id = #{tenantId} AND sbr.tenant_bucket_id = #{bucketId} AND sbr.bill_period = #{period}")
    StorageBillingRecord findByTenantBucketPeriod(@Param("tenantId") Long tenantId,
                                                   @Param("bucketId") Long bucketId,
                                                   @Param("period") String period);

    @Select("SELECT COUNT(*) FROM storage_billing_record WHERE bill_status = 'pending'")
    Long countPendingBills();

    @Select("SELECT COALESCE(SUM(total_fee), 0) FROM storage_billing_record WHERE bill_status = 'paid'")
    BigDecimal sumPaidFees();
}
