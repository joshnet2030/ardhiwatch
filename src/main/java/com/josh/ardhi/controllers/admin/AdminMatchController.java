package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.ClientKeywordMatch;
import com.josh.ardhi.models.ClientKeywordMatchStatus;
import com.josh.ardhi.models.ClientNotification;
import com.josh.ardhi.models.ClientSubscriptionPlan;
import com.josh.ardhi.models.KeywordType;
import com.josh.ardhi.models.NotificationChannel;
import com.josh.ardhi.models.NotificationStatus;
import com.josh.ardhi.services.ClientKeywordMatchService;
import com.josh.ardhi.services.ClientSubscriptionService;
import com.josh.ardhi.services.GazetteService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/matches")
public class AdminMatchController {

    private static final int MATCHES_PAGE_SIZE = 10;

    private final ClientKeywordMatchService clientKeywordMatchService;
    private final GazetteService gazetteService;
    private final ClientSubscriptionService clientSubscriptionService;

    public AdminMatchController(ClientKeywordMatchService clientKeywordMatchService,
                                GazetteService gazetteService,
                                ClientSubscriptionService clientSubscriptionService) {
        this.clientKeywordMatchService = clientKeywordMatchService;
        this.gazetteService = gazetteService;
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @GetMapping("")
    public String list(@RequestParam(name = "status", defaultValue = "PENDING_REVIEW") String status,
                       @RequestParam(name = "issue", required = false) UUID issueId,
                       @RequestParam(name = "client", defaultValue = "") String clientQuery,
                       @RequestParam(name = "keywordType", defaultValue = "") String keywordType,
                       @RequestParam(name = "matchMode", defaultValue = "") String matchMode,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<ClientKeywordMatch> matchesPage = clientKeywordMatchService.searchMatches(
                status, issueId, clientQuery, keywordType, matchMode, page, MATCHES_PAGE_SIZE);

        model.addAttribute("matchesPage", matchesPage);
        model.addAttribute("matches", matchesPage.getContent());
        model.addAttribute("selectedStatus", status != null ? status.trim().toUpperCase() : "ALL");
        model.addAttribute("selectedIssueId", issueId);
        model.addAttribute("clientQuery", clientQuery != null ? clientQuery.trim() : "");
        model.addAttribute("selectedKeywordType", keywordType != null ? keywordType.trim().toUpperCase() : "");
        model.addAttribute("selectedMatchMode", matchMode != null ? matchMode.trim().toUpperCase() : "");
        model.addAttribute("keywordTypes", KeywordType.values());
        model.addAttribute("gazetteIssues", gazetteService.getAllActiveIssues());

        model.addAttribute("pendingCount", clientKeywordMatchService.countMatchesByStatus(ClientKeywordMatchStatus.PENDING_REVIEW));
        model.addAttribute("approvedCount", clientKeywordMatchService.countMatchesByStatus(ClientKeywordMatchStatus.APPROVED));
        model.addAttribute("rejectedCount", clientKeywordMatchService.countMatchesByStatus(ClientKeywordMatchStatus.REJECTED));
        model.addAttribute("allCount", clientKeywordMatchService.countAllMatches());

        return "admin/matches/list";
    }

    @PostMapping("/bulk-review")
    public String bulkReview(@RequestParam(name = "ids", required = false) List<UUID> ids,
                             @RequestParam(name = "action", defaultValue = "approve") String action,
                             @RequestParam(name = "status", defaultValue = "PENDING_REVIEW") String status,
                             RedirectAttributes redirectAttributes) {
        try {
            clientKeywordMatchService.bulkReviewMatches(ids, action);
            redirectAttributes.addFlashAttribute("reviewSuccess",
                    "Selected matches have been " + ("approve".equalsIgnoreCase(action) ? "approved." : "rejected."));
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
        }
        return "redirect:/admin/matches?status=" + status;
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable UUID id,
                           @RequestParam(name = "view", defaultValue = "highlighted") String view,
                           Model model) {
        loadMatchDetails(id, model);
        model.addAttribute("pdfView", "original".equalsIgnoreCase(view) ? "original" : "highlighted");
        model.addAttribute("currentTab", "overview");
        return "admin/matches/tabs/overview";
    }

    @PostMapping("/show/{id}/approve")
    public String approveMatch(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            clientKeywordMatchService.approveMatch(id);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Match approved.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
        }
        return "redirect:/admin/matches/show/" + id;
    }

