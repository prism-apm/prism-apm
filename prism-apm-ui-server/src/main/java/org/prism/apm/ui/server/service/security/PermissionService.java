package org.prism.apm.ui.server.service.security;

import org.prism.apm.ui.server.entity.dto.security.PermissionWithRoleDTO;
import org.prism.apm.ui.server.mapper.security.PermissionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    /**
     * Query all permission rules and organize them into a URL -> required role set mapping
     * @return Map<String, Collection<String>>
     */
    public Map<String, Collection<String>> getPermissionMap() {
        // 1. Call the Mapper method to get the flattened permission-role list from the database
        List<PermissionWithRoleDTO> list = permissionMapper.getPermissionWithRoles();

        // 2. Process the flattened list into a Map grouped by URL
        Map<String, Collection<String>> map = new HashMap<>();
        for (PermissionWithRoleDTO item : list) {
            String url = item.getUrl();
            String roleName = item.getRoleName();

            // Remember to add the "ROLE_" prefix
            String attribute = "ROLE_" + roleName;

            // Use computeIfAbsent to simplify the code: if the key does not exist, create a new ArrayList and then add the element
            map.computeIfAbsent(url, k -> new ArrayList<>()).add(attribute);
        }

        return map;
    }
}