package org.prism.apm.ui.server.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.prism.apm.ui.server.mapper.security.UserMapper;
import org.prism.apm.ui.server.mapper.security.UserRoleMapper;
import org.prism.apm.ui.server.entity.model.security.User;
import org.prism.apm.ui.server.entity.param.common.PageParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserProjectGroupMapper userProjectGroupMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void addUser_Success() {
        // Arrange: prepare a User object,
 // and mock findByUsername returning null (username is unique)
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("plain-password");
        when(userMapper.findByUsername("testuser")).thenReturn(null);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // Act
        boolean result = adminUserService.addUser(user);

        // Assert: Verify the return value true,
// and findByUsername and insert method are called
        assertTrue(result);
        verify(userMapper).findByUsername("testuser");
        verify(passwordEncoder).encode("plain-password");
        verify(userMapper).insert(user);
        assertEquals("encoded-password", user.getPassword());
    }

    @Test
    void addUser_UsernameAlreadyExists() {
        // Arrange: prepare a User object,
 // and mock findByUsername returning an existing User (username is duplicated)
        User user = new User();
        user.setUsername("testuser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        when(userMapper.findByUsername("testuser")).thenReturn(existingUser);

        // Act
        boolean result = adminUserService.addUser(user);

        // Assert: Verify the return value false,
// and insert method is not called
        assertFalse(result);
        verify(userMapper).findByUsername("testuser");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void addUser_InsertFailure() {
        // Arrange: mock the username as unique,
// but database insert failure
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("plain-password");
        when(userMapper.findByUsername("testuser")).thenReturn(null);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenReturn(0);

        // Act
        boolean result = adminUserService.addUser(user);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(userMapper).findByUsername("testuser");
        verify(passwordEncoder).encode("plain-password");
        verify(userMapper).insert(user);
        assertEquals("encoded-password", user.getPassword());
    }

    @Test
    void modifyUser_Success() {
        // Arrange: prepare a User object to update and mock conditions
        User user = new User();
        user.setId(1L);
        user.setUsername("updateduser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("olduser");
        
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.findByUsername("updateduser")).thenReturn(null);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // Act
        boolean result = adminUserService.modifyUser(user);

        // Assert: Verify the return value true,
// and related query and update methods are called
        assertTrue(result);
        verify(userMapper).selectById(1L);
        verify(userMapper).findByUsername("updateduser");
        verify(userMapper).updateById(user);
    }

    @Test
    void modifyUser_NotExists() {
        // Arrange: mock the User to update as missing
        User user = new User();
        user.setId(1L);
        user.setUsername("updateduser");
        
        when(userMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminUserService.modifyUser(user);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void modifyUser_UsernameAlreadyUsedByAnother() {
        // Arrange: mock the new username as used by another User
        User user = new User();
        user.setId(1L);
        user.setUsername("updateduser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("olduser");
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("updateduser");
        
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.findByUsername("updateduser")).thenReturn(anotherUser);

        // Act
        boolean result = adminUserService.modifyUser(user);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(userMapper).selectById(1L);
        verify(userMapper).findByUsername("updateduser");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void modifyUser_UpdateFailure() {
        // Arrange: mock database update failure
        User user = new User();
        user.setId(1L);
        user.setUsername("updateduser");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("olduser");
        
        when(userMapper.selectById(1L)).thenReturn(existingUser);
        when(userMapper.findByUsername("updateduser")).thenReturn(null);
        when(userMapper.updateById(any(User.class))).thenReturn(0);

        // Act
        boolean result = adminUserService.modifyUser(user);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(userMapper).selectById(1L);
        verify(userMapper).findByUsername("updateduser");
        verify(userMapper).updateById(user);
    }

    @Test
    void deleteUser_Success() {
        // Arrange: prepare an existing User,
 // and mock selectById and deleteById
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminUserService.deleteUser(1L);

        // Assert: Verify the return value true,
// and related methods are called
        assertTrue(result);
        verify(userMapper).selectById(1L);
        verify(userMapper).deleteById(1L);
    }

    @Test
    void deleteUser_NotExists() {
        // Arrange: mock User as missing
        when(userMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminUserService.deleteUser(1L);

        // Assert: Verify the return value false,
// and deleteById is not called
        assertFalse(result);
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).deleteById(1L);
    }

    @Test
    void deleteUser_DeleteFailure() {
        // Arrange: mock database delete failure
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deleteById(1L)).thenReturn(0);

        // Act
        boolean result = adminUserService.deleteUser(1L);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(userMapper).selectById(1L);
        verify(userMapper).deleteById(1L);
    }

    @Test
    void listUsers_Success() {
        // Arrange: prepare pagination parameters
        PageParam pageParam = new PageParam();
        pageParam.setPageNum(1);
        pageParam.setPageSize(10);

        // Act: mock selectPage returning the expected Page object
        Page<User> expectedPage = new Page<>(1, 10);
        when(userMapper.selectPage(any(Page.class), isNull())).thenReturn(expectedPage);

        Page<User> resultPage = adminUserService.listUsers(pageParam);

        // Assert: Verify the returned Page object is correct
        assertNotNull(resultPage);
        assertEquals(expectedPage, resultPage);

        // Assert: Verify selectPage parameters are correct
        ArgumentCaptor<Page<User>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(userMapper).selectPage(pageCaptor.capture(), isNull());
        Page<User> capturedPage = pageCaptor.getValue();
        assertEquals(1, capturedPage.getCurrent());
        assertEquals(10, capturedPage.getSize());
    }
}
