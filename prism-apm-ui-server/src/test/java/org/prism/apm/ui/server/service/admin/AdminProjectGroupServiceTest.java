package org.prism.apm.ui.server.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.mapper.project.ProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.ProjectProjectGroupMapper;
import org.prism.apm.ui.server.mapper.project.UserProjectGroupMapper;
import org.prism.apm.ui.server.entity.model.project.ProjectGroup;
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
class AdminProjectGroupServiceTest {

    @InjectMocks
    private AdminProjectGroupService adminProjectGroupService;

    @Mock
    private ProjectGroupMapper projectGroupMapper;

    @Mock
    private UserProjectGroupMapper userProjectGroupMapper;

    @Mock
    private ProjectProjectGroupMapper projectProjectGroupMapper;

    @Test
    void addProjectGroup_Success() {
        // Arrange: prepare a project group object,
 // and mock the name as missing
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setName("Test Group");
        when(projectGroupMapper.selectByName("Test Group")).thenReturn(null);
        when(projectGroupMapper.insert(any(ProjectGroup.class))).thenReturn(1);

        // Act
        boolean result = adminProjectGroupService.addProjectGroup(projectGroup);

        // Assert: Verify the return value true,
// and selectByName and insert are both called
        assertTrue(result);
        verify(projectGroupMapper).selectByName("Test Group");
        verify(projectGroupMapper).insert(projectGroup);
    }

    @Test
    void addProjectGroup_NameAlreadyExists() {
        // Arrange: prepare a project group object,
 // and mock the name as existing
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setName("Test Group");
        ProjectGroup existingGroup = new ProjectGroup();
        existingGroup.setId(1L);
        existingGroup.setName("Test Group");
        when(projectGroupMapper.selectByName("Test Group")).thenReturn(existingGroup);

        // Act
        boolean result = adminProjectGroupService.addProjectGroup(projectGroup);

        // Assert: Verify the return value false,
// and insert is not called
        assertFalse(result);
        verify(projectGroupMapper).selectByName("Test Group");
        verify(projectGroupMapper, never()).insert(any(ProjectGroup.class));
    }

    @Test
    void addProjectGroup_InsertFailure() {
        // Arrange: prepare a project group object,
        // mock the name as missing,
// but database insert failure
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setName("Test Group");
        when(projectGroupMapper.selectByName("Test Group")).thenReturn(null);
        when(projectGroupMapper.insert(any(ProjectGroup.class))).thenReturn(0);

        // Act
        boolean result = adminProjectGroupService.addProjectGroup(projectGroup);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(projectGroupMapper).selectByName("Test Group");
        verify(projectGroupMapper).insert(projectGroup);
    }

    @Test
    void modifyProjectGroup_Success() {
        // Arrange: prepare an object to update and mock conditions
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        projectGroup.setName("Updated Group");
        ProjectGroup existingGroup = new ProjectGroup();
        existingGroup.setId(1L);
        existingGroup.setName("Old Group");

        when(projectGroupMapper.selectById(1L)).thenReturn(existingGroup);
        when(projectGroupMapper.selectByName("Updated Group")).thenReturn(null);
        when(projectGroupMapper.updateById(any(ProjectGroup.class))).thenReturn(1);

        // Act
        boolean result = adminProjectGroupService.modifyProjectGroup(projectGroup);

        // Assert: Verify the return value true,
// and related query and update methods are called
        assertTrue(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper).selectByName("Updated Group");
        verify(projectGroupMapper).updateById(projectGroup);
    }

    @Test
    void modifyProjectGroup_NotExists() {
        // Arrange: prepare an object to update,
 // and mock the object as missing in the database
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        projectGroup.setName("Updated Group");

        when(projectGroupMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminProjectGroupService.modifyProjectGroup(projectGroup);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper, never()).updateById(any(ProjectGroup.class));
    }

