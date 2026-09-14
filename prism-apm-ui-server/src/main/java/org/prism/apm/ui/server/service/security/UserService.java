package org.prism.apm.ui.server.service.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.prism.apm.ui.server.entity.model.security.Role;
import org.prism.apm.ui.server.entity.model.security.User;
import org.prism.apm.ui.server.mapper.security.RoleMapper;
import org.prism.apm.ui.server.mapper.security.UserMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public List<Role> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.in("id", roleIds);
        return roleMapper.selectList(wrapper);
    }

    public List<String> getUserRoleNames(Long userId) {
        List<Role> roles = getUserRoles(userId);
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }
} 