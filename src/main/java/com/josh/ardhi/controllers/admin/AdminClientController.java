package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AdminClientForm;
import com.josh.ardhi.forms.ClientKeywordForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.KeywordType;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientKeywordService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/clients")
public class AdminClientController {

    private static final int CLIENTS_PAGE_SIZE = 10;

    private final AppUserService appUserService;
    private final ClientKeywordService clientKeywordService;

    public AdminClientController(AppUserService appUserService, ClientKeywordService clientKeywordService) {
        this.appUserService = appUserService;
        this.clientKeywordService = clientKeywordService;
    }

    @GetMapping("")
    public String list(@RequestParam(name = "q", defaultValue = "") String q,
                       @RequestParam(name = "verified", defaultValue = "") String verified,
                       @RequestParam(name = "status", defaultValue = "") String status,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        populateClientsListModel(q, verified, status, page, model);
        return "admin/clients/list";
    }

    @HxRequest
    @GetMapping("/table")
    public String tableFragment(@RequestParam(name = "q", defaultValue = "") String q,
                                @RequestParam(name = "verified", defaultValue = "") String verified,
                                @RequestParam(name = "status", defaultValue = "") String status,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        populateClientsListModel(q, verified, status, page, model);
        return "admin/clients/list :: clientsTableCard";
    }

    @HxRequest
    @GetMapping("/modal/create")
    public String createModal(Model model) {
        model.addAttribute("clientForm", new AdminClientForm());
        model.addAttribute("isEdit", false);
        return "admin/clients/fragments/client-modals :: clientFormModal";
    }