    @PostMapping("/show/{id}/reject")
    public String rejectMatch(@PathVariable UUID id,
                              @RequestParam(name = "reason", defaultValue = "not_same_person") String reason,
                              @RequestParam(name = "memo", required = false) String memo,
                              RedirectAttributes redirectAttributes) {
        try {
            clientKeywordMatchService.rejectMatch(id, reason, memo);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Match rejected.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
        }
        return "redirect:/admin/matches/show/" + id;
    }

    @GetMapping("/show/{id}/notifications")
    public String notifications(@PathVariable UUID id, Model model) {
        loadMatchDetails(id, model);
        model.addAttribute("currentTab", "notifications");
        return "admin/matches/tabs/notifications";
    }

    @PostMapping("/show/{id}/notifications/{nid}/resend")
    public String resendMatchNotification(@PathVariable UUID id,
                                          @PathVariable UUID nid,
                                          RedirectAttributes redirectAttributes) {
        try {
            clientKeywordMatchService.resendNotification(nid);
            redirectAttributes.addFlashAttribute("notificationSuccess", "Notification queued for re-sending.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("notificationError", e.getMessage());
        }
        return "redirect:/admin/matches/show/" + id + "/notifications";
    }

    private void loadMatchDetails(UUID id, Model model) {
        try {
            ClientKeywordMatch match = clientKeywordMatchService.getMatchById(id);
            List<ClientNotification> notifications = clientKeywordMatchService.getNotificationsForMatch(id);
            ClientSubscriptionPlan currentSubscription = clientSubscriptionService
                    .getCurrentSubscriptionForClient(match.getClient().getId()).orElse(null);

            long emailCount = notifications.stream().filter(n -> n.getChannel() == NotificationChannel.EMAIL).count();
            long smsCount = notifications.stream().filter(n -> n.getChannel() == NotificationChannel.SMS).count();
            long deliveredCount = notifications.stream().filter(n -> n.getStatus() == NotificationStatus.DELIVERED).count();
            long failedCount = notifications.stream().filter(n -> n.getStatus() == NotificationStatus.FAILED).count();
            long pendingCount = notifications.stream().filter(n -> n.getStatus() == NotificationStatus.PENDING).count();

            ClientNotification emailNotification = notifications.stream()
                    .filter(n -> n.getChannel() == NotificationChannel.EMAIL)
                    .findFirst().orElse(null);
            ClientNotification smsNotification = notifications.stream()
                    .filter(n -> n.getChannel() == NotificationChannel.SMS)
                    .findFirst().orElse(null);

            model.addAttribute("match", match);
            model.addAttribute("notifications", notifications);
            model.addAttribute("notificationsCount", notifications.size());
            model.addAttribute("emailCount", emailCount);
            model.addAttribute("smsCount", smsCount);
            model.addAttribute("deliveredCount", deliveredCount);
            model.addAttribute("failedCount", failedCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("emailNotification", emailNotification);
            model.addAttribute("smsNotification", smsNotification);
            model.addAttribute("currentSubscription", currentSubscription);
            model.addAttribute("prevMatch", clientKeywordMatchService.findPreviousPendingMatch(match).orElse(null));
            model.addAttribute("nextMatch", clientKeywordMatchService.findNextPendingMatch(match).orElse(null));
            model.addAttribute("otherClientMatches",
                    clientKeywordMatchService.findOtherMatchesForClient(match.getClient().getId(), match.getId()));
        } catch (AppException e) {
            model.addAttribute("matchError", e.getMessage());
        }
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "matches";
    }
}
