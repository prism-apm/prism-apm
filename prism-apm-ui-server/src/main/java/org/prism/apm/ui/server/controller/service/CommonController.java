package org.prism.apm.ui.server.controller.service;

import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.service.service.CommonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apm/common")
public class CommonController {
    private final CommonService commonService;

    public CommonController(CommonService commonService) {
        this.commonService = commonService;
    }

    /**
     * Request the full service list
     */
    @RequestMapping("/service-list")
    public ViewResponse.Result<List<String>> getServiceList() {
        List<String> list = commonService.getCurrentUserServiceNameList();
        return ViewResponse.Result.ok(list);
    }

    @RequestMapping("/service-object-list")
    public ViewResponse.Result<List<Project>> getServiceObjectList() {
        List<Project> list = commonService.getCurrentUserServiceList();
        return ViewResponse.Result.ok(list);
    }

    /**
     * Request all available host names by time range
     */
    @RequestMapping("/machine-list")
    public ViewResponse.Result<List<String>> getMachineList(DateParam dateParam, String serviceName) {
        List<String> list = commonService.getMachineList(serviceName, dateParam);
        return ViewResponse.Result.ok(list);
    }

    @GetMapping("/time-axis")
    public ViewResponse.Result<List<String>> getTimeAxis(DateParam dateParam) {
        List<String> list = commonService.getTimeAxis(dateParam);
        return ViewResponse.Result.ok(list);
    }

}
