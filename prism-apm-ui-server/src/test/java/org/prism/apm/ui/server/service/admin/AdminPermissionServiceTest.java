package org.prism.apm.ui.server.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.mapper.security.PermissionMapper;
import org.prism.apm.ui.server.mapper.security.RolePermissionMapper;
import org.prism.apm.ui.server.entity.model.security.Permission;
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
class AdminPermissionServiceTest {

    @InjectMocks
    private AdminPermissionService adminPermissionService;

    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Test
    void addPermission_Success() {
        // Arrange: prepare a Permission object
        Permission permission = new Permission();
        permission.setName("test:permission");
        // Act: mock permissionMapper.insert returning 1 to indicate insert success
        when(permissionMapper.insert(any(Permission.class))).thenReturn(1);

        boolean result = adminPermissionService.addPermission(permission);

        // Assert: Verify the return valueis true,
// and insert method is called correctly
        assertTrue(result);
        verify(permissionMapper).insert(permission);
    }

    @Test
    void addPermission_Failure() {
        // Arrange: prepare a Permission object
        Permission permission = new Permission();
        permission.setName("test:permission");
        // Act: mock permissionMapper.insert returning 0 to indicate insert failure
        when(permissionMapper.insert(any(Permission.class))).thenReturn(0);

        boolean result = adminPermissionService.addPermission(permission);

        // Assert: Verify the return valueis false
        assertFalse(result);
        verify(permissionMapper).insert(permission);
    }

    @Test
    void modifyPermission_Success() {
        // Arrange: prepare a Permission object to update
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("test:updated_permission");
        // Act: mock updateById returning 1 to indicate update success
        when(permissionMapper.updateById(any(Permission.class))).thenReturn(1);

        boolean result = adminPermissionService.modifyPermission(permission);

        // Assert: Verify the return valueis true,
// and updateById method is called correctly
        assertTrue(result);
        verify(permissionMapper).updateById(permission);
    }

    @Test
    void modifyPermission_Failure() {
        // Arrange: prepare a Permission object to update
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("test:updated_permission");
        // Act: mock updateById returning 0 to indicate update failure
        when(permissionMapper.updateById(any(Permission.class))).thenReturn(0);

        boolean result = adminPermissionService.modifyPermission(permission);

        // Assert: Verify the return valueis false
        assertFalse(result);
        verify(permissionMapper).updateById(permission);
    }

    @Test
    void deletePermission_Success() {
        // Arrange: prepare an existing Permission object,
 // and mock selectById and deleteById
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("test:permission");
        when(permissionMapper.selectById(1L)).thenReturn(permission);
        when(permissionMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminPermissionService.deletePermission(1L);

        // Assert: Verify the return valueis true,
// and selectById and deleteById are both called
        assertTrue(result);
        verify(permissionMapper).selectById(1L);
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    void deletePermission_NotExists() {
        // Arrange: mock selectById returning null, meaning the permission does not exist
        when(permissionMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminPermissionService.deletePermission(1L);

        // Assert: Verify the return valueis false,
// and deleteById is not called
        assertFalse(result);
        verify(permissionMapper).selectById(1L);
        verify(permissionMapper, never()).deleteById(1L);
    }

    @Test
    void deletePermission_Failure() {
        // Arrange: mock selectById finding an object, but deleteById returning 0 to indicate delete failure
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("test:permission");
        when(permissionMapper.selectById(1L)).thenReturn(permission);
        when(permissionMapper.deleteById(1L)).thenReturn(0);

        // Act
        boolean result = adminPermissionService.deletePermission(1L);

        // Assert: Verify the return valueis false
        assertFalse(result);
        verify(permissionMapper).selectById(1L);
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    void listPermissions_Success() {
        // Arrange: prepare pagination parameters
        PageParam pageParam = new PageParam();
        pageParam.setPageNum(1);
        pageParam.setPageSize(10);

        // Act: mock selectPage returning an expected Page object
        Page<Permission> expectedPage = new Page<>(1, 10);
        when(permissionMapper.selectPage(any(Page.class), isNull())).thenReturn(expectedPage);

        Page<Permission> resultPage = adminPermissionService.listPermissions(pageParam);

        // Assert: Verify the returned Page object is not null and matches expectations
        assertNotNull(resultPage);
        assertEquals(expectedPage, resultPage);

        // Assert: Verify selectPage is called with the correct Page parameters
        ArgumentCaptor<Page<Permission>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(permissionMapper).selectPage(pageCaptor.capture(), isNull());
        Page<Permission> capturedPage = pageCaptor.getValue();
        assertEquals(1, capturedPage.getCurrent());
        assertEquals(10, capturedPage.getSize());
    }
} 