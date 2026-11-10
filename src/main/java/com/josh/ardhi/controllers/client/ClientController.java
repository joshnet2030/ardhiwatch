package com.josh.ardhi.controllers.client;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.ClientKeywordForm;
import com.josh.ardhi.models.AppSession;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.ClientKeywordMatch;
import com.josh.ardhi.models.ClientKeywordMatchStatus;
import com.josh.ardhi.models.ClientSubscriptionPlan;
import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.KeywordType;
import com.josh.ardhi.repositories.AppSessionRepository;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientKeywordMatchService;
import com.josh.ardhi.services.ClientKeywordService;
import com.josh.ardhi.services.ClientSubscriptionService;
import com.josh.ardhi.services.GazetteService;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/client")
public class ClientController {

    private final AppSessionRepository appSessionRepository;
    private final AppUserService appUserService;
    private final ClientKeywordService clientKeywordService;
    private final ClientSubscriptionService clientSubscriptionService;
    private final ClientKeywordMatchService clientKeywordMatchService;
    private final GazetteService gazetteService;

    public ClientController(AppSessionRepository appSessionRepository,
                            AppUserService appUserService,
                            ClientKeywordService clientKeywordService,
                            ClientSubscriptionService clientSubscriptionService,
                            ClientKeywordMatchService clientKeywordMatchService,
                            GazetteService gazetteService) {
        this.appSessionRepository = appSessionRepository;
        this.appUserService = appUserService;
        this.clientKeywordService = clientKeywordService;
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientKeywordMatchService = clientKeywordMatchService;
        this.gazetteService = gazetteService;
    }

