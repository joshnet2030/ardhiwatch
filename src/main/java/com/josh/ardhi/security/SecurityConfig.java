package com.josh.ardhi.security;

import jakarta.servlet.http.HttpFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    private CustomAuthProvider authProvider;

    public SecurityConfig(CustomAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                    "/",
                                    "/error",
                                    "/public/**",
                                    "/forgot-password",
                                    "/process-forgot-password",
                                    "/reset-password/*",
                                    "/process-reset-password",
                                    "/client-signup",
                                    "/process-client-signup"
                            ).permitAll()
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .requestMatchers("/client/**").hasRole("CLIENT")
                            .anyRequest().authenticated();

                }).formLogin(formLogin -> {
                    formLogin.loginPage("/login")
                            .loginProcessingUrl("/process-login")
                            .failureUrl("/login?error")
                            .successHandler(customSuccessHandler())
                            .permitAll();
                }).logout(logout -> {
                    logout.logoutSuccessUrl("/login?logout")
                            .logoutUrl("/logout")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll();
                }).build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            var roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard");
            } else if (roles.contains("ROLE_CLIENT")) {
                response.sendRedirect("/client/dashboard");
            } else {
                response.sendRedirect("/home");
            }
        };
    }


}
