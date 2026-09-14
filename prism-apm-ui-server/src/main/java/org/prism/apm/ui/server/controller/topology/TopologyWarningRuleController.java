package org.prism.apm.ui.server.controller.topology;

import org.prism.apm.ui.server.entity.model.topology.TopologyWarningRule;
import org.prism.apm.ui.server.entity.param.common.OnlyIdParam;
import org.prism.apm.ui.server.entity.request.topology.SwitchEnableStatusRequest;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.service.topology.TopologyWarningRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apm/topology/topology-warning-rules")
public class TopologyWarningRuleController {

    private final TopologyWarningRuleService topologyWarningRuleService;

    public TopologyWarningRuleController(TopologyWarningRuleService topologyWarningRuleService) {
        this.topologyWarningRuleService = topologyWarningRuleService;
    }

    @GetMapping
    public ViewResponse.Result<List<TopologyWarningRule>> getRuleList(TopologyWarningRule topologyWarningRule) {
        return ViewResponse.Result.ok(topologyWarningRuleService.getRuleList(topologyWarningRule));
    }

    @PostMapping("/add")
    public ViewResponse.Result<Integer> addRule(@RequestBody TopologyWarningRule topologyWarningRule) {
        return ViewResponse.Result.ok(topologyWarningRuleService.addRule(topologyWarningRule));
    }

    @PostMapping("/update")
    public ViewResponse.Result<Integer> updateRule(@RequestBody TopologyWarningRule topologyWarningRule) {
        return ViewResponse.Result.ok(topologyWarningRuleService.updateRule(topologyWarningRule));
    }

    @PostMapping("/delete")
    public ViewResponse.Result<Integer> deleteRule(@RequestBody OnlyIdParam idParam) {
        TopologyWarningRule rule = new TopologyWarningRule();
        rule.setId(idParam.getIdInteger());
        return ViewResponse.Result.ok(topologyWarningRuleService.deleteRule(rule));
    }

    @PostMapping("/switch-enable-status")
    public ViewResponse.Result<Integer> switchRuleEnableStatus(@RequestBody SwitchEnableStatusRequest request) {
        return ViewResponse.Result.ok(topologyWarningRuleService.switchRuleEnableStatus(request.getId(), request.getIsEnable()));
    }
}

