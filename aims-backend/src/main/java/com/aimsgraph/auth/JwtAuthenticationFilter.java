package com.aimsgraph.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getParameter("token") != null) {
            token = request.getParameter("token");
        }

        if (token != null) {
            if ("MVP_DUMMY_TOKEN".equals(token)) {
                String headerWorkspaceId = request.getHeader("X-Workspace-ID");
                if (headerWorkspaceId == null || headerWorkspaceId.isEmpty()) {
                    headerWorkspaceId = request.getParameter("workspaceId");
                }
                String workspaceId = (headerWorkspaceId != null && !headerWorkspaceId.isEmpty()) ? headerWorkspaceId : "default-workspace";
                
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "dummy_user", null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                if (RequestContextHolder.getRequestAttributes() != null) {
                    RequestContextHolder.getRequestAttributes().setAttribute(
                        JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, workspaceId, RequestAttributes.SCOPE_REQUEST);
                }
            } else if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                String tokenWorkspaceId = jwtUtil.getWorkspaceIdFromToken(token);
                String headerWorkspaceId = request.getHeader("X-Workspace-ID");
                if (headerWorkspaceId == null || headerWorkspaceId.isEmpty()) {
                    headerWorkspaceId = request.getParameter("workspaceId");
                }
                
                String workspaceId = (headerWorkspaceId != null && !headerWorkspaceId.isEmpty()) ? headerWorkspaceId : tokenWorkspaceId;

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                if (RequestContextHolder.getRequestAttributes() != null) {
                    RequestContextHolder.getRequestAttributes().setAttribute(
                        JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, workspaceId, RequestAttributes.SCOPE_REQUEST);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