    @HxRequest
    @PostMapping("/process-create")
    public String processCreate(@Valid @ModelAttribute("clientForm") AdminClientForm clientForm,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletResponse response) {
        model.addAttribute("isEdit", false);
        if (!StringUtils.hasText(clientForm.getPassword()) || clientForm.getPassword().trim().length() < 8) {
            bindingResult.rejectValue("password", "password.short", "Password must be at least 8 characters");
        }
        if (bindingResult.hasErrors()) {
            return "admin/clients/fragments/client-modals :: clientFormModal";
        }

        try {
            appUserService.createClient(clientForm);
            response.setHeader("HX-Trigger", "clientSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: clientFormModal";
        }
    }

    @HxRequest
    @GetMapping("/modal/edit/{id}")
    public String editModal(@PathVariable UUID id, Model model) {
        try {
            AppUser client = appUserService.getById(id);
            model.addAttribute("clientForm", AdminClientForm.fromEntity(client));
            model.addAttribute("isEdit", true);
        } catch (AppException e) {
            model.addAttribute("clientForm", new AdminClientForm());
            model.addAttribute("isEdit", true);
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/clients/fragments/client-modals :: clientFormModal";
    }

    @HxRequest
    @PostMapping("/process-edit/{id}")
    public String processEdit(@PathVariable UUID id,
                              @Valid @ModelAttribute("clientForm") AdminClientForm clientForm,
                              BindingResult bindingResult,
                              Model model,
                              HttpServletResponse response) {
        clientForm.setId(id);
        model.addAttribute("isEdit", true);
        if (StringUtils.hasText(clientForm.getPassword()) && clientForm.getPassword().trim().length() < 8) {
            bindingResult.rejectValue("password", "password.short", "New password must be at least 8 characters");
        }
        if (bindingResult.hasErrors()) {
            return "admin/clients/fragments/client-modals :: clientFormModal";
        }

        try {
            appUserService.updateClient(id, clientForm);
            response.setHeader("HX-Trigger", "clientSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: clientFormModal";
        }
    }

    @HxRequest
    @GetMapping("/modal/status/{id}")
    public String statusModal(@PathVariable UUID id, Model model) {
        try {
            AppUser client = appUserService.getById(id);
            model.addAttribute("client", client);
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/clients/fragments/client-modals :: clientStatusModal";
    }

    @HxRequest
    @PostMapping("/process-status/{id}")
    public String processStatus(@PathVariable UUID id,
                                @RequestParam("active") boolean active,
                                @RequestParam(name = "deactivationMemo", required = false) String deactivationMemo,
                                Model model,
                                HttpServletResponse response) {
        try {
            appUserService.updateClientStatus(id, active, deactivationMemo);
            response.setHeader("HX-Trigger", "clientSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            try {
                model.addAttribute("client", appUserService.getById(id));
            } catch (AppException ignored) {
            }
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: clientStatusModal";
        }
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        model.addAttribute("currentTab", "overview");
        return "admin/clients/tabs/overview";
    }

    @HxRequest
    @PostMapping("/show/{id}/notes")
    public String saveInternalNotes(@PathVariable UUID id,
                                    @RequestParam(name = "internalNotes", defaultValue = "") String internalNotes,
                                    Model model) {
        try {
            AppUser updated = appUserService.updateInternalNotes(id, internalNotes);
            model.addAttribute("client", updated);
            model.addAttribute("notesSuccess", "Internal notes saved successfully.");
        } catch (AppException e) {
            try {
                model.addAttribute("client", appUserService.getById(id));
            } catch (AppException ignored) {
            }
            model.addAttribute("notesFailure", e.getMessage());
        }
        return "admin/clients/tabs/overview :: internalNotesCard";
    }

    @GetMapping("/show/{id}/keywords")
    public String keywords(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        model.addAttribute("currentTab", "keywords");
        return "admin/clients/tabs/keywords";
    }

    @HxRequest
    @GetMapping("/show/{id}/keywords/table")
    public String keywordsTableFragment(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        return "admin/clients/tabs/keywords :: keywordsSection";
    }

    @HxRequest
    @GetMapping("/show/{id}/keywords/modal/create")
    public String createKeywordModal(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("client", appUserService.getById(id));
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        model.addAttribute("keywordForm", new ClientKeywordForm());
        model.addAttribute("keywordTypes", KeywordType.values());
        model.addAttribute("isEdit", false);
        return "admin/clients/fragments/client-modals :: keywordFormModal";
    }

    @HxRequest
    @PostMapping("/show/{id}/keywords/process-create")
    public String processCreateKeyword(@PathVariable UUID id,
                                       @Valid @ModelAttribute("keywordForm") ClientKeywordForm keywordForm,
                                       BindingResult bindingResult,
                                       Model model,
                                       HttpServletResponse response) {
        model.addAttribute("keywordTypes", KeywordType.values());
        model.addAttribute("isEdit", false);
        try {
            model.addAttribute("client", appUserService.getById(id));
        } catch (AppException ignored) {
        }

        if (bindingResult.hasErrors()) {
            return "admin/clients/fragments/client-modals :: keywordFormModal";
        }

        try {
            clientKeywordService.createKeyword(id, keywordForm);
            response.setHeader("HX-Trigger", "keywordSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: keywordFormModal";
        }
    }

    @HxRequest
    @GetMapping("/show/{id}/keywords/modal/edit/{keywordId}")
    public String editKeywordModal(@PathVariable UUID id, @PathVariable UUID keywordId, Model model) {
        model.addAttribute("keywordTypes", KeywordType.values());
        model.addAttribute("isEdit", true);
        try {
            model.addAttribute("client", appUserService.getById(id));
            ClientKeyword kw = clientKeywordService.getById(keywordId);
            model.addAttribute("keywordForm", ClientKeywordForm.fromEntity(kw));
        } catch (AppException e) {
            model.addAttribute("keywordForm", new ClientKeywordForm());
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/clients/fragments/client-modals :: keywordFormModal";
    }

    @HxRequest
    @PostMapping("/show/{id}/keywords/process-edit/{keywordId}")
    public String processEditKeyword(@PathVariable UUID id,
                                     @PathVariable UUID keywordId,
                                     @Valid @ModelAttribute("keywordForm") ClientKeywordForm keywordForm,
                                     BindingResult bindingResult,
                                     Model model,
                                     HttpServletResponse response) {
        keywordForm.setId(keywordId);
        model.addAttribute("keywordTypes", KeywordType.values());
        model.addAttribute("isEdit", true);
        try {
            model.addAttribute("client", appUserService.getById(id));
        } catch (AppException ignored) {
        }

        if (bindingResult.hasErrors()) {
            return "admin/clients/fragments/client-modals :: keywordFormModal";
        }

        try {
            clientKeywordService.updateKeyword(id, keywordId, keywordForm);
            response.setHeader("HX-Trigger", "keywordSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: keywordFormModal";
        }
    }

    @HxRequest
    @PostMapping("/show/{id}/keywords/{keywordId}/toggle-pause")
    public String togglePauseKeyword(@PathVariable UUID id,
                                     @PathVariable UUID keywordId,
                                     Model model) {
        try {
            clientKeywordService.togglePauseKeyword(id, keywordId);
        } catch (AppException e) {
            model.addAttribute("keywordsError", e.getMessage());
        }
        loadClientAndKeywords(id.toString(), model);
        return "admin/clients/tabs/keywords :: keywordsSection";
    }

    @HxRequest
    @PostMapping("/show/{id}/keywords/{keywordId}/delete")
    public String deleteKeyword(@PathVariable UUID id,
                                @PathVariable UUID keywordId,
                                Model model) {
        try {
            clientKeywordService.deleteKeyword(id, keywordId);
        } catch (AppException e) {
            model.addAttribute("keywordsError", e.getMessage());
        }
        loadClientAndKeywords(id.toString(), model);
        return "admin/clients/tabs/keywords :: keywordsSection";
    }

    @GetMapping("/show/{id}/matches")
    public String matches(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        model.addAttribute("currentTab", "matches");
        return "admin/clients/tabs/matches";
    }

    @GetMapping("/show/{id}/notifications")
    public String notifications(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        model.addAttribute("currentTab", "notifications");
        return "admin/clients/tabs/notifications";
    }

    @GetMapping("/show/{id}/subscriptions")
    public String subscriptions(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        model.addAttribute("currentTab", "subscriptions");
        return "admin/clients/tabs/subscriptions";
    }

    @GetMapping("/show/{id}/transactions")
    public String transactions(@PathVariable String id, Model model) {
        loadClientAndKeywords(id, model);
        model.addAttribute("currentTab", "transactions");
        return "admin/clients/tabs/transactions";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "clients";
    }

    private void populateClientsListModel(String q, String verified, String status, int page, Model model) {
        Page<AppUser> clientsPage = appUserService.searchClients(q, verified, status, page, CLIENTS_PAGE_SIZE);
        Map<UUID, Long> keywordCounts = new HashMap<>();
        for (AppUser c : clientsPage.getContent()) {
            keywordCounts.put(c.getId(), clientKeywordService.countActiveKeywordsForClient(c.getId()));
        }

        model.addAttribute("clientsPage", clientsPage);
        model.addAttribute("clients", clientsPage.getContent());
        model.addAttribute("keywordCounts", keywordCounts);
        model.addAttribute("q", q != null ? q.trim() : "");
        model.addAttribute("selectedVerified", verified != null ? verified.trim().toLowerCase() : "");
        model.addAttribute("selectedStatus", status != null ? status.trim().toLowerCase() : "");
    }

    private void loadClientAndKeywords(String id, Model model) {
        try {
            UUID uuid = UUID.fromString(id);
            AppUser client = appUserService.getById(uuid);
            List<ClientKeyword> keywords = clientKeywordService.getKeywordsForClient(uuid);
            long activeKeywordsCount = clientKeywordService.countActiveKeywordsForClient(uuid);

            model.addAttribute("client", client);
            model.addAttribute("keywords", keywords);
            model.addAttribute("keywordsCount", keywords.size());
            model.addAttribute("activeKeywordsCount", activeKeywordsCount);
        } catch (IllegalArgumentException | AppException ignored) {
        }
    }
}
