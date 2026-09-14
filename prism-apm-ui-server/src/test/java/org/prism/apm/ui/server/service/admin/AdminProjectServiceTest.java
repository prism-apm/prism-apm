package org.prism.apm.ui.server.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.prism.apm.ui.server.mapper.project.ProjectMapper;
import org.prism.apm.ui.server.mapper.project.ProjectProjectGroupMapper;
import org.prism.apm.ui.server.entity.model.project.Project;
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
class AdminProjectServiceTest {

    @InjectMocks
    private AdminProjectService adminProjectService;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectProjectGroupMapper projectProjectGroupMapper;

    @Test
    void addProject_Success() {
        // Arrange: prepare a Project object,
 // and mock selectByName returning null (name is unique)
        Project project = new Project();
        project.setName("Test Project");
        when(projectMapper.selectByName("Test Project")).thenReturn(null);
        when(projectMapper.insert(any(Project.class))).thenReturn(1);

        // Act
        boolean result = adminProjectService.addProject(project);

        // Assert: Verify the return value true,
// and selectByName and insert method are called
        assertTrue(result);
        verify(projectMapper).selectByName("Test Project");
        verify(projectMapper).insert(project);
    }

    @Test
    void addProject_NameAlreadyExists() {
        // Arrange: prepare a Project object,
 // and mock selectByName returning an existing Project (name is duplicated)
        Project project = new Project();
        project.setName("Test Project");
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setName("Test Project");
        when(projectMapper.selectByName("Test Project")).thenReturn(existingProject);

        // Act
        boolean result = adminProjectService.addProject(project);

        // Assert: Verify the return value false,
// and insert method is not called
        assertFalse(result);
        verify(projectMapper).selectByName("Test Project");
        verify(projectMapper, never()).insert(any(Project.class));
    }

    @Test
    void addProject_InsertFailure() {
        // Arrange: mock the name as unique,
// but database insert failure
        Project project = new Project();
        project.setName("Test Project");
        when(projectMapper.selectByName("Test Project")).thenReturn(null);
        when(projectMapper.insert(any(Project.class))).thenReturn(0);

        // Act
        boolean result = adminProjectService.addProject(project);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(projectMapper).selectByName("Test Project");
        verify(projectMapper).insert(project);
    }

    @Test
    void modifyProject_Success() {
        // Arrange: prepare a Project object to update and mock conditions
        Project project = new Project();
        project.setId(1L);
        project.setName("Updated Project");
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setName("Old Project");
        
        when(projectMapper.selectById(1L)).thenReturn(existingProject);
        when(projectMapper.selectByName("Updated Project")).thenReturn(null);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        // Act
        boolean result = adminProjectService.modifyProject(project);

        // Assert: Verify the return value true,
// and related query and update methods are called
        assertTrue(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper).selectByName("Updated Project");
        verify(projectMapper).updateById(project);
    }

    @Test
    void modifyProject_NotExists() {
        // Arrange: mock the Project to update as missing
        Project project = new Project();
        project.setId(1L);
        project.setName("Updated Project");
        
        when(projectMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminProjectService.modifyProject(project);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper, never()).updateById(any(Project.class));
    }

    @Test
    void modifyProject_NameAlreadyUsedByAnother() {
        // Arrange: mock the new name as used by another Project
        Project project = new Project();
        project.setId(1L);
        project.setName("Updated Project");
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setName("Old Project");
        Project anotherProject = new Project();
        anotherProject.setId(2L);
        anotherProject.setName("Updated Project");
        
        when(projectMapper.selectById(1L)).thenReturn(existingProject);
        when(projectMapper.selectByName("Updated Project")).thenReturn(anotherProject);

        // Act
        boolean result = adminProjectService.modifyProject(project);

        // Assert: Verify the return value false,
// and updateById is not called
        assertFalse(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper).selectByName("Updated Project");
        verify(projectMapper, never()).updateById(any(Project.class));
    }

    @Test
    void modifyProject_UpdateFailure() {
        // Arrange: mock database update failure
        Project project = new Project();
        project.setId(1L);
        project.setName("Updated Project");
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setName("Old Project");
        
        when(projectMapper.selectById(1L)).thenReturn(existingProject);
        when(projectMapper.selectByName("Updated Project")).thenReturn(null);
        when(projectMapper.updateById(any(Project.class))).thenReturn(0);

        // Act
        boolean result = adminProjectService.modifyProject(project);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper).selectByName("Updated Project");
        verify(projectMapper).updateById(project);
    }

    @Test
    void deleteProject_Success() {
        // Arrange: prepare an existing Project,
 // and mock selectById and deleteById
        Project project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectMapper.deleteById(1L)).thenReturn(1);

        // Act
        boolean result = adminProjectService.deleteProject(1L);

        // Assert: Verify the return value true,
// and related methods are called
        assertTrue(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper).deleteById(1L);
    }

    @Test
    void deleteProject_NotExists() {
        // Arrange: mock Project as missing
        when(projectMapper.selectById(1L)).thenReturn(null);

        // Act
        boolean result = adminProjectService.deleteProject(1L);

        // Assert: Verify the return value false,
// and deleteById is not called
        assertFalse(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper, never()).deleteById(1L);
    }

    @Test
    void deleteProject_DeleteFailure() {
        // Arrange: mock database delete failure
        Project project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectMapper.deleteById(1L)).thenReturn(0);

        // Act
        boolean result = adminProjectService.deleteProject(1L);

        // Assert: Verify the return value false
        assertFalse(result);
        verify(projectMapper).selectById(1L);
        verify(projectMapper).deleteById(1L);
    }

    @Test
    void listProjects_Success() {
        // Arrange: prepare pagination parameters
        PageParam pageParam = new PageParam();
        pageParam.setPageNum(1);
        pageParam.setPageSize(10);

        // Act: mock selectPage returning the expected Page object
        Page<Project> expectedPage = new Page<>(1, 10);
        when(projectMapper.selectPage(any(Page.class), isNull())).thenReturn(expectedPage);

        Page<Project> resultPage = adminProjectService.listProjects(pageParam);

        // Assert: Verify the returned Page object is correct
        assertNotNull(resultPage);
        assertEquals(expectedPage, resultPage);

        // Assert: Verify selectPage parameters are correct
        ArgumentCaptor<Page<Project>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(projectMapper).selectPage(pageCaptor.capture(), isNull());
        Page<Project> capturedPage = pageCaptor.getValue();
        assertEquals(1, capturedPage.getCurrent());
        assertEquals(10, capturedPage.getSize());
    }
} 