package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.CreateUserForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.services.AppUserService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppUserServiceImpl implements AppUserService {


    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;

    public AppUserServiceImpl(PasswordEncoder passwordEncoder, AppUserRepository appUserRepository) {
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AppUser create(String createdBy, CreateUserForm form) {
        var passwordHash = passwordEncoder.encode(form.getPassword());
        var emailVerificationToken = UUID.randomUUID().toString();
        var newAppuser = AppUser.builder()
                .fullName(form.getFullName().trim())
                .email(form.getEmail().trim())
                .phone(form.getPhoneNumber() != null ? form.getPhoneNumber().trim() : null)
                .passwordHash(passwordHash)
                .emailVerificationToken(emailVerificationToken)
                .emailVerificationTokenExpiresAt(ZonedDateTime.now().plusHours(24))
                .build();
        newAppuser.setRole(form.getRole());
        appUserRepository.save(newAppuser);
        return newAppuser;
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppUser createUser(CreateUserForm form) throws AppException {
        if (form == null || !StringUtils.hasText(form.getEmail())) {
            throw new AppException("Email address is required.");
        }
        String normalizedEmail = form.getEmail().trim();
        if (appUserRepository.findFirstByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new AppException("A user with this email address already exists.");
        }
        try {
            return create(null, form);
        } catch (Exception e) {
            throw new AppException("Unable to create user: " + e.getMessage());
        }
    }

    @Override
    public AppUser getById(UUID id) throws AppException {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found."));
    }

    @Override
    public List<AppUser> getAllUsers() {
        return appUserRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Page<AppUser> searchUsers(String q, String role, String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? size : 10;

        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), pattern);
                Predicate emailMatch = cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch, phoneMatch));
            }

            if (StringUtils.hasText(role)) {
                try {
                    AppUserRole parsedRole = AppUserRole.valueOf(role.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("role"), parsedRole));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown role filter value
                }
            }

            if (StringUtils.hasText(status)) {
                String normalizedStatus = status.trim().toLowerCase();
                if ("active".equals(normalizedStatus)) {
                    predicates.add(cb.isTrue(root.get("isActive")));
                } else if ("suspended".equals(normalizedStatus)) {
                    predicates.add(cb.isFalse(root.get("isActive")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return appUserRepository.findAll(spec, pageRequest);
    }
}