    @Test
    void modifyProjectGroup_NameAlreadyUsedByAnother() {
        // Arrange: prepare an object to update,
 // and mock the new name as used by another project group
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        projectGroup.setName("Updated Group");
        ProjectGroup existingGroup = new ProjectGroup();
        existingGroup.setId(1L);
        existingGroup.setName("Old Group");
        ProjectGroup anotherGroup = new ProjectGroup();
        anotherGroup.setId(2L);
        anotherGroup.setName("Updated Group");

        when(projectGroupMapper.selectById(1L)).thenReturn(existingGroup);
        when(projectGroupMapper.selectByName("Updated Group")).thenReturn(anotherGroup);

        // Act
        boolean result = adminProjectGroupService.modifyProjectGroup(projectGroup);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper).selectByName("Updated Group");
        verify(projectGroupMapper, never()).updateById(any(ProjectGroup.class));
    }

    @Test
    void modifyProjectGroup_UpdateFailure() {
        // Arrange: mock database update failure
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        projectGroup.setName("Updated Group");
        ProjectGroup existingGroup = new ProjectGroup();
        existingGroup.setId(1L);
        existingGroup.setName("Old Group");

        when(projectGroupMapper.selectById(1L)).thenReturn(existingGroup);
        when(projectGroupMapper.selectByName("Updated Group")).thenReturn(null);
        when(projectGroupMapper.updateById(any(ProjectGroup.class))).thenReturn(0);

        // Act
        boolean result = adminProjectGroupService.modifyProjectGroup(projectGroup);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper).selectByName("Updated Group");
        verify(projectGroupMapper).updateById(projectGroup);
    }

    @Test
    void deleteProjectGroup_Success() {
        // Arrange: prepare an existing project group,
 // and mock selectById and deleteById
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        projectGroup.setName("Test Group");
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(projectGroupMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminProjectGroupService.deleteProjectGroup(1L);

        // Assert: Verify the return value true,
// and related methods are called
        assertTrue(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper).deleteById(1L);
    }

    @Test
    void deleteProjectGroup_NotExists() {
        // Arrange: mock project group as missing
        when(projectGroupMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminProjectGroupService.deleteProjectGroup(1L);

        // Assert: Verify the return value false,
// and deleteById is not called
        assertFalse(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper, never()).deleteById(1L);
    }

    @Test
    void deleteProjectGroup_DeleteFailure() {
        // Arrange: mock database delete failure
        ProjectGroup projectGroup = new ProjectGroup();
        projectGroup.setId(1L);
        projectGroup.setName("Test Group");
        when(projectGroupMapper.selectById(1L)).thenReturn(projectGroup);
        when(projectGroupMapper.deleteById(1L)).thenReturn(0);

        // Act
        boolean result = adminProjectGroupService.deleteProjectGroup(1L);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(projectGroupMapper).selectById(1L);
        verify(projectGroupMapper).deleteById(1L);
    }

    @Test
    void listProjectGroups_Success() {
        // Arrange: prepare pagination parameters
        PageParam pageParam = new PageParam();
        pageParam.setPageNum(1);
        pageParam.setPageSize(10);

        // Act: mock selectPage returning the expected Page object
        Page<ProjectGroup> expectedPage = new Page<>(1, 10);
        when(projectGroupMapper.selectPage(any(Page.class), isNull())).thenReturn(expectedPage);

        Page<ProjectGroup> resultPage = adminProjectGroupService.listProjectGroups(pageParam);

        // Assert: Verify the returned Page object is correct
        assertNotNull(resultPage);
        assertEquals(expectedPage, resultPage);

        // Assert: Verify selectPage parameters are correct
        ArgumentCaptor<Page<ProjectGroup>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(projectGroupMapper).selectPage(pageCaptor.capture(), isNull());
        Page<ProjectGroup> capturedPage = pageCaptor.getValue();
        assertEquals(1, capturedPage.getCurrent());
        assertEquals(10, capturedPage.getSize());
    }
}
