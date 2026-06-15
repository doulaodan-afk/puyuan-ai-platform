package com.puyuanmaoshan.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puyuanmaoshan.platform.entity.StoragePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoragePlanMapper extends BaseMapper<StoragePlan> {

    @Select("SELECT sp.* FROM storage_plan sp WHERE sp.status = 1 ORDER BY sp.sort_order ASC")
    List<StoragePlan> findAllActive();

    @Select("SELECT sp.* FROM storage_plan sp WHERE sp.plan_code = #{planCode} AND sp.status = 1")
    StoragePlan findByCode(@Param("planCode") String planCode);

    @Select("SELECT COUNT(*) FROM tenant_storage_plan tsp WHERE tsp.plan_id = #{planId} AND tsp.plan_status = 'active'")
    Long countActiveTenants(@Param("planId") Long planId);
}
