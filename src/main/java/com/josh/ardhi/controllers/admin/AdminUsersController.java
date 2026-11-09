package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AdminCreateUserForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.services.AppUserService;
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

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

    private static final int USERS_PAGE_SIZE = 10;

    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;

    public AdminUsersController(AppUserService appUserService, AppUserRepository appUserRepository) {
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("")
    public String list(@RequestParam(name = "q", defaultValue = "") String q,
                       @RequestParam(name = "role", defaultValue = "") String role,
                       @RequestParam(name = "status", defaultValue = "") String status,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        populateUsersListModel(q, role, status, page, model);
        return "admin/users/list";
    }

    @HxRequest
    @GetMapping("/table")
    public String tableFragment(@RequestParam(name = "q", defaultValue = "") String q,
                                @RequestParam(name = "role", defaultValue = "") String role,
                                @RequestParam(name = "status", defaultValue = "") String status,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        populateUsersListModel(q, role, status, page, model);
        return "admin/users/list :: usersTableCard";
    }

    private void populateUsersListModel(String q, String role, String status, int page, Model model) {
        Page<AppUser> usersPage = appUserService.searchUsers(q, role, status, page, USERS_PAGE_SIZE);
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("q", q != null ? q.trim() : "");
        model.addAttribute("selectedRole", role != null ? role.trim().toUpperCase() : "");
        model.addAttribute("selectedStatus", status != null ? status.trim().toLowerCase() : "");
        model.addAttribute("adminCount", appUserRepository.countByRoleAndIsActiveTrue(AppUserRole.ADMIN));
        model.addAttribute("clientCount", appUserRepository.countByRoleAndIsActiveTrue(AppUserRole.CLIENT));
        model.addAttribute("suspendedCount", appUserRepository.countByIsActiveFalse());
        model.addAttribute("totalCount", appUserRepository.count());
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("createUserForm", new AdminCreateUserForm());
        model.addAttribute("roles", AppUserRole.values());
        return "admin/users/create";
    }

    @HxRequest
    @PostMapping("/process-create")
    public String processCreateHx(@Valid @ModelAttribute("createUserForm") AdminCreateUserForm createUserForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  HttpServletResponse response) {
        model.addAttribute("roles", AppUserRole.values());
        validateForm(createUserForm, bindingResult);

        if (bindingResult.hasErrors()) {
            return "admin/users/create :: createUserForm";
        }

        try {
            AppUser created = appUserService.createUser(createUserForm);
            response.setHeader("HX-Redirect", "/admin/users/show/" + created.getId());
            return "admin/users/create :: createUserForm";
        } catch (AppException e) {
            model.addAttribute("createFailure", e.getMessage());
            return "admin/users/create :: createUserForm";
        }
    }

    @PostMapping("/process-create")
    public String processCreate(@Valid @ModelAttribute("createUserForm") AdminCreateUserForm createUserForm,
                                BindingResult bindingResult,
                                Model model) {
        model.addAttribute("roles", AppUserRole.values());
        validateForm(createUserForm, bindingResult);

        if (bindingResult.hasErrors()) {
            return "admin/users/create";
        }

        try {
            AppUser created = appUserService.createUser(createUserForm);
            return "redirect:/admin/users/show/" + created.getId();
        } catch (AppException e) {
            model.addAttribute("createFailure", e.getMessage());
            return "admin/users/create";
        }
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable String id, Model model) {
        loadUserIfValid(id, model);
        model.addAttribute("currentTab", "overview");
        return "admin/users/tabs/overview";
    }

    @GetMapping("/show/{id}/activities")
    public String activities(@PathVariable String id, Model model) {
        loadUserIfValid(id, model);
        model.addAttribute("currentTab", "activities");
        return "admin/users/tabs/activities";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "users";
    }

    private void validateForm(AdminCreateUserForm form, BindingResult bindingResult) {
        if (StringUtils.hasText(form.getPassword())
                && StringUtils.hasText(form.getConfirmPassword())
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }
        if (StringUtils.hasText(form.getEmail())
                && appUserRepository.findFirstByEmailIgnoreCase(form.getEmail().trim()).isPresent()) {
            bindingResult.rejectValue("email", "email.exists", "A user with this email address already exists");
        }
    }

    private void loadUserIfValid(String id, Model model) {
        try {
            UUID uuid = UUID.fromString(id);
            AppUser user = appUserService.getById(uuid);
            model.addAttribute("user", user);
        } catch (IllegalArgumentException | AppException ignored) {
            // Leave user unset for non-UUID placeholder IDs
        }
    }
}
