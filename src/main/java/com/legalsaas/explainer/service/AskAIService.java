package com.legalsaas.explainer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AskAIService {

    private final HttpClient httpClient;
    private final String openaiApiKey;
    private final String openaiApiUrl;
    private final ObjectMapper objectMapper;

    public AskAIService(
            @Value("${openai.api.key}") String openaiApiKey,
            @Value("${openai.api.url}") String openaiApiUrl) throws Exception {
        this.openaiApiKey = openaiApiKey;
        this.openaiApiUrl = openaiApiUrl;
        this.httpClient = createHttpClientWithDisabledSSL();
        this.objectMapper = new ObjectMapper();
    }

    public String askOpenAI(String userQuestion) {
        try {
            // Build the request body as a Map
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a helpful legal assistant. Please explain legal concepts in simple terms."),
                    Map.of("role", "user", "content", userQuestion)
            ));

            // Convert the request body to JSON
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openaiApiUrl))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson, StandardCharsets.UTF_8))
                    .build();

            // Send the request and handle the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get response from OpenAI: " + response.statusCode() + " - " + response.body());
            }

            // Parse the response
            String responseBody = response.body();
            int start = responseBody.indexOf("\"content\":\"") + 11;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end).replace("\\n", "\n");

        } catch (Exception e) {
            return "Error contacting OpenAI: " + e.getMessage();
        }
    }

    private HttpClient createHttpClientWithDisabledSSL() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        return HttpClient.newBuilder()
                .sslContext(sc)
                .build();
    }
}