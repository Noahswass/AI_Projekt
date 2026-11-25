package com.example.AI_Projekt.web.catalog;

import com.example.AI_Projekt.service.catalog.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LandingPageController {

    private final CatalogService catalogService;

    @GetMapping("/")
    public String showLandingPage(Principal principal, Model model) {
        model.addAttribute("themes", catalogService.themeCards());
        // null for a visitor; the page then shows Login and Register instead of the name
        model.addAttribute("username", principal == null ? null : principal.getName());
        return "navigation/landingPage";
    }
}
