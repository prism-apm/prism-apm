package org.prism.apm.ui.server.service.topology;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.prism.apm.ui.server.entity.dto.security.UserDTO;
import org.prism.apm.ui.server.entity.model.topology.TopologyWarningRule;
import org.prism.apm.ui.server.mapper.topology.TopologyWarningRuleMapper;
import org.prism.apm.ui.server.service.service.CommonService;
import org.prism.apm.ui.server.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class TopologyWarningRuleService {

    private final TopologyWarningRuleMapper topologyWarningRuleMapper;
    private final CommonService commonService;

    public TopologyWarningRuleService(TopologyWarningRuleMapper topologyWarningRuleMapper, CommonService commonService) {
        this.topologyWarningRuleMapper = topologyWarningRuleMapper;
        this.commonService = commonService;
    }

    public List<TopologyWarningRule> getRuleList(TopologyWarningRule topologyWarningRule){
        List<String> grantedServiceNames = commonService.getCurrentUserServiceNameList();
        if (CollectionUtils.isEmpty(grantedServiceNames)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TopologyWarningRule> queryWrapper = new LambdaQueryWrapper<>();
        if (topologyWarningRule.getEnabled() != null) {
            queryWrapper.eq(TopologyWarningRule::getEnabled, topologyWarningRule.getEnabled());
        }
        UserDTO current_user = SecurityUtils.getCurrentUser();
        boolean current_user_admin = SecurityUtils.isCurrentUserAdmin(current_user);
        if (!current_user_admin) {
            queryWrapper.in(TopologyWarningRule::getServiceName, grantedServiceNames);
            queryWrapper.eq(TopologyWarningRule::getUserId, current_user.getUserId());
        }
        queryWrapper.orderByDesc(TopologyWarningRule::getUpdatedAt);
        return topologyWarningRuleMapper.selectList(queryWrapper);
    }

    public Integer addRule(TopologyWarningRule topologyWarningRule) {
        UserDTO current_user = SecurityUtils.getCurrentUser();
        topologyWarningRule.setUserId(current_user.getUserId());
        return topologyWarningRuleMapper.insert(topologyWarningRule);
    }

    public Integer updateRule(TopologyWarningRule rule){
        UserDTO current_user = SecurityUtils.getCurrentUser();
        rule.setUserId(current_user.getUserId());
        rule.setUpdatedAt(new Date());
        return topologyWarningRuleMapper.updateById(rule);
    }

    public Integer deleteRule(TopologyWarningRule topologyWarningRule){
        return topologyWarningRuleMapper.deleteById(topologyWarningRule);
    }

    public Integer switchRuleEnableStatus(Integer id, Boolean isEnable){
        TopologyWarningRule rule = topologyWarningRuleMapper.selectById(id);
        Integer enable_int = isEnable ? 1 : 0;
        rule.setEnabled(enable_int);
        rule.setUpdatedAt(new Date());
        return topologyWarningRuleMapper.updateById(rule);
    }

}
