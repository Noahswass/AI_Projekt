package com.example.AI_Projekt.web.ai;

import com.example.AI_Projekt.service.ai.AssistantRateLimiter;
import com.example.AI_Projekt.service.ai.AssistantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class AssistantController {

    private static final int MAX_QUESTION_LENGTH = 500;

    private final AssistantService assistantService;
    private final AssistantRateLimiter rateLimiter;

    @GetMapping("/assistant")
    public String form() {
        return "assistant/ask";
    }
    @PostMapping("/assistant")
    public String ask(@RequestParam(required = false) String question, Model model, Principal principal, HttpServletRequest request) {
        String cleaned = clean(question);
        model.addAttribute("question", cleaned);           // keep text in all paths
        if (cleaned == null || cleaned.isBlank()) {
            model.addAttribute("error", "Please enter a question before asking.");
            return "assistant/ask";                        // nothing asked, no quota used
        }

        boolean loggedIn = principal != null;
        if (!rateLimiter.tryConsume(visitorKey(principal, request), loggedIn)) {
            model.addAttribute("error", loggedIn
                    ? "You've hit the question limit, try again in a bit."
                    : "This is a demo for non-logged-in users, log in to ask more questions ;)");
            return "assistant/ask";
        }
        try {
            AssistantService.AssistantAnswer result = assistantService.ask(cleaned);
            model.addAttribute("question", cleaned);           // text box keeps what was typed
            model.addAttribute("answer", result.answer());
            model.addAttribute("sources", result.sources());
        } catch (Exception e) {
            model.addAttribute("error", "Something went wrong, try again in a bit.");
            return "assistant/ask";
        }
        return "assistant/ask";
    }

    private String clean(String text) {
        if (text == null || text.isBlank()) {
            return null;                                   // service returns its fixed blank answer
        }
        String trimmed = text.trim();
        return trimmed.length() > MAX_QUESTION_LENGTH ? trimmed.substring(0, MAX_QUESTION_LENGTH) : trimmed;
    }
    static String visitorKey(Principal principal, HttpServletRequest request){
        return principal == null ? "ip:" +request.getRemoteAddr() : "user:" + principal.getName();
    }
}

