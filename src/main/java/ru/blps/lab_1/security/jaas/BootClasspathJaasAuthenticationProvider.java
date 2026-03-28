package ru.blps.lab_1.security.jaas;

import org.springframework.security.authentication.jaas.JaasAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public final class BootClasspathJaasAuthenticationProvider extends JaasAuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader appLoader = XmlLoginModule.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(appLoader);
            return super.authenticate(authentication);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
