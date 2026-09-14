package org.prism.apm.common.repository;

import org.prism.apm.common.model.entity.FieldContentReplacerRuleEntity;

import java.util.List;

/**
 * @author john
 * @date 2025-09-09 18:30
 */
public interface FieldContentReplacerRuleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(FieldContentReplacerRuleEntity record);

    int insertSelective(FieldContentReplacerRuleEntity record);

    FieldContentReplacerRuleEntity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(FieldContentReplacerRuleEntity record);

    int updateByPrimaryKey(FieldContentReplacerRuleEntity record);

    List<FieldContentReplacerRuleEntity> selectByAll(FieldContentReplacerRuleEntity fieldContentReplacerRuleEntity);
}