package com.chatapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class NodeTranslationClient {


    private static final Logger log = LoggerFactory.getLogger(NodeTranslationClient.class);

    @Value("${node.translation.enabled:true}")
    private boolean enabled;

    @Value("${node.translation.node-executable:node}")
    private String nodeExecutable;

    @Value("${node.translation.script:node/translate.js}")
    private String scriptPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void init() {
        log.info("Node translation: enabled={}, nodeExecutable='{}', scriptPath='{}'", enabled, nodeExecutable, scriptPath);
    }

    public TranslationService.TranslationResult translate(String text, String sourceLang, String targetLang) {
        if (!enabled) {
            return new TranslationService.TranslationResult(text, text, sourceLang, targetLang);
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(nodeExecutable);
            cmd.add(scriptPath);

            // Ensure node runs from project root so relative scriptPath like `node/translate.js` resolves.
            // (If this fails in your environment, we can make it configurable.)
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new java.io.File("."));
            pb.redirectErrorStream(false);

            Process p = pb.start();


            String inputJson = objectMapper.createObjectNode()
                    .put("text", text)
                    .put("sourceLang", sourceLang)
                    .put("targetLang", targetLang)
                    .toString();

            p.getOutputStream().write(inputJson.getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().flush();
            p.getOutputStream().close();

            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line);
                }
            }

            int exit = p.waitFor();
            if (exit != 0) {
                log.warn("Node translation exited with code {}. Output: {}", exit, output);
                return null;
            }

            JsonNode root = objectMapper.readTree(output.toString());
            String translatedText = root.path("translatedText").asText(text);
            String detectedSource = root.path("detectedSource").asText(sourceLang);

            return new TranslationService.TranslationResult(text, translatedText, detectedSource, targetLang);
        } catch (Exception e) {
            log.error("Node translation failed: {}", e.getMessage(), e);
            return null;
        }
    }
}

