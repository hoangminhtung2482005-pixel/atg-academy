package com.example.demo.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class GoogleUserAuthenticationToken extends AbstractAuthenticationToken {

    private final GoogleUserPrincipal principal;

    public GoogleUserAuthenticationToken(GoogleUserPrincipal principal,
                                         Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.email();
    }
}
