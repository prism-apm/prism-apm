package org.prism.apm.ui.server.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.mapper.security.RoleMapper;
import org.prism.apm.ui.server.mapper.security.RolePermissionMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.prism.apm.ui.server.entity.model.security.Role;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminRoleConstantsServiceTest {

    @InjectMocks
    private AdminRoleService adminRoleService;

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Test
    void addRole_Success() {
        // Arrange: prepare a Role object,
 // and mock findByName returning null (name is unique)
        Role role = new Role();
        role.setName("ROLE_TEST");
        when(roleMapper.findByName("ROLE_TEST")).thenReturn(null);
        when(roleMapper.insert(any(Role.class))).thenReturn(1);

        // Act
        boolean result = adminRoleService.addRole(role);

        // Assert: Verify the return value true,
// and findByName and insert method are called
        assertTrue(result);
        verify(roleMapper).findByName("ROLE_TEST");
        verify(roleMapper).insert(role);
    }

    @Test
    void addRole_NameAlreadyExists() {
        // Arrange: prepare a Role object,
 // and mock findByName returning an existing Role (name is duplicated)
        Role role = new Role();
        role.setName("ROLE_TEST");
        Role existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setName("ROLE_TEST");
        when(roleMapper.findByName("ROLE_TEST")).thenReturn(existingRole);

        // Act
        boolean result = adminRoleService.addRole(role);

        // Assert: Verify the return value false,
// and insert method is not called
        assertFalse(result);
        verify(roleMapper).findByName("ROLE_TEST");
        verify(roleMapper, never()).insert(any(Role.class));
    }

    @Test
    void addRole_InsertFailure() {
        // Arrange: mock the name as unique,
// but database insert failure
        Role role = new Role();
        role.setName("ROLE_TEST");
        when(roleMapper.findByName("ROLE_TEST")).thenReturn(null);
        when(roleMapper.insert(any(Role.class))).thenReturn(0);

        // Act
        boolean result = adminRoleService.addRole(role);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(roleMapper).findByName("ROLE_TEST");
        verify(roleMapper).insert(role);
    }

    @Test
    void modifyRole_Success() {
        // Arrange: prepare a Role object to update and mock conditions
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_UPDATED");
        Role existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setName("ROLE_OLD");
        
        when(roleMapper.selectById(1L)).thenReturn(existingRole);
        when(roleMapper.findByName("ROLE_UPDATED")).thenReturn(null);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        // Act
        boolean result = adminRoleService.modifyRole(role);

        // Assert: Verify the return value true,
// and related query and update methods are called
        assertTrue(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper).findByName("ROLE_UPDATED");
        verify(roleMapper).updateById(role);
    }

    @Test
    void modifyRole_NotExists() {
        // Arrange: mock the Role to update as missing
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_UPDATED");
        
        when(roleMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminRoleService.modifyRole(role);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper, never()).updateById(any(Role.class));
    }

    @Test
    void modifyRole_NameAlreadyUsedByAnother() {
        // Arrange: mock the new name as used by another Role
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_UPDATED");
        Role existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setName("ROLE_OLD");
        Role anotherRole = new Role();
        anotherRole.setId(2L);
        anotherRole.setName("ROLE_UPDATED");
        
        when(roleMapper.selectById(1L)).thenReturn(existingRole);
        when(roleMapper.findByName("ROLE_UPDATED")).thenReturn(anotherRole);

        // Act
        boolean result = adminRoleService.modifyRole(role);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper).findByName("ROLE_UPDATED");
        verify(roleMapper, never()).updateById(any(Role.class));
    }

    @Test
    void modifyRole_UpdateFailure() {
        // Arrange: mock database update failure
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_UPDATED");
        Role existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setName("ROLE_OLD");
        
        when(roleMapper.selectById(1L)).thenReturn(existingRole);
        when(roleMapper.findByName("ROLE_UPDATED")).thenReturn(null);
        when(roleMapper.updateById(any(Role.class))).thenReturn(0);

        // Act
        boolean result = adminRoleService.modifyRole(role);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper).findByName("ROLE_UPDATED");
        verify(roleMapper).updateById(role);
    }

    @Test
    void deleteRole_Success() {
        // Arrange: prepare an existing Role,
 // and mock selectById and deleteById
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_TEST");
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(roleMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminRoleService.deleteRole(1L);

        // Assert: Verify the return value true,
// and related methods are called
        assertTrue(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper).deleteById(1L);
    }

    @Test
    void deleteRole_NotExists() {
        // Arrange: mock Role as missing
        when(roleMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminRoleService.deleteRole(1L);

        // Assert: Verify the return value false,
// and deleteById is not called
        assertFalse(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper, never()).deleteById(1L);
    }

    @Test
    void deleteRole_DeleteFailure() {
        // Arrange: mock database delete failure
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_TEST");
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(roleMapper.deleteById(1L)).thenReturn(0);

        // Act
        boolean result = adminRoleService.deleteRole(1L);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(roleMapper).selectById(1L);
        verify(roleMapper).deleteById(1L);
    }

    @Test
    void listRoles_Success() {
        // Arrange: prepare pagination parameters
        PageParam pageParam = new PageParam();
        pageParam.setPageNum(1);
        pageParam.setPageSize(10);

        // Act: mock selectPage returning the expected Page object
        Page<Role> expectedPage = new Page<>(1, 10);
        when(roleMapper.selectPage(any(Page.class), isNull())).thenReturn(expectedPage);

        Page<Role> resultPage = adminRoleService.listRoles(pageParam);

        // Assert: Verify the returned Page object is correct
        assertNotNull(resultPage);
        assertEquals(expectedPage, resultPage);

        // Assert: Verify selectPage parameters are correct
        ArgumentCaptor<Page<Role>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(roleMapper).selectPage(pageCaptor.capture(), isNull());
        Page<Role> capturedPage = pageCaptor.getValue();
        assertEquals(1, capturedPage.getCurrent());
        assertEquals(10, capturedPage.getSize());
    }
} 