package com.josh.ardhi.security;

import com.josh.ardhi.models.AppSession;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.repositories.AppSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.UUID;

@Configuration
public class SecurityConfig {

    private CustomAuthProvider authProvider;

    public SecurityConfig(CustomAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AppSessionRepository appSessionRepository) throws Exception {
        return http
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                    "/",
                                    "/error",
                                    "/js/**",
                                    "/css/**",
                                    "/forgot-password",
                                    "/process-forgot-password",
                                    "/reset-password/*",
                                    "/process-reset-password",
                                    "/client-signup",
                                    "/process-client-signup",
                                    "/verify-email",
                                    "/process-verify-email",
                                    "/resend-email-verification-token",
                                    "/contact-us",
                                    "/process-contact-us",
                                    "/terms",
                                    "/privacy-policy"
                            ).permitAll()
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .requestMatchers("/client/**").hasRole("CLIENT")
                            .anyRequest().authenticated();

                }).formLogin(formLogin -> {
                    formLogin.loginPage("/login")
                            .loginProcessingUrl("/process-login")
                            .failureUrl("/login?error")
                            .successHandler(customSuccessHandler(appSessionRepository))
                            .permitAll();
                }).logout(logout -> {
                    logout.logoutSuccessUrl("/login?logout")
                            .logoutUrl("/logout")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll();
                }).headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler(AppSessionRepository appSessionRepository) {
        return (request, response, authentication) -> {

            String sessionId = authentication.getName();
            AppSession appSession = appSessionRepository.findById(UUID.fromString(sessionId)).get();
            AppUser appUser = appSession.getUser();
            if (!appUser.isEmailVerified()) {
                response.sendRedirect("/verify-email");
                return;
            }

            var roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin");
            } else if (roles.contains("ROLE_CLIENT")) {
                response.sendRedirect("/client");
            } else {
                response.sendRedirect("/");
            }
        };
    }


}
