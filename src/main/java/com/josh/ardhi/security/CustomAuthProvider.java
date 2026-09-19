package com.josh.ardhi.security;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.services.AuthService;
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

    public CustomAuthProvider(AuthService authService) {
        this.authService = authService;
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

        AppUser user;
        try {
            user = authService.login(username, password);
        } catch (AppException e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Set<SimpleGrantedAuthority> roles = new HashSet<>();
        if(user.isAdmin()){
            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if(user.isClient()){
            roles.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }
        return new UsernamePasswordAuthenticationToken(user.getId().toString(),null, roles);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }
}
