package com.legalsaas.explainer.controller;

import com.legalsaas.explainer.service.AskAIService;
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

    @Autowired
    private AskAIService askAIService;

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

    @PostMapping("/ask")
    public String askAI(@RequestParam("query") String query, Model model) {
        try {
            String prompt = "You are a legal assistant. Answer the following question clearly:\n" + query;

            String aiAnswer = askAIService.askOpenAI(prompt); // use your existing service
            model.addAttribute("aiAnswer", aiAnswer);

        } catch (Exception e) {
            model.addAttribute("aiAnswer", "Error while contacting AI: " + e.getMessage());
        }

        return "upload"; // or whatever your template name is
    }

}