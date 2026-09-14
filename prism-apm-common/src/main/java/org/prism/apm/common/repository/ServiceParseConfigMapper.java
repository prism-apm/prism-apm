package org.prism.apm.common.repository;

import org.prism.apm.common.model.entity.ServiceParseConfigEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 
 * @author john
 * @date 2024-10-17 11:31
 *
 */
public interface ServiceParseConfigMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ServiceParseConfigEntity record);

    int insertSelective(ServiceParseConfigEntity record);

    ServiceParseConfigEntity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ServiceParseConfigEntity record);

    int updateByPrimaryKey(ServiceParseConfigEntity record);

    List<ServiceParseConfigEntity> selectByAll(ServiceParseConfigEntity serviceParseConfigEntity);


    List<ServiceParseConfigEntity> selectByPage(
            @Param("entity") ServiceParseConfigEntity entity,
            @Param("limit") int limit,
            @Param("offset") int offset);



}