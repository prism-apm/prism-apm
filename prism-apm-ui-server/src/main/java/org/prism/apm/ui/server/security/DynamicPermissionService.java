package org.prism.apm.ui.server.security;

import com.google.common.collect.Maps;
import org.prism.apm.ui.server.service.security.PermissionService;
import org.prism.apm.ui.server.utils.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DynamicPermissionService {

    private final PermissionService permissionService;

    // Cache URL -> permission configuration mapping
    private volatile Map<String, Collection<String>> permissionMap = new HashMap<>();

    public DynamicPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostConstruct
    public void loadPermissionData() {
        refreshPermissionData();
        log.info("Permission configuration initialized successfully");
    }

    public void refreshPermissionData() {
        // Get the formatted Map directly from Service
        this.permissionMap = permissionService.getPermissionMap();
        log.debug("Permission configuration refreshed successfully, permission configuration:{}", JsonUtils.toJson(this.permissionMap));
    }

    /**
     * Provides all rules to AuthorizationManager
     */
    public Map<String, Collection<String>> getPermissionMap() {
        return Maps.newHashMap(permissionMap);
    }
}