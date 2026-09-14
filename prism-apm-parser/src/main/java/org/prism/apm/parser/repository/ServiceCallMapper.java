package org.prism.apm.parser.repository;

import org.prism.apm.parser.model.entity.ServiceCallEntity;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * @author john
 * @date 2025-09-04 17:46
 */
public interface ServiceCallMapper {
    int deleteByPrimaryKey(@Param("time") OffsetDateTime time, @Param("spanId") String spanId);

    int insert(ServiceCallEntity record);

    int insertSelective(ServiceCallEntity record);

    ServiceCallEntity selectByPrimaryKey(@Param("time") OffsetDateTime time, @Param("spanId") String spanId);

    int updateByPrimaryKeySelective(ServiceCallEntity record);

    int updateByPrimaryKey(ServiceCallEntity record);

    int deleteByPrimaryKey(OffsetDateTime time);

    ServiceCallEntity selectByPrimaryKey(OffsetDateTime time);

    int batchInsert(@Param("list") List<ServiceCallEntity> list);

    int deleteDataInRange(@Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    List<ServiceCallEntity> selectAll();

    void refreshServiceCallMinute(@Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

    int batchUpsert(@Param("list") List<ServiceCallEntity> list);

    int batchUpsertDoNothing(@Param("list") List<ServiceCallEntity> list);
}