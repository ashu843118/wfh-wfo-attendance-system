package com.wfhwfo.attendance.auth.controller;

import com.wfhwfo.attendance.auth.dto.LoginRequest;
import com.wfhwfo.attendance.auth.dto.LoginResponse;
import com.wfhwfo.attendance.auth.service.AuthService;
import com.wfhwfo.attendance.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthController authController;

    @Test
    void rejectsPasswordInQueryString() {
        when(httpRequest.getParameter("password")).thenReturn("secret");

        assertThatThrownBy(() -> authController.login(
                new LoginRequest("employee@demo.com", "password"), httpRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Login credentials must be provided in the request body.");

        verify(authService, never()).login(any(), any());
    }

    @Test
    void acceptsCredentialsInRequestBodyOnly() {
        when(httpRequest.getParameter("password")).thenReturn(null);
        when(httpRequest.getParameter("email")).thenReturn(null);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.login(any(LoginRequest.class), eq("127.0.0.1")))
                .thenReturn(LoginResponse.builder().token("jwt").build());

        authController.login(new LoginRequest("employee@demo.com", "password"), httpRequest);

        verify(authService).login(any(LoginRequest.class), eq("127.0.0.1"));
    }
}
