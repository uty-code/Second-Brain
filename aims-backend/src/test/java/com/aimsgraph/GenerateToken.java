package com.aimsgraph;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.nio.charset.StandardCharsets;

public class GenerateToken {
    public static void main(String[] args) {
        String secret = "your-super-secret-key-for-aims-graph-backend-dev-only";
        String token = Jwts.builder()
                .claim("workspace_id", "test-workspace-1")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 864000000L))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        System.out.println("\n=== [TEST JWT TOKEN] ===");
        System.out.println("Bearer " + token);
        System.out.println("========================\n");
    }
}
