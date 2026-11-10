package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.GazetteUploadForm;
import com.josh.ardhi.models.ClientKeywordMatchStatus;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientKeywordMatchService;
import com.josh.ardhi.services.ClientSubscriptionService;
import com.josh.ardhi.services.GazetteService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final GazetteService gazetteService;
    private final AppUserService appUserService;
    private final ClientSubscriptionService clientSubscriptionService;
    private final ClientKeywordMatchService clientKeywordMatchService;

    public AdminDashboardController(GazetteService gazetteService,
                                    AppUserService appUserService,
                                    ClientSubscriptionService clientSubscriptionService,
                                    ClientKeywordMatchService clientKeywordMatchService) {
        this.gazetteService = gazetteService;
        this.appUserService = appUserService;
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientKeywordMatchService = clientKeywordMatchService;
    }

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("uploadForm", new GazetteUploadForm());
        populateDashboardModel(model);
        return "admin/dashboard";
    }

    @HxRequest
    @PostMapping("/dashboard/matches/{id}/approve")
    public String quickApprove(@PathVariable UUID id, Model model) {
        try {
            clientKeywordMatchService.approveMatch(id);
            model.addAttribute("dashboardMatchSuccess", "Match approved.");
        } catch (AppException e) {
            model.addAttribute("dashboardMatchError", e.getMessage());
        }
        populateDashboardModel(model);
        return "admin/dashboard :: pendingMatchesSection";
    }

    @HxRequest
    @PostMapping("/dashboard/matches/{id}/reject")
    public String quickReject(@PathVariable UUID id, Model model) {
        try {
            clientKeywordMatchService.rejectMatch(id, "not_same_person", "Rejected from dashboard");
            model.addAttribute("dashboardMatchSuccess", "Match rejected.");
        } catch (AppException e) {
            model.addAttribute("dashboardMatchError", e.getMessage());
        }
        populateDashboardModel(model);
        return "admin/dashboard :: pendingMatchesSection";
    }

    private void populateDashboardModel(Model model) {
        model.addAttribute("activeClientsCount", appUserService.countActiveClients());
        model.addAttribute("activeSubscriptionsCount", clientSubscriptionService.countActiveValidSubscriptions());
        model.addAttribute("pendingMatchesCount",
                clientKeywordMatchService.countMatchesByStatus(ClientKeywordMatchStatus.PENDING_REVIEW));
        model.addAttribute("gazetteIssues", gazetteService.getRecentIssues());
        model.addAttribute("pendingMatches",
                clientKeywordMatchService.searchMatches("PENDING_REVIEW", null, null, null, null, 0, 10).getContent());
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "dashboard";
    }
}
