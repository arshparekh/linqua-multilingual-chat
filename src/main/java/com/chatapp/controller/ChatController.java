package com.chatapp.controller;

import com.chatapp.model.ChatMessage;
import com.chatapp.service.TranslationService;
import com.chatapp.service.TranslationService.TranslationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final TranslationService translationService;
    private final SimpMessagingTemplate messagingTemplate;

    

    // Private rooms: roomCode → (username → preferred language)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> roomUserLanguages = new ConcurrentHashMap<>();

    public ChatController(TranslationService translationService, SimpMessagingTemplate messagingTemplate) {
        this.translationService = translationService;
        this.messagingTemplate = messagingTemplate;
    }

    
    //  PRIVATE 1:1 ROOMS  — dynamic topic /topic/room/{code}

    @MessageMapping("/room.join")
    public void joinRoom(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String username = message.getSender();
        String language = message.getSenderLanguage() != null ? message.getSenderLanguage() : "en";
        String roomCode = message.getRoomCode();

        if (roomCode == null || roomCode.isBlank()) return;

        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("language", language);
        headerAccessor.getSessionAttributes().put("roomCode", roomCode);

        // Register this user's language preference under the room
        roomUserLanguages
                .computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                .put(username, language);

        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setType(ChatMessage.MessageType.JOIN);
        joinMessage.setSender(username);
        joinMessage.setSenderLanguage(language);
        joinMessage.setRoomCode(roomCode);
        joinMessage.setContent(username + " joined the room");

        messagingTemplate.convertAndSend("/topic/room/" + roomCode, joinMessage);
    }

    @MessageMapping("/room.send")
    public void sendRoomMessage(@Payload ChatMessage message) {
        String sourceLang = message.getSenderLanguage() != null ? message.getSenderLanguage() : "en";
        String originalContent = message.getContent();
        String roomCode = message.getRoomCode();

        if (roomCode == null || roomCode.isBlank()) return;

        // Build per-room translation map — only translate into languages used in THIS room
        Map<String, String> translationMap = new HashMap<>();
        translationMap.put(sourceLang, originalContent);

        ConcurrentHashMap<String, String> roomLangs = roomUserLanguages.getOrDefault(roomCode, new ConcurrentHashMap<>());
        roomLangs.values().stream().distinct().forEach(targetLang -> {
            if (!targetLang.equalsIgnoreCase(sourceLang)) {
                try {
                    TranslationResult result = translationService.translate(originalContent, sourceLang, targetLang);
                    if (result != null && result.translatedText() != null) {
                        translationMap.put(targetLang, result.translatedText());
                    }
                } catch (Exception e) {
                    log.error("Failed room translation to {}: {}", targetLang, e.getMessage());
                }
            }
        });

        ChatMessage outgoing = new ChatMessage();
        outgoing.setType(ChatMessage.MessageType.CHAT);
        outgoing.setSender(message.getSender());
        outgoing.setSenderLanguage(sourceLang);
        outgoing.setContent(originalContent);
        outgoing.setRoomCode(roomCode);
        outgoing.setTranslations(translationMap);

        messagingTemplate.convertAndSend("/topic/room/" + roomCode, outgoing);
    }
}