    @ModelAttribute
    public void populateCommonClientAttributes(Model model) {
        AppUser client = resolveCurrentClient();
        model.addAttribute("currentClient", client);

        List<GazetteIssue> recentIssues = gazetteService.getRecentIssues();
        GazetteIssue latestIssue = recentIssues.isEmpty() ? null : recentIssues.get(0);
        model.addAttribute("latestGazetteIssue", latestIssue);

        if (client != null) {
            ClientSubscriptionPlan currentSub = clientSubscriptionService
                    .getCurrentSubscriptionForClient(client.getId()).orElse(null);
            long activeKwCount = clientKeywordService.countActiveKeywordsForClient(client.getId());
            long totalKwCount = clientKeywordService.countTotalKeywordsForClient(client.getId());
            int keywordLimit = (currentSub != null && currentSub.getSubscriptionPlan() != null
                    && currentSub.getSubscriptionPlan().getKeywordLimit() != null)
                    ? currentSub.getSubscriptionPlan().getKeywordLimit()
                    : 5;
            long verifiedMatchesCount = clientKeywordMatchService
                    .countClientMatches(client.getId());

            int quotaPercent = keywordLimit > 0
                    ? (int) Math.min(100, Math.round((activeKwCount * 100.0) / keywordLimit))
                    : 0;

            model.addAttribute("currentSubscription", currentSub);
            model.addAttribute("activeKeywordsCount", activeKwCount);
            model.addAttribute("totalKeywordsCount", totalKwCount);
            model.addAttribute("pausedKeywordsCount", Math.max(0L, totalKwCount - activeKwCount));
            model.addAttribute("keywordLimit", keywordLimit);
            model.addAttribute("slotsAvailable", Math.max(0L, keywordLimit - activeKwCount));
            model.addAttribute("quotaPercent", quotaPercent);
            model.addAttribute("verifiedMatchesCount", verifiedMatchesCount);
        }
    }

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("currentMenu", "dashboard");
        AppUser client = (AppUser) model.getAttribute("currentClient");
        if (client != null) {
            List<ClientKeyword> keywords = clientKeywordService.getKeywordsForClient(client.getId());
            Page<ClientKeywordMatch> matchesPage = clientKeywordMatchService
                    .searchClientMatches(client.getId(), null, null, 0, 5);
            Map<UUID, Long> keywordMatchCounts = buildKeywordMatchCounts(client.getId(), keywords);

            model.addAttribute("keywords", keywords);
            model.addAttribute("recentMatches", matchesPage.getContent());
            model.addAttribute("latestMatch", matchesPage.hasContent() ? matchesPage.getContent().get(0) : null);
            model.addAttribute("keywordMatchCounts", keywordMatchCounts);
        } else {
            model.addAttribute("keywords", Collections.emptyList());
            model.addAttribute("recentMatches", Collections.emptyList());
        }
        return "client/dashboard";
    }

    @GetMapping("/keywords")
    public String keywords(@RequestParam(name = "editId", required = false) UUID editId,
                           Model model) {
        model.addAttribute("currentMenu", "keywords");
        AppUser client = (AppUser) model.getAttribute("currentClient");
        if (client != null) {
            List<ClientKeyword> keywords = clientKeywordService.getKeywordsForClient(client.getId());
            Map<UUID, Long> keywordMatchCounts = buildKeywordMatchCounts(client.getId(), keywords);
            model.addAttribute("keywords", keywords);
            model.addAttribute("keywordMatchCounts", keywordMatchCounts);

            if (editId != null) {
                try {
                    ClientKeyword editKw = clientKeywordService.getById(editId);
                    if (editKw.getClient().getId().equals(client.getId())) {
                        model.addAttribute("editKeyword", editKw);
                    }
                } catch (AppException ignored) {
                }
            }
        } else {
            model.addAttribute("keywords", Collections.emptyList());
        }
        return "client/keywords";
    }

    @PostMapping("/keywords")
    public String addKeyword(@RequestParam(name = "type", defaultValue = "TITLE") String type,
                             @RequestParam(name = "keyword", defaultValue = "") String keyword,
                             @RequestParam(name = "notes", required = false) String notes,
                             RedirectAttributes redirectAttributes) {
        AppUser client = resolveCurrentClient();
        if (client == null) {
            redirectAttributes.addFlashAttribute("keywordError", "Unable to identify your client account. Please sign in again.");
            return "redirect:/client/keywords";
        }

        if (!StringUtils.hasText(keyword)) {
            redirectAttributes.addFlashAttribute("keywordError", "Please enter a keyword to watch.");
            return "redirect:/client/keywords";
        }

        KeywordType parsedType = KeywordType.TITLE;
        try {
            parsedType = KeywordType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        ClientKeywordForm form = new ClientKeywordForm();
        form.setKeyword(keyword.trim());
        form.setKeywordType(parsedType);
        form.setMatchPrecision(parsedType.getDefaultPrecision());
        form.setNotes(StringUtils.hasText(notes) ? notes.trim() : null);

        try {
            clientKeywordService.createKeyword(client.getId(), form);
            redirectAttributes.addFlashAttribute("keywordSuccess",
                    "Keyword \"" + keyword.trim() + "\" added and is now being watched.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("keywordError", e.getMessage());
        }

        return "redirect:/client/keywords";
    }

    @PostMapping("/keywords/{id}/edit")
    public String updateKeyword(@PathVariable UUID id,
                                @RequestParam(name = "type", defaultValue = "TITLE") String type,
                                @RequestParam(name = "keyword", defaultValue = "") String keyword,
                                @RequestParam(name = "notes", required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        AppUser client = resolveCurrentClient();
        if (client == null) {
            return "redirect:/client/keywords";
        }

        KeywordType parsedType = KeywordType.TITLE;
        try {
            parsedType = KeywordType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        ClientKeywordForm form = new ClientKeywordForm();
        form.setId(id);
        form.setKeyword(keyword != null ? keyword.trim() : "");
        form.setKeywordType(parsedType);
        form.setMatchPrecision(parsedType.getDefaultPrecision());
        form.setNotes(StringUtils.hasText(notes) ? notes.trim() : null);

        try {
            clientKeywordService.updateKeyword(client.getId(), id, form);
            redirectAttributes.addFlashAttribute("keywordSuccess", "Keyword updated successfully.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("keywordError", e.getMessage());
        }
        return "redirect:/client/keywords";
    }

    @PostMapping("/keywords/{id}/toggle-pause")
    public String togglePauseKeyword(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        AppUser client = resolveCurrentClient();
        if (client == null) {
            return "redirect:/client/keywords";
        }
        try {
            ClientKeyword updated = clientKeywordService.togglePauseKeyword(client.getId(), id);
            redirectAttributes.addFlashAttribute("keywordSuccess",
                    "Keyword \"" + updated.getKeyword() + "\" " + (updated.isPaused() ? "paused." : "resumed."));
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("keywordError", e.getMessage());
        }
        return "redirect:/client/keywords";
    }

    @PostMapping("/keywords/{id}/delete")
    public String deleteKeyword(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        AppUser client = resolveCurrentClient();
        if (client == null) {
            return "redirect:/client/keywords";
        }
        try {
            clientKeywordService.deleteKeyword(client.getId(), id);
            redirectAttributes.addFlashAttribute("keywordSuccess", "Keyword deleted.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("keywordError", e.getMessage());
        }
        return "redirect:/client/keywords";
    }

    @GetMapping("/matches")
    public String matches(Model model) {
        model.addAttribute("currentMenu", "matches");
        AppUser client = (AppUser) model.getAttribute("currentClient");
        if (client != null) {
            Page<ClientKeywordMatch> matchesPage = clientKeywordMatchService
                    .searchClientMatches(client.getId(), null, null, 0, 20);
            model.addAttribute("matches", matchesPage.getContent());
        }
        return "client/matches/list";
    }

    @GetMapping("/matches/show/{id}")
    public String showMatch(@PathVariable String id, Model model) {
        model.addAttribute("currentMenu", "matches");
        model.addAttribute("matchId", id);
        try {
            UUID matchUuid = UUID.fromString(id);
            model.addAttribute("match", clientKeywordMatchService.getMatchById(matchUuid));
        } catch (IllegalArgumentException | AppException ignored) {
        }
        return "client/matches/show";
    }

    @GetMapping("/subscription")
    public String subscription(Model model) {
        model.addAttribute("currentMenu", "subscription");
        return "client/subscription";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("currentMenu", "settings");
        return "client/settings";
    }

    private AppUser resolveCurrentClient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            try {
                UUID sessionId = UUID.fromString(auth.getName());
                Optional<AppSession> sessionOpt = appSessionRepository.findById(sessionId);
                if (sessionOpt.isPresent() && sessionOpt.get().getUser() != null) {
                    return sessionOpt.get().getUser();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return appUserService.getAllUsers().stream()
                .filter(u -> u.getRole() == AppUserRole.CLIENT && u.isActive())
                .findFirst()
                .orElse(null);
    }

    private Map<UUID, Long> buildKeywordMatchCounts(UUID clientId, List<ClientKeyword> keywords) {
        Map<UUID, Long> counts = new HashMap<>();
        for (ClientKeyword kw : keywords) {
            long c = clientKeywordMatchService
                    .searchClientMatches(clientId, null, kw.getId(), 0, 1)
                    .getTotalElements();
            counts.put(kw.getId(), c);
        }
        return counts;
    }
}
