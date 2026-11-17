package com.example.AI_Projekt.web;

import com.example.AI_Projekt.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LandingPageController {

    private final CatalogService catalogService;

    @GetMapping("/")
    public String showLandingPage(Model model) {
        model.addAttribute("themes", catalogService.themeCards());
        return "navigation/landingPage";
    }
}
