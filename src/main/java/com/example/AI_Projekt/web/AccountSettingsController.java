package com.example.AI_Projekt.web;

import com.example.AI_Projekt.rebrickable.RebrickableException;
import com.example.AI_Projekt.service.RebrickableLinkService;
import com.example.AI_Projekt.service.RebrickableLoginFailedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountSettingsController {
    private final RebrickableLinkService rebrickableLinkService;


    public AccountSettingsController(RebrickableLinkService rebrickableLinkService)
    {
        this.rebrickableLinkService = rebrickableLinkService;
    }

    @GetMapping("/account/settings")
    public String getAccountSettingsPage(Authentication authentication, Model model)
    {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("linked", rebrickableLinkService.isLinked(authentication.getName()));
        return "account/settings";
    }

    @PostMapping("/account/settings/link")
    public String linkAccount(Authentication authentication, @RequestParam String rebrickableUsername, @RequestParam String password, RedirectAttributes redirect)
    {
        try {
            rebrickableLinkService.linkAccount(authentication.getName(), rebrickableUsername, password);
            redirect.addFlashAttribute("success", "Rebrickable account linked.");
        }
        catch (RebrickableLoginFailedException e) {
            redirect.addFlashAttribute("error",
                    "Rebrickable didn't accept that username and password. Check them and try again.");
        }
        catch (RebrickableException e) {
            redirect.addFlashAttribute("error", "Rebrickable can't be reached right now. Try again in a few minutes.");
        }
        return "redirect:/account/settings";
    }

    @PostMapping("/account/settings/unlink")
    public String unlinkAccount(Authentication authentication, RedirectAttributes redirect)
    {
        rebrickableLinkService.unlinkAccount(authentication.getName());
        redirect.addFlashAttribute("success", "Rebrickable account unlinked.");
        return "redirect:/account/settings";
    }

}
