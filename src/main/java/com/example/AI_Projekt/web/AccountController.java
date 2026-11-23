package com.example.AI_Projekt.web;

import com.example.AI_Projekt.service.RegistrationService;
import com.example.AI_Projekt.service.UsernameTakenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


import java.util.Objects;

/** Register, log in and the user's own page. Checking the password is done by Spring Security. */
@Controller
@RequiredArgsConstructor
public class AccountController {

    private final RegistrationService registrationService;

    @GetMapping("/login")
    public String loginPage() {
        return "account/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "account/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "The two passwords are not the same.");
        }
        if (!result.hasErrors()) {
            try {
                registrationService.register(form.getUsername(), form.getPassword());
            } catch (UsernameTakenException ex) {
                result.rejectValue("username", "taken", "That username is already taken.");
            }
        }
        if (result.hasErrors()) {
            return "account/register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/me")
    public String me(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "account/me";
    }
}
