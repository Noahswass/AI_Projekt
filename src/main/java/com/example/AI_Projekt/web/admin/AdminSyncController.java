package com.example.AI_Projekt.web.admin;

import com.example.AI_Projekt.service.sync.SyncRunService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** PB-07: the admin sees recent sync runs and can start one by hand. ADMIN only, see SecurityConfig. */
@Controller
public class AdminSyncController {

    private final SyncRunService syncRunService;
    private final TaskExecutor taskExecutor;

    // Spring Boot's own pool for background work. Named, because the scheduler is a TaskExecutor too.
    public AdminSyncController(SyncRunService syncRunService,
                               @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.syncRunService = syncRunService;
        this.taskExecutor = taskExecutor;
    }

    @GetMapping("/admin/sync")
    public String syncPage(Model model) {
        model.addAttribute("running", syncRunService.isRunning());
        model.addAttribute("runs", syncRunService.recentRuns());
        return "admin/sync";
    }

    @PostMapping("/admin/sync")
    public String startSync(RedirectAttributes redirect) {
        if (syncRunService.startInBackground(taskExecutor)) {
            redirect.addFlashAttribute("message", "Sync started. Reload this page to follow it.");
        } else {
            redirect.addFlashAttribute("warning", "A sync is already running. Wait for it to finish.");
        }
        // Redirect after POST, so reloading the page does not start another run
        return "redirect:/admin/sync";
    }
}
