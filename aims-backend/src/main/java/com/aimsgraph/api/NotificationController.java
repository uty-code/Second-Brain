package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/sse")
    public SseEmitter subscribe() {
        String workspaceId = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (workspaceId == null || workspaceId.isEmpty()) {
            throw new IllegalArgumentException("Workspace ID missing");
        }

        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1 hour timeout
        emitters.put(workspaceId, emitter);

        emitter.onCompletion(() -> emitters.remove(workspaceId));
        emitter.onTimeout(() -> emitters.remove(workspaceId));
        emitter.onError((e) -> emitters.remove(workspaceId));

        try {
            emitter.send(SseEmitter.event().name("connected").data("Successfully subscribed to notifications."));
        } catch (Exception e) {
            emitters.remove(workspaceId);
        }

        return emitter;
    }

    public static void broadcastNotification(String workspaceId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(workspaceId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                emitters.remove(workspaceId);
            }
        }
    }
}
