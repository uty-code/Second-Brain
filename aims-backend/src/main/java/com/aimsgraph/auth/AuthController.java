package com.aimsgraph.auth;

import com.aimsgraph.domain.user.Users;
import com.aimsgraph.domain.user.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final org.redisson.api.RedissonClient redissonClient;
    private final org.springframework.data.neo4j.core.Neo4jClient neo4jClient;

    @org.springframework.beans.factory.annotation.Value("${wiki.base.dir:workspaces}")
    private String wikiBaseDir;

    public AuthController(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, org.redisson.api.RedissonClient redissonClient, org.springframework.data.neo4j.core.Neo4jClient neo4jClient) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redissonClient = redissonClient;
        this.neo4jClient = neo4jClient;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
        }

        if (userMapper.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        Users user = new Users();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        String uniqueWsId = "ws-" + username.trim().toLowerCase().replaceAll("[^a-z0-9\\-]", "");
        user.setDefaultWorkspaceId(uniqueWsId);

        userMapper.insertUser(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Users user = userMapper.findByUsername(username);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        String workspaceId = user.getDefaultWorkspaceId();
        String token = jwtUtil.generateToken(username, workspaceId);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "workspaceId", workspaceId,
                "username", username
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                java.util.Date expiration = jwtUtil.getExpirationFromToken(token);
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    org.redisson.api.RBucket<String> bucket = redissonClient.getBucket("blacklist:" + token);
                    bucket.set("logout", ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser") || username.equals("dummy_user")) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        try {
            // 1. Delete all Neo4j nodes across all workspaces owned by this user
            neo4jClient.query("MATCH (n) WHERE n.workspaceId = 'ws-' + $username OR n.workspaceId STARTS WITH $username + '_' DETACH DELETE n")
                    .bind(username).to("username")
                    .fetch().all();

            // 2. Delete all physical workspace directories owned by the user
            java.nio.file.Path path = java.nio.file.Paths.get(wikiBaseDir);
            if (java.nio.file.Files.exists(path) && java.nio.file.Files.isDirectory(path)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(path)) {
                    java.util.List<java.nio.file.Path> dirsToDelete = stream
                            .filter(java.nio.file.Files::isDirectory)
                            .filter(p -> {
                                String name = p.getFileName().toString();
                                return name.equals("ws-" + username) || name.startsWith(username + "_");
                            })
                            .collect(java.util.stream.Collectors.toList());

                    for (java.nio.file.Path dir : dirsToDelete) {
                        java.util.List<java.nio.file.Path> files = java.nio.file.Files.walk(dir)
                                .sorted(java.util.Comparator.reverseOrder())
                                .collect(java.util.stream.Collectors.toList());
                        for (java.nio.file.Path f : files) {
                            java.nio.file.Files.deleteIfExists(f);
                        }
                    }
                }
            }

            // 3. Delete DB record
            userMapper.deleteByUsername(username);

            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to delete account: " + e.getMessage()));
        }
    }
}
