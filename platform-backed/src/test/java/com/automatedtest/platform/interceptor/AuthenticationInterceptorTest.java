package com.automatedtest.platform.interceptor;

import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.ProjectApiKey;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationInterceptorTest {

    @InjectMocks
    private AuthenticationInterceptor interceptor;

    @Mock
    private UserService userService;

    @Mock
    private UserApiKeyService userApiKeyService;

    @Mock
    private ProjectApiKeyService projectApiKeyService;

    @Mock
    private ProjectService projectService;

    @Mock
    private TeamMemberService teamMemberService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testPreHandle_ValidUserName() throws Exception {
        request.addHeader("X-User-Name", "testuser");
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(userService.getOne(any(QueryWrapper.class))).thenReturn(user);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(user, UserContext.getCurrentUser());
    }

    @Test
    void testPreHandle_ValidUserApiKey() throws Exception {
        request.addHeader("X-API-KEY", "valid-user-key");
        User user = new User();
        user.setId(1L);

        when(userApiKeyService.verifyKey("valid-user-key")).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(user);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(user, UserContext.getCurrentUser());
    }

    @Test
    void testPreHandle_ValidProjectApiKey() throws Exception {
        request.addHeader("X-API-KEY", "valid-project-key");
        ProjectApiKey projectApiKey = new ProjectApiKey();
        projectApiKey.setProjectId(100);
        projectApiKey.setUserId(1);

        User user = new User();
        user.setId(1L);
        Project project = new Project();
        project.setId(100);
        project.setTeamId(10);

        when(userApiKeyService.verifyKey("valid-project-key")).thenReturn(null);
        when(projectApiKeyService.verifyKey("valid-project-key")).thenReturn(projectApiKey);
        when(userService.getById(1L)).thenReturn(user);
        when(projectService.getById(100)).thenReturn(project);
        // Project access check logic needs to pass
        // If user is owner/creator (which is implied by verifyKey logic setting user), 
        // access check: teamMemberService.count(...) > 0
        when(teamMemberService.count(any(QueryWrapper.class))).thenReturn(1L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(user, UserContext.getCurrentUser());
        assertEquals(100, UserContext.getCurrentProjectId());
    }

    @Test
    void testPreHandle_ProjectApiKey_MismatchProjectId() throws Exception {
        request.addHeader("X-API-KEY", "valid-project-key");
        request.addHeader("X-Project-Id", "200"); // Requesting project 200, but key is for 100

        ProjectApiKey projectApiKey = new ProjectApiKey();
        projectApiKey.setProjectId(100);
        projectApiKey.setUserId(1);

        when(userApiKeyService.verifyKey("valid-project-key")).thenReturn(null);
        when(projectApiKeyService.verifyKey("valid-project-key")).thenReturn(projectApiKey);
        
        // User resolution happens
        User user = new User();
        user.setId(1L);
        when(userService.getById(1L)).thenReturn(user);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("API Key does not match requested Project ID", response.getErrorMessage());
    }

    @Test
    void testPreHandle_ProjectId_AccessDenied() throws Exception {
        request.addHeader("X-User-Name", "testuser");
        request.addHeader("X-Project-Id", "100");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole("member"); // Not admin

        Project project = new Project();
        project.setId(100);
        project.setTeamId(10);

        when(userService.getOne(any(QueryWrapper.class))).thenReturn(user);
        when(projectService.getById(100)).thenReturn(project);
        when(teamMemberService.count(any(QueryWrapper.class))).thenReturn(0L); // Not a member

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("You do not have access to this project", response.getErrorMessage());
    }
}
