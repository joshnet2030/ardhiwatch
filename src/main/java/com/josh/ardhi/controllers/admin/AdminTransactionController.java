package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.RecordPaymentForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.PaymentMethod;
import com.josh.ardhi.models.PaymentTransaction;
import com.josh.ardhi.models.PaymentTransactionStatus;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientSubscriptionService;
import com.josh.ardhi.services.SubscriptionPlanService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/transactions")
public class AdminTransactionController {

    private static final int TRANSACTIONS_PAGE_SIZE = 10;

    private final ClientSubscriptionService clientSubscriptionService;
    private final AppUserService appUserService;
    private final SubscriptionPlanService subscriptionPlanService;

    public AdminTransactionController(ClientSubscriptionService clientSubscriptionService,
                                      AppUserService appUserService,
                                      SubscriptionPlanService subscriptionPlanService) {
        this.clientSubscriptionService = clientSubscriptionService;
        this.appUserService = appUserService;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @GetMapping("")
    public String list(@RequestParam(name = "q", defaultValue = "") String q,
                       @RequestParam(name = "status", defaultValue = "") String status,
                       @RequestParam(name = "method", defaultValue = "") String method,
                       @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        populateTransactionsListModel(q, status, method, from, to, page, model);
        return "admin/transactions/list";
    }

    @HxRequest
    @GetMapping("/table")
    public String tableFragment(@RequestParam(name = "q", defaultValue = "") String q,
                                @RequestParam(name = "status", defaultValue = "") String status,
                                @RequestParam(name = "method", defaultValue = "") String method,
                                @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        populateTransactionsListModel(q, status, method, from, to, page, model);
        return "admin/transactions/list :: transactionsListSection";
    }

    @HxRequest
    @GetMapping("/modal/record")
    public String recordPaymentModal(Model model) {
        RecordPaymentForm form = new RecordPaymentForm();
        form.setPaymentMethod(PaymentMethod.MANUAL);
        model.addAttribute("paymentForm", form);
        populateModalLookups(model);
        return "admin/transactions/fragments/transaction-modals :: recordPaymentModal";
    }

    @HxRequest
    @PostMapping("/process-record")
    public String processRecordPayment(@Valid @ModelAttribute("paymentForm") RecordPaymentForm paymentForm,
                                       BindingResult bindingResult,
                                       Model model,
                                       HttpServletResponse response) {
        populateModalLookups(model);
        if (paymentForm.getClientId() == null) {
            bindingResult.rejectValue("clientId", "client.required", "Please select a client");
        }
        if (bindingResult.hasErrors()) {
            return "admin/transactions/fragments/transaction-modals :: recordPaymentModal";
        }

        try {
            clientSubscriptionService.recordPayment(paymentForm);
            response.setHeader("HX-Trigger", "transactionSaved");
            return "admin/transactions/fragments/transaction-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/transactions/fragments/transaction-modals :: recordPaymentModal";
        }
    }

    @HxRequest
    @GetMapping("/modal/reconcile/{id}")
    public String reconcileModal(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("tx", clientSubscriptionService.getTransactionById(id));
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/transactions/fragments/transaction-modals :: reconcileModal";
    }

    @HxRequest
    @PostMapping("/process-reconcile/{id}")
    public String processReconcile(@PathVariable UUID id,
                                   @RequestParam(name = "receiptNumber", required = false) String receiptNumber,
                                   Model model,
                                   HttpServletResponse response) {
        try {
            clientSubscriptionService.reconcileTransaction(id, receiptNumber);
            response.setHeader("HX-Trigger", "transactionSaved");
            return "admin/transactions/fragments/transaction-modals :: modalClosed";
        } catch (AppException e) {
            try {
                model.addAttribute("tx", clientSubscriptionService.getTransactionById(id));
            } catch (AppException ignored) {
            }
            model.addAttribute("modalError", e.getMessage());
            return "admin/transactions/fragments/transaction-modals :: reconcileModal";
        }
    }

    @HxRequest
    @GetMapping("/modal/refund/{id}")
    public String refundModal(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("tx", clientSubscriptionService.getTransactionById(id));
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/transactions/fragments/transaction-modals :: refundModal";
    }

    @HxRequest
    @PostMapping("/process-refund/{id}")
    public String processRefund(@PathVariable UUID id,
                                @RequestParam(name = "reason", required = false) String reason,
                                Model model,
                                HttpServletResponse response) {
        try {
            clientSubscriptionService.refundTransaction(id, reason);
            response.setHeader("HX-Trigger", "transactionSaved");
            return "admin/transactions/fragments/transaction-modals :: modalClosed";
        } catch (AppException e) {
            try {
                model.addAttribute("tx", clientSubscriptionService.getTransactionById(id));
            } catch (AppException ignored) {
            }
            model.addAttribute("modalError", e.getMessage());
            return "admin/transactions/fragments/transaction-modals :: refundModal";
        }
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable String id, Model model) {
        try {
            UUID uuid = UUID.fromString(id);
            PaymentTransaction tx = clientSubscriptionService.getTransactionById(uuid);
            model.addAttribute("tx", tx);
        } catch (IllegalArgumentException | AppException ignored) {
        }
        return "admin/transactions/tabs/overview";
    }

    @HxRequest
    @PostMapping("/show/{id}/notes")
    public String saveTransactionNotes(@PathVariable UUID id,
                                       @RequestParam(name = "adminNotes", defaultValue = "") String adminNotes,
                                       Model model) {
        try {
            PaymentTransaction updated = clientSubscriptionService.updateTransactionNotes(id, adminNotes);
            model.addAttribute("tx", updated);
            model.addAttribute("notesSuccess", "Admin notes saved successfully.");
        } catch (AppException e) {
            try {
                model.addAttribute("tx", clientSubscriptionService.getTransactionById(id));
            } catch (AppException ignored) {
            }
            model.addAttribute("notesFailure", e.getMessage());
        }
        return "admin/transactions/tabs/overview :: adminNotesCard";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "transactions";
    }

    private void populateTransactionsListModel(String q,
                                               String status,
                                               String method,
                                               LocalDate from,
                                               LocalDate to,
                                               int page,
                                               Model model) {
        Page<PaymentTransaction> transactionsPage = clientSubscriptionService
                .searchTransactions(q, status, method, from, to, page, TRANSACTIONS_PAGE_SIZE);

        model.addAttribute("transactionsPage", transactionsPage);
        model.addAttribute("transactions", transactionsPage.getContent());
        model.addAttribute("q", q != null ? q.trim() : "");
        model.addAttribute("selectedStatus", status != null ? status.trim().toUpperCase() : "");
        model.addAttribute("selectedMethod", method != null ? method.trim().toUpperCase() : "");
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        model.addAttribute("collectedThisMonth", clientSubscriptionService.getCollectedThisMonth());
        model.addAttribute("pendingTotal", clientSubscriptionService.getPendingTotal());
        model.addAttribute("pendingCount", clientSubscriptionService.getPendingCount());
        model.addAttribute("failedThisMonthCount", clientSubscriptionService.getFailedThisMonthCount());
        model.addAttribute("refundedThisMonth", clientSubscriptionService.getRefundedThisMonth());
    }

    private void populateModalLookups(Model model) {
        List<AppUser> clients = appUserService.searchClients("", "", "", 0, 500).getContent();
        model.addAttribute("clients", clients);
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("paymentStatuses", PaymentTransactionStatus.values());
    }
}
