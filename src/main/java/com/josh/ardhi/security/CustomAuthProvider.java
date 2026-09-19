package com.josh.ardhi.security;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.AppSession;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import com.josh.ardhi.services.AuthService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class CustomAuthProvider implements AuthenticationProvider {

    final private AuthService authService;
    private final HttpServletResponse httpServletResponse;
    private final ServletRequest httpServletRequest;

    public CustomAuthProvider(AuthService authService, HttpServletResponse httpServletResponse, ServletRequest httpServletRequest) {
        this.authService = authService;
        this.httpServletResponse = httpServletResponse;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        System.out.println("Login attempt: "+username);

        if(password == null || password.trim().isEmpty()){
            throw new BadCredentialsException("Invalid username or password");
        }

        if(username == null  || username.trim().isEmpty()){
            throw new BadCredentialsException("Invalid username or password");
        }

        AppSession appSession;
        try {
            String userAgent = httpServletResponse.getHeader("User-Agent");
            String ipAddress = httpServletRequest.getRemoteAddr();
            appSession = authService.login(userAgent, ipAddress, username, password);
        } catch (AppException e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        AppUser user = appSession.getUser();
        Set<SimpleGrantedAuthority> roles = new HashSet<>();
        if(AppUserRole.ADMIN == user.getRole()){
            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if(AppUserRole.CLIENT == user.getRole()){
            roles.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }
        return new UsernamePasswordAuthenticationToken(appSession.getId().toString(),null, roles);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }
}
