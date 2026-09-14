package org.prism.apm.ui.server.service.admin;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.entity.model.project.UserProjectGroup;
import org.prism.apm.ui.server.entity.model.security.User;
import org.prism.apm.ui.server.entity.model.security.UserRole;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.prism.apm.ui.server.mapper.security.UserMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user management service
 */
@Service
@DS("master")
public class AdminUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserProjectGroupMapper userProjectGroupMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserMapper userMapper, UserRoleMapper userRoleMapper, UserProjectGroupMapper userProjectGroupMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.userProjectGroupMapper = userProjectGroupMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Add user
     */
    @Transactional
    public boolean addUser(User user) {
        User byUsername = userMapper.findByUsername(user.getUsername());
        if (byUsername != null) {
            return false;
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userMapper.insert(user) > 0;
    }

    /**
     * Update user
     */
    @Transactional
    public boolean modifyUser(User user) {
        User byId = userMapper.selectById(user.getId());
        if (byId == null) {
            return false;
        }
        User byUsername = userMapper.findByUsername(user.getUsername());
        if (byUsername != null && !byUsername.getId().equals(user.getId())) {
            return false;
        }
        // 密码留空表示不修改（updateById 默认跳过 null 字段）
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(null);
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.updateById(user) > 0;
    }

    /**
     * Delete user
     */
    @Transactional
    public boolean deleteUser(Long id) {
        User byId = userMapper.selectById(id);
        if (byId == null) {
            return false;
        }
        boolean relation_exist = userRoleMapper.selectCount(
                Wrappers.lambdaQuery(UserRole.class)
                        .eq(UserRole::getUserId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        relation_exist = userProjectGroupMapper.selectCount(
                Wrappers.lambdaQuery(UserProjectGroup.class)
                        .eq(UserProjectGroup::getUserId, id)) != 0;
        if (relation_exist) {
            return false;
        }
        return userMapper.deleteById(id) > 0;
    }

    /**
     * Query the user list with pagination
     */
    public Page<User> listUsers(PageParam pageRequest) {
        Page<User> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        return userMapper.selectPage(page, null);
    }
} 