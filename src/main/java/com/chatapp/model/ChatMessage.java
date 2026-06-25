package com.chatapp.model;

import java.util.Map;

public class ChatMessage {

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    private MessageType type;
    private String sender;
    private String senderLanguage;
    private String content;
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private long timestamp;
    private String roomCode;
    
    // Dictionary tracking translations: key = language code, value = translated text
    private Map<String, String> translations;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderLanguage() {
        return senderLanguage;
    }

    public void setSenderLanguage(String senderLanguage) {
        this.senderLanguage = senderLanguage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}