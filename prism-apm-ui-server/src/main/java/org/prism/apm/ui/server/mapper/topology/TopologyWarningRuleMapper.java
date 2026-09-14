package org.prism.apm.ui.server.mapper.topology;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.prism.apm.ui.server.entity.model.topology.TopologyWarningRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("master")
public interface TopologyWarningRuleMapper extends BaseMapper<TopologyWarningRule> {

}
