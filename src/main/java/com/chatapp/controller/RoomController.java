package com.chatapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    // Stores active room codes → creation timestamp
    private final ConcurrentHashMap<String, Long> rooms = new ConcurrentHashMap<>();

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    /**
     * POST /api/rooms
     * Generates a unique 6-character alphanumeric room code and stores it.
     * Returns: { "code": "AB1234" }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createRoom() {
        String code;
        // Ensure uniqueness
        do {
            code = generateCode();
        } while (rooms.containsKey(code));

        rooms.put(code, System.currentTimeMillis());
        return ResponseEntity.ok(Map.of("code", code));
    }

    /**
     * GET /api/rooms/{code}
     * Returns 200 if the room code is valid, 404 otherwise.
     * Used by the join flow to validate before connecting via WebSocket.
     */
    @GetMapping("/{code}")
    public ResponseEntity<Map<String, String>> validateRoom(@PathVariable String code) {
        if (rooms.containsKey(code.toUpperCase())) {
            return ResponseEntity.ok(Map.of("code", code.toUpperCase()));
        }
        return ResponseEntity.notFound().build();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
