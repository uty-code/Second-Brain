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

    public AuthController(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
        user.setDefaultWorkspaceId("default-workspace");

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
}
