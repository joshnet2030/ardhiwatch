package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.SubscriptionPlanForm;
import com.josh.ardhi.models.SubscriptionPlan;
import com.josh.ardhi.services.SubscriptionPlanService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/admin/subscriptions")
public class AdminSubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;

    public AdminSubscriptionController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @GetMapping("")
    public String list(Model model) {
        populatePlansListModel(model);
        return "admin/subscriptions/list";
    }

    @HxRequest
    @GetMapping("/table")
    public String tableFragment(Model model) {
        populatePlansListModel(model);
        return "admin/subscriptions/list :: plansListSection";
    }

    @HxRequest
    @GetMapping("/modal/create")
    public String createModal(Model model) {
        model.addAttribute("planForm", new SubscriptionPlanForm());
        model.addAttribute("isEdit", false);
        return "admin/subscriptions/fragments/plan-modals :: planFormModal";
    }

    @HxRequest
    @PostMapping("/process-create")
    public String processCreate(@Valid @ModelAttribute("planForm") SubscriptionPlanForm planForm,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletResponse response) {
        model.addAttribute("isEdit", false);
        if (bindingResult.hasErrors()) {
            return "admin/subscriptions/fragments/plan-modals :: planFormModal";
        }

        try {
            subscriptionPlanService.createPlan(planForm);
            response.setHeader("HX-Trigger", "planSaved");
            return "admin/subscriptions/fragments/plan-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/subscriptions/fragments/plan-modals :: planFormModal";
        }
    }

    @HxRequest
    @GetMapping("/modal/edit/{id}")
    public String editModal(@PathVariable UUID id, Model model) {
        try {
            SubscriptionPlan plan = subscriptionPlanService.getById(id);
            model.addAttribute("planForm", SubscriptionPlanForm.fromEntity(plan));
            model.addAttribute("isEdit", true);
        } catch (AppException e) {
            model.addAttribute("planForm", new SubscriptionPlanForm());
            model.addAttribute("isEdit", true);
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/subscriptions/fragments/plan-modals :: planFormModal";
    }

    @HxRequest
    @PostMapping("/process-edit/{id}")
    public String processEdit(@PathVariable UUID id,
                              @Valid @ModelAttribute("planForm") SubscriptionPlanForm planForm,
                              BindingResult bindingResult,
                              Model model,
                              HttpServletResponse response) {
        planForm.setId(id);
        model.addAttribute("isEdit", true);
        if (bindingResult.hasErrors()) {
            return "admin/subscriptions/fragments/plan-modals :: planFormModal";
        }

        try {
            subscriptionPlanService.updatePlan(id, planForm);
            response.setHeader("HX-Trigger", "planSaved");
            return "admin/subscriptions/fragments/plan-modals :: modalClosed";
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
            return "admin/subscriptions/fragments/plan-modals :: planFormModal";
        }
    }

    @HxRequest
    @GetMapping("/modal/status/{id}")
    public String statusModal(@PathVariable UUID id, Model model) {
        try {
            SubscriptionPlan plan = subscriptionPlanService.getById(id);
            model.addAttribute("plan", plan);
        } catch (AppException e) {
            model.addAttribute("modalError", e.getMessage());
        }
        return "admin/subscriptions/fragments/plan-modals :: planStatusModal";
    }

    @HxRequest
    @PostMapping("/process-status/{id}")
    public String processStatus(@PathVariable UUID id,
                                @RequestParam("active") boolean active,
                                @RequestParam(name = "deactivationMemo", required = false) String deactivationMemo,
                                Model model,
                                HttpServletResponse response) {
        try {
            subscriptionPlanService.updatePlanStatus(id, active, deactivationMemo);
            response.setHeader("HX-Trigger", "planSaved");
            return "admin/subscriptions/fragments/plan-modals :: modalClosed";
        } catch (AppException e) {
            try {
                model.addAttribute("plan", subscriptionPlanService.getById(id));
            } catch (AppException ignored) {
            }
            model.addAttribute("modalError", e.getMessage());
            return "admin/subscriptions/fragments/plan-modals :: planStatusModal";
        }
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable String id, Model model) {
        loadPlanIfValid(id, model);
        model.addAttribute("currentTab", "overview");
        return "admin/subscriptions/tabs/overview";
    }

    @GetMapping("/show/{id}/transactions")
    public String transactions(@PathVariable String id, Model model) {
        loadPlanIfValid(id, model);
        model.addAttribute("currentTab", "transactions");
        return "admin/subscriptions/tabs/transactions";
    }

    @GetMapping("/show/{id}/clients")
    public String clients(@PathVariable String id, Model model) {
        loadPlanIfValid(id, model);
        model.addAttribute("currentTab", "clients");
        return "admin/subscriptions/tabs/clients";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "subscriptions";
    }

    private void populatePlansListModel(Model model) {
        model.addAttribute("plans", subscriptionPlanService.getAllPlans());
        model.addAttribute("activePlansCount", subscriptionPlanService.countActivePlans());
        model.addAttribute("retiredPlansCount", subscriptionPlanService.countRetiredPlans());
    }

    private void loadPlanIfValid(String id, Model model) {
        try {
            UUID uuid = UUID.fromString(id);
            SubscriptionPlan plan = subscriptionPlanService.getById(uuid);
            model.addAttribute("plan", plan);
        } catch (IllegalArgumentException | AppException ignored) {
        }
    }
}
