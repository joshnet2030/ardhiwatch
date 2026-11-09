package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AdminClientForm;
import com.josh.ardhi.forms.ClientKeywordForm;
import com.josh.ardhi.forms.CreateClientSubscriptionForm;
import com.josh.ardhi.forms.RecordPaymentForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.ClientSubscriptionPlan;
import com.josh.ardhi.models.KeywordType;
import com.josh.ardhi.models.PaymentMethod;
import com.josh.ardhi.models.PaymentTransaction;
import com.josh.ardhi.models.PaymentTransactionStatus;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientKeywordService;
import com.josh.ardhi.services.ClientSubscriptionService;
import com.josh.ardhi.services.SubscriptionPlanService;
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

import java.math.BigDecimal;
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
    private final ClientSubscriptionService clientSubscriptionService;
    private final SubscriptionPlanService subscriptionPlanService;

    public AdminClientController(AppUserService appUserService,
                                 ClientKeywordService clientKeywordService,
                                 ClientSubscriptionService clientSubscriptionService,
                                 SubscriptionPlanService subscriptionPlanService) {
        this.appUserService = appUserService;
        this.clientKeywordService = clientKeywordService;
        this.clientSubscriptionService = clientSubscriptionService;
        this.subscriptionPlanService = subscriptionPlanService;
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
        loadClientFullDetails(id, model);
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
        loadClientFullDetails(id, model);
        model.addAttribute("currentTab", "keywords");
        return "admin/clients/tabs/keywords";
    }

    @HxRequest
    @GetMapping("/show/{id}/keywords/table")
    public String keywordsTableFragment(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
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
        loadClientFullDetails(id.toString(), model);
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
        loadClientFullDetails(id.toString(), model);
        return "admin/clients/tabs/keywords :: keywordsSection";
    }

    @GetMapping("/show/{id}/subscriptions")
    public String subscriptions(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
        model.addAttribute("currentTab", "subscriptions");
        return "admin/clients/tabs/subscriptions";
    }

    @HxRequest
    @GetMapping("/show/{id}/subscriptions/table")
    public String subscriptionsTableFragment(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
        return "admin/clients/tabs/subscriptions :: subscriptionsSection";
    }

    @HxRequest
    @GetMapping("/show/{id}/subscriptions/modal/create")
    public String createSubscriptionModal(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("client", appUserService.getById(id));
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        model.addAttribute("subForm", new CreateClientSubscriptionForm());
        model.addAttribute("nextAvailableStartDate", clientSubscriptionService.getNextAvailableStartDate(id));
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "admin/clients/fragments/client-modals :: subscriptionFormModal";
    }

    @HxRequest
    @PostMapping("/show/{id}/subscriptions/process-create")
    public String processCreateSubscription(@PathVariable UUID id,
                                            @Valid @ModelAttribute("subForm") CreateClientSubscriptionForm subForm,
                                            BindingResult bindingResult,
                                            Model model,
                                            HttpServletResponse response) {
        model.addAttribute("nextAvailableStartDate", clientSubscriptionService.getNextAvailableStartDate(id));
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        try {
            model.addAttribute("client", appUserService.getById(id));
        } catch (AppException ignored) {
        }

        if (bindingResult.hasErrors()) {
            return "admin/clients/fragments/client-modals :: subscriptionFormModal";
        }

        try {
            clientSubscriptionService.createSubscription(id, subForm);
            response.setHeader("HX-Trigger", "subscriptionSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: subscriptionFormModal";
        }
    }

    @HxRequest
    @GetMapping("/show/{id}/subscriptions/{subId}/modal/cancel")
    public String cancelSubscriptionModal(@PathVariable UUID id,
                                          @PathVariable UUID subId,
                                          Model model) {
        try {
            model.addAttribute("client", appUserService.getById(id));
            model.addAttribute("subscription", clientSubscriptionService.getSubscriptionById(subId));
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/clients/fragments/client-modals :: cancelSubscriptionModal";
    }

    @HxRequest
    @PostMapping("/show/{id}/subscriptions/{subId}/cancel")
    public String cancelSubscription(@PathVariable UUID id,
                                     @PathVariable UUID subId,
                                     @RequestParam(name = "reason", required = false) String reason,
                                     Model model,
                                     HttpServletResponse response) {
        try {
            ClientSubscriptionPlan beforeSub = clientSubscriptionService.getSubscriptionById(subId);
            boolean alreadyStarted = beforeSub.hasStarted();
            BigDecimal paidBefore = beforeSub.getPaidAmount() != null ? beforeSub.getPaidAmount() : BigDecimal.ZERO;

            clientSubscriptionService.cancelSubscription(id, subId, reason);
            response.setHeader("HX-Trigger", "subscriptionCancelled");

            if (alreadyStarted) {
                model.addAttribute("subscriptionsWarning",
                        "Subscription cancelled. Since the subscription had already started (" + beforeSub.getStartDate()
                                + "), the user will not get a refund to their wallet.");
            } else if (paidBefore.compareTo(BigDecimal.ZERO) > 0) {
                AppUser updatedClient = appUserService.getById(id);
                model.addAttribute("subscriptionsSuccess",
                        "Subscription cancelled before start date. KES " + String.format("%,.2f", paidBefore)
                                + " has been moved to the user's wallet balance (New wallet balance: KES "
                                + String.format("%,.2f", updatedClient.getWalletBalance()) + ").");
            } else {
                model.addAttribute("subscriptionsSuccess",
                        "Subscription cancelled before start date (no payments had been made on this subscription).");
            }
        } catch (AppException e) {
            response.setHeader("HX-Trigger", "subscriptionCancelled");
            model.addAttribute("subscriptionsError", e.getMessage());
        }
        loadClientFullDetails(id.toString(), model);
        return "admin/clients/tabs/subscriptions :: subscriptionsSection";
    }

    @GetMapping("/show/{id}/transactions")
    public String transactions(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
        model.addAttribute("currentTab", "transactions");
        return "admin/clients/tabs/transactions";
    }

    @HxRequest
    @GetMapping("/show/{id}/transactions/table")
    public String transactionsTableFragment(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
        return "admin/clients/tabs/transactions :: transactionsSection";
    }

    @HxRequest
    @GetMapping("/show/{id}/transactions/modal/record")
    public String recordClientPaymentModal(@PathVariable UUID id,
                                           @RequestParam(name = "subscriptionId", required = false) UUID subscriptionId,
                                           Model model) {
        RecordPaymentForm form = new RecordPaymentForm();
        form.setClientId(id);
        try {
            AppUser client = appUserService.getById(id);
            model.addAttribute("client", client);
            form.setPaidFrom(client.getPhone());
            if (subscriptionId != null) {
                ClientSubscriptionPlan sub = clientSubscriptionService.getSubscriptionById(subscriptionId);
                form.setClientSubscriptionPlanId(sub.getId());
                form.setSubscriptionPlanId(sub.getSubscriptionPlan().getId());
                if (sub.getBalance().signum() > 0) {
                    form.setAmount(sub.getBalance());
                }
            }
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        model.addAttribute("paymentForm", form);
        model.addAttribute("clientSubscriptions", clientSubscriptionService.getClientSubscriptions(id));
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("paymentStatuses", PaymentTransactionStatus.values());
        return "admin/clients/fragments/client-modals :: clientPaymentFormModal";
    }

    @HxRequest
    @PostMapping("/show/{id}/transactions/process-record")
    public String processRecordClientPayment(@PathVariable UUID id,
                                             @Valid @ModelAttribute("paymentForm") RecordPaymentForm paymentForm,
                                             BindingResult bindingResult,
                                             Model model,
                                             HttpServletResponse response) {
        paymentForm.setClientId(id);
        model.addAttribute("clientSubscriptions", clientSubscriptionService.getClientSubscriptions(id));
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("paymentStatuses", PaymentTransactionStatus.values());
        try {
            model.addAttribute("client", appUserService.getById(id));
        } catch (AppException ignored) {
        }

        if (bindingResult.hasErrors()) {
            return "admin/clients/fragments/client-modals :: clientPaymentFormModal";
        }

        try {
            clientSubscriptionService.recordPayment(paymentForm);
            response.setHeader("HX-Trigger", "paymentSaved");
            return "admin/clients/fragments/client-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/clients/fragments/client-modals :: clientPaymentFormModal";
        }
    }

    @HxRequest
    @PostMapping("/show/{id}/transactions/{txId}/reconcile")
    public String reconcileClientTransaction(@PathVariable UUID id,
                                             @PathVariable UUID txId,
                                             Model model) {
        try {
            clientSubscriptionService.reconcileTransaction(txId, null);
        } catch (AppException e) {
            model.addAttribute("transactionsError", e.getMessage());
        }
        loadClientFullDetails(id.toString(), model);
        return "admin/clients/tabs/transactions :: transactionsSection";
    }

    @GetMapping("/show/{id}/matches")
    public String matches(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
        model.addAttribute("currentTab", "matches");
        return "admin/clients/tabs/matches";
    }

    @GetMapping("/show/{id}/notifications")
    public String notifications(@PathVariable String id, Model model) {
        loadClientFullDetails(id, model);
        model.addAttribute("currentTab", "notifications");
        return "admin/clients/tabs/notifications";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "clients";
    }

    private void populateClientsListModel(String q, String verified, String status, int page, Model model) {
        Page<AppUser> clientsPage = appUserService.searchClients(q, verified, status, page, CLIENTS_PAGE_SIZE);
        Map<UUID, Long> keywordCounts = new HashMap<>();
        Map<UUID, ClientSubscriptionPlan> currentSubscriptions = new HashMap<>();
        for (AppUser c : clientsPage.getContent()) {
            keywordCounts.put(c.getId(), clientKeywordService.countActiveKeywordsForClient(c.getId()));
            clientSubscriptionService.getCurrentSubscriptionForClient(c.getId())
                    .ifPresent(sub -> currentSubscriptions.put(c.getId(), sub));
        }

        model.addAttribute("clientsPage", clientsPage);
        model.addAttribute("clients", clientsPage.getContent());
        model.addAttribute("keywordCounts", keywordCounts);
        model.addAttribute("currentSubscriptions", currentSubscriptions);
        model.addAttribute("q", q != null ? q.trim() : "");
        model.addAttribute("selectedVerified", verified != null ? verified.trim().toLowerCase() : "");
        model.addAttribute("selectedStatus", status != null ? status.trim().toLowerCase() : "");
    }

    private void loadClientFullDetails(String id, Model model) {
        try {
            UUID uuid = UUID.fromString(id);
            AppUser client = appUserService.getById(uuid);
            List<ClientKeyword> keywords = clientKeywordService.getKeywordsForClient(uuid);
            long activeKeywordsCount = clientKeywordService.countActiveKeywordsForClient(uuid);
            List<ClientSubscriptionPlan> subscriptions = clientSubscriptionService.getClientSubscriptions(uuid);
            ClientSubscriptionPlan currentSubscription = clientSubscriptionService
                    .getCurrentSubscriptionForClient(uuid).orElse(null);
            List<PaymentTransaction> transactions = clientSubscriptionService.getClientTransactions(uuid);

            model.addAttribute("client", client);
            model.addAttribute("keywords", keywords);
            model.addAttribute("keywordsCount", keywords.size());
            model.addAttribute("activeKeywordsCount", activeKeywordsCount);
            model.addAttribute("subscriptions", subscriptions);
            model.addAttribute("subscriptionsCount", subscriptions.size());
            model.addAttribute("currentSubscription", currentSubscription);
            model.addAttribute("transactions", transactions);
            model.addAttribute("transactionsCount", transactions.size());
            model.addAttribute("totalPaid", clientSubscriptionService.getTotalPaidByClient(uuid));
        } catch (IllegalArgumentException | AppException ignored) {
        }
    }
}
