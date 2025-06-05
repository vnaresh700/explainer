package com.legalsaas.explainer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.legalsaas.explainer.service.OpenAIService;

@Controller
public class UploadController {

    @Autowired
    private OpenAIService openAIService;

    @GetMapping("/")
    public String showForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            String extractedText = openAIService.extractTextFromFile(file);
            String explanation = openAIService.explainText(extractedText);
            model.addAttribute("explanation", explanation);
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        return "upload";
    }
}