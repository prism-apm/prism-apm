package org.prism.apm.ui.server.schedule;

import org.prism.apm.ui.server.security.DynamicPermissionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApmSchedule {

    private final DynamicPermissionService dynamicPermissionService;

    public ApmSchedule(DynamicPermissionService dynamicPermissionService) {
        this.dynamicPermissionService = dynamicPermissionService;
    }

    @Scheduled(fixedRate = 60 * 1000)
    public void refreshPermissionData() {
        dynamicPermissionService.refreshPermissionData();
    }

}
