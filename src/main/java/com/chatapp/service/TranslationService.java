package com.chatapp.service;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    @Value("${google.translate.api-key:}")
    private String apiKey;

    private Translate translateClient;
    private boolean useGoogleApi;

    private final NodeTranslationClient nodeClient;

    public TranslationService(NodeTranslationClient nodeClient) {
        this.nodeClient = nodeClient;
    }

    @PostConstruct
    void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                translateClient = TranslateOptions.newBuilder()
                        .setApiKey(apiKey)
                        .build()
                        .getService();
                useGoogleApi = true;
                log.info("Google Translate API initialized successfully");
            } catch (Exception e) {
                log.warn("Failed to initialize Cloud Google Translate Client: {}", e.getMessage());
                useGoogleApi = false;
            }
        } else {
            useGoogleApi = false;
            log.info("No Google Translate Cloud API key provided — relying entirely on local Node runtime client");
        }
    }

    public TranslationResult translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return new TranslationResult(text, text, sourceLang, targetLang);
        }

        String normalizedSource = normalizeLang(sourceLang);
        String normalizedTarget = normalizeLang(targetLang);

        if (normalizedSource.equals(normalizedTarget)) {
            return new TranslationResult(text, text, normalizedSource, normalizedTarget);
        }

        // 1. Run local Node script wrapper engine
        TranslationResult nodeResult = nodeClient.translate(text, normalizedSource, normalizedTarget);
        if (nodeResult != null && nodeResult.translatedText() != null) {
            return nodeResult;
        }

        // 2. Secondary fallback path: Execute remote query using cloud endpoint infrastructure
        if (useGoogleApi) {
            try {
                Translation translation = translateClient.translate(
                        text,
                        Translate.TranslateOption.sourceLanguage(normalizedSource),
                        Translate.TranslateOption.targetLanguage(normalizedTarget)
                );
                String detectedSource = translation.getSourceLanguage() != null
                        ? translation.getSourceLanguage()
                        : normalizedSource;
                return new TranslationResult(text, translation.getTranslatedText(), detectedSource, normalizedTarget);
            } catch (Exception e) {
                log.error("Google Cloud Translate API exception caught: {}", e.getMessage());
            }
        }

        // 3. Absolute Fallback: Output pristine source string to maintain seamless chat flow
        return new TranslationResult(text, text, normalizedSource, normalizedTarget);
    }

    private String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "en";
        }
        return lang.toLowerCase().split("-")[0];
    }

    public record TranslationResult(
            String originalText,
            String translatedText,
            String sourceLanguage,
            String targetLanguage
    ) {}
}