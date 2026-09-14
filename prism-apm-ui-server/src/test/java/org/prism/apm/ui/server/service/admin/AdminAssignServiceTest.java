package org.prism.apm.ui.server.service.admin;

import org.prism.apm.ui.server.mapper.project.ProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.ProjectMapper;
import org.prism.apm.ui.server.mapper.project.ProjectProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.prism.apm.ui.server.mapper.security.PermissionMapper;
import org.prism.apm.ui.server.mapper.security.RoleMapper;
import org.prism.apm.ui.server.mapper.security.RolePermissionMapper;
import org.prism.apm.ui.server.mapper.security.UserMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.prism.apm.ui.server.entity.model.project.Project;
import org.prism.apm.ui.server.entity.model.project.ProjectGroup;
import org.prism.apm.ui.server.entity.model.project.ProjectProjectGroup;
import org.prism.apm.ui.server.entity.model.project.UserProjectGroup;
import org.prism.apm.ui.server.entity.model.security.Permission;
import org.prism.apm.ui.server.entity.model.security.Role;
import org.prism.apm.ui.server.entity.model.security.RolePermission;
import org.prism.apm.ui.server.entity.model.security.User;
import org.prism.apm.ui.server.entity.model.security.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminAssignServiceTest {

    @InjectMocks
    private AdminAssignService adminAssignService;

    @Mock
    private UserMapper userMapper;
    
    @Mock
    private RoleMapper roleMapper;
    
    @Mock
    private PermissionMapper permissionMapper;
    
    @Mock
    private ProjectGroupMapper projectGroupMapper;
    
    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Mock
    private ProjectProjectGroupMapper projectProjectGroupMapper;

    @Mock
    private UserProjectGroupMapper userProjectGroupMapper;

    // ==================== User-role assignment tests ====================

    @Test
    void assignRoleToUser_Success() {
        // Arrange: prepare data
        User user = new User();
        user.setId(1L);
        Role role = new Role();
        role.setId(1L);
        
        // Act: mock user and role exist,
// and there is no duplicate relation
        when(userMapper.selectById(1L)).thenReturn(user);
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(userRoleMapper.selectByUserIdAndRoleId(1L, 1L)).thenReturn(null);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        boolean result = adminAssignService.assignRoleToUser(1L, 1L);

        // Assert: Verify the result is true,
// and verify related methods are called
        assertTrue(result);
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_UserNotExists() {
        // Arrange: mock user as missing
        when(userMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignRoleToUser(1L, 1L);

        // Assert: Verify the result is false,
// and insert method is not called
        assertFalse(result);
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_RoleNotExists() {
        // Arrange: mock role as missing
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(roleMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignRoleToUser(1L, 1L);

        // Assert: Verify the result is false,
// and insert method is not called
        assertFalse(result);
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_RelationAlreadyExists() {
        // Arrange: mock relation as existing
        User user = new User();
        user.setId(1L);
        Role role = new Role();
        role.setId(1L);
        UserRole existingRelation = new UserRole();
        existingRelation.setId(1L);
        
        when(userMapper.selectById(1L)).thenReturn(user);
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(userRoleMapper.selectByUserIdAndRoleId(1L, 1L)).thenReturn(existingRelation);

        // Act
        boolean result = adminAssignService.assignRoleToUser(1L, 1L);

        // Assert: Verify the result is false,
// and insert method is not called
        assertFalse(result);
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void removeUserRole_Success() {
        // Arrange: prepare data and mocks
        UserRole userRole = new UserRole();
        userRole.setId(1L);
        when(userRoleMapper.selectById(1L)).thenReturn(userRole);
        when(userRoleMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminAssignService.removeUserRole(1L);

        // Assert: Verify the result is true,
// and verify deleteById is called
        assertTrue(result);
        verify(userRoleMapper).deleteById(1L);
    }

    @Test
    void removeUserRole_RelationNotExists() {
        // Arrange: mock relation as missing
        when(userRoleMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.removeUserRole(1L);

        // Assert: Verify the result is false,
// and deleteById is not called
        assertFalse(result);
        verify(userRoleMapper, never()).deleteById(1L);
    }

    // ==================== Role-permission assignment tests ====================

    @Test
    void assignPermissionToRole_Success() {
        // Arrange: prepare data and mocks
        Role role = new Role();
        role.setId(1L);
        Permission permission = new Permission();
        permission.setId(1L);
        
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(permissionMapper.selectById(1L)).thenReturn(permission);
        when(rolePermissionMapper.selectByRoleIdAndPermissionId(1L, 1L)).thenReturn(null);
        when(rolePermissionMapper.insert(any(RolePermission.class))).thenReturn(1);

        // Act
        boolean result = adminAssignService.assignPermissionToRole(1L, 1L);

        // Assert: Verify the result and method calls
        assertTrue(result);
        verify(rolePermissionMapper).insert(any(RolePermission.class));
    }

    @Test
    void assignPermissionToRole_RoleNotExists() {
        // Arrange: mock role as missing
        when(roleMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignPermissionToRole(1L, 1L);

        // Assert: Verify failure and no new relation is inserted
        assertFalse(result);
        verify(rolePermissionMapper, never()).insert(any(RolePermission.class));
    }

    @Test
    void assignPermissionToRole_PermissionNotExists() {
        // Arrange: mock permission as missing
        Role role = new Role();
        role.setId(1L);
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(permissionMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignPermissionToRole(1L, 1L);

        // Assert: Verify failure and no new relation is inserted
        assertFalse(result);
        verify(rolePermissionMapper, never()).insert(any(RolePermission.class));
    }

    @Test
    void assignPermissionToRole_RelationAlreadyExists() {
        // Arrange: mock relation as existing
        Role role = new Role();
        role.setId(1L);
        Permission permission = new Permission();
        permission.setId(1L);
        RolePermission existingRelation = new RolePermission();
        existingRelation.setId(1L);
        
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(permissionMapper.selectById(1L)).thenReturn(permission);
        when(rolePermissionMapper.selectByRoleIdAndPermissionId(1L, 1L)).thenReturn(existingRelation);

        // Act
        boolean result = adminAssignService.assignPermissionToRole(1L, 1L);

        // Assert: Verify failure and no new relation is inserted
        assertFalse(result);
        verify(rolePermissionMapper, never()).insert(any(RolePermission.class));
    }

    @Test
    void removeRolePermission_Success() {
        // Arrange: prepare data and mocks
        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(1L);
        when(rolePermissionMapper.selectById(1L)).thenReturn(rolePermission);
        when(rolePermissionMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminAssignService.removeRolePermission(1L);

        // Assert: Verify success and method calls
        assertTrue(result);
        verify(rolePermissionMapper).deleteById(1L);
    }

    @Test
    void removeRolePermission_RelationNotExists() {
        // Arrange: mock relation as missing
        when(rolePermissionMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.removeRolePermission(1L);

        // Assert: Verify failure and no delete
        assertFalse(result);
        verify(rolePermissionMapper, never()).deleteById(1L);
    }

    // ==================== Project group-project assignment tests ====================

    @Test
    void assignProjectToProjectGroup_Success() {
        // Arrange: prepare data and mocks
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        Project project = new Project();
        project.setId(1L);
        
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectProjectGroupMapper.selectByProjectIdAndProjectGroupId(1L, 1L)).thenReturn(null);
        when(projectProjectGroupMapper.insert(any(ProjectProjectGroup.class))).thenReturn(1);

        // Act
        boolean result = adminAssignService.assignProjectToProjectGroup(1L, 1L);

        // Assert: Verify success and method calls
        assertTrue(result);
        verify(projectProjectGroupMapper).insert(any(ProjectProjectGroup.class));
    }

    @Test
    void assignProjectToProjectGroup_ProjectGroupNotExists() {
        // Arrange: mock project group as missing
        when(projectGroupMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignProjectToProjectGroup(1L, 1L);

        // Assert: Verify failure and no new relation is inserted
        assertFalse(result);
        verify(projectProjectGroupMapper, never()).insert(any(ProjectProjectGroup.class));
    }

    @Test
    void assignProjectToProjectGroup_ProjectNotExists() {
        // Arrange: mock project as missing
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(projectMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignProjectToProjectGroup(1L, 1L);

        // Assert: Verify failure and no new relation is inserted
        assertFalse(result);
        verify(projectProjectGroupMapper, never()).insert(any(ProjectProjectGroup.class));
    }

    @Test
    void assignProjectToProjectGroup_RelationAlreadyExists() {
        // Arrange: mock relation as existing
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        Project project = new Project();
        project.setId(1L);
        ProjectProjectGroup existingRelation = new ProjectProjectGroup();
        existingRelation.setId(1L);
        
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectProjectGroupMapper.selectByProjectIdAndProjectGroupId(1L, 1L)).thenReturn(existingRelation);

        // Act
        boolean result = adminAssignService.assignProjectToProjectGroup(1L, 1L);

        // Assert: Verify failure and no new relation is inserted
        assertFalse(result);
        verify(projectProjectGroupMapper, never()).insert(any(ProjectProjectGroup.class));
    }

    @Test
    void removeProjectGroupProject_Success() {
        // Arrange: prepare data and mocks
        ProjectProjectGroup relation = new ProjectProjectGroup();
        relation.setId(1L);
        when(projectProjectGroupMapper.selectById(1L)).thenReturn(relation);
        when(projectProjectGroupMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminAssignService.removeProjectGroupProject(1L);

        // Assert: Verify success and method calls
        assertTrue(result);
        verify(projectProjectGroupMapper).deleteById(1L);
    }

    @Test
    void removeProjectGroupProject_RelationNotExists() {
        // Arrange: mock relation as missing
        when(projectProjectGroupMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.removeProjectGroupProject(1L);

        // Assert: Verify failure and no delete
        assertFalse(result);
        verify(projectProjectGroupMapper, never()).deleteById(1L);
    }

    // ==================== Project group-user assignment tests ====================

    @Test
    void assignUserToProjectGroup_Success() {
        // Arrange: prepare data and mocks
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        User user = new User();
        user.setId(1L);
        
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userProjectGroupMapper.selectByUserIdAndProjectGroupId(1L, 1L)).thenReturn(null);
        when(userProjectGroupMapper.insert(any(UserProjectGroup.class))).thenReturn(1);

        // Act
        boolean result = adminAssignService.assignUserToProjectGroup(1L, 1L);

        // Assert: Verify success and method calls
        assertTrue(result);
        verify(userProjectGroupMapper).insert(any(UserProjectGroup.class));
    }

    @Test
    void assignUserToProjectGroup_ProjectGroupNotExists() {
        // Arrange: mock project group as missing
        when(projectGroupMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignUserToProjectGroup(1L, 1L);

        // Assert: Verify failure and no insert
        assertFalse(result);
        verify(userProjectGroupMapper, never()).insert(any(UserProjectGroup.class));
    }

    @Test
    void assignUserToProjectGroup_UserNotExists() {
        // Arrange: mock user as missing
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(userMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.assignUserToProjectGroup(1L, 1L);

        // Assert: Verify failure and no insert
        assertFalse(result);
        verify(userProjectGroupMapper, never()).insert(any(UserProjectGroup.class));
    }

    @Test
    void assignUserToProjectGroup_RelationAlreadyExists() {
        // Arrange: mock relation as existing
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        User user = new User();
        user.setId(1L);
        UserProjectGroup existingRelation = new UserProjectGroup();
        existingRelation.setId(1L);
        
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userProjectGroupMapper.selectByUserIdAndProjectGroupId(1L, 1L)).thenReturn(existingRelation);

        // Act
        boolean result = adminAssignService.assignUserToProjectGroup(1L, 1L);

        // Assert: Verify failure and no insert
        assertFalse(result);
        verify(userProjectGroupMapper, never()).insert(any(UserProjectGroup.class));
    }

    @Test
    void removeProjectGroupUser_Success() {
        // Arrange: prepare data and mocks
        UserProjectGroup relation = new UserProjectGroup();
        relation.setId(1L);
        when(userProjectGroupMapper.selectById(1L)).thenReturn(relation);
        when(userProjectGroupMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminAssignService.removeProjectGroupUser(1L);

        // Assert: Verify success and method calls
        assertTrue(result);
        verify(userProjectGroupMapper).deleteById(1L);
    }

    @Test
    void removeProjectGroupUser_RelationNotExists() {
        // Arrange: mock relation as missing
        when(userProjectGroupMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminAssignService.removeProjectGroupUser(1L);

        // Assert: Verify failure and no delete
        assertFalse(result);
        verify(userProjectGroupMapper, never()).deleteById(1L);
    }
} 