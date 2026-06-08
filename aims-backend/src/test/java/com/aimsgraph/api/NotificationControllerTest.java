package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationControllerTest {

    @Test
    void testSubscribeWithWorkspaceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, "tenant1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        NotificationController controller = new NotificationController();
        SseEmitter emitter = controller.subscribe();
        assertNotNull(emitter);
    }

    @Test
    void testSubscribeWithoutWorkspaceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        NotificationController controller = new NotificationController();
        assertThrows(IllegalArgumentException.class, controller::subscribe);
    }
}
