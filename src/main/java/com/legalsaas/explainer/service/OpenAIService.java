package com.legalsaas.explainer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenAIService {



    @Value("${openai.api.key}")
    private String OPENAI_API_KEY;

    public String extractTextFromFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new Exception("Invalid file");

        if (filename.endsWith(".pdf")) {
            return extractTextFromPDF(file);
        } else if (filename.endsWith(".docx")) {
            return extractTextFromDocx(file);
        } else {
            throw new Exception("Unsupported file type. Use PDF or DOCX.");
        }
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            document.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    public String explainText(String inputText) throws Exception {
        Logger logger = Logger.getLogger(OpenAIService.class.getName());
        String prompt = "Explain the following legal document in plain, simple English. " +
                "Make it easy for a non-lawyer to understand. Highlight any risks or obligations.\n\n" + inputText;

        HttpClient client = createHttpClientWithDisabledSSL();
        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        ObjectMapper mapper = new ObjectMapper();
        String requestBody = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        String responseBody = response.body();

        logger.info("HTTP Status Code: " + statusCode);
        logger.info("Response Body: " + responseBody);


        if (statusCode != 200) {
            throw new RuntimeException("Failed to get response from OpenAI: " + statusCode + " - " + responseBody);
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("Empty response from the server");
        }

        Map<?, ?> responseMap = mapper.readValue(responseBody, Map.class);

        List<?> choices = (List<?>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("No response from GPT");

        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        return (String) message.get("content");
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