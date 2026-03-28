package ru.blps.lab_1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.jaas.JaasAuthenticationCallbackHandler;
import org.springframework.security.authentication.jaas.JaasNameCallbackHandler;
import org.springframework.security.authentication.jaas.JaasPasswordCallbackHandler;
import org.springframework.security.web.SecurityFilterChain;
import ru.blps.lab_1.security.jaas.BootClasspathJaasAuthenticationProvider;
import ru.blps.lab_1.security.jaas.RoleToPrivilegesAuthorityGranter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BootClasspathJaasAuthenticationProvider jaasAuthenticationProvider() throws Exception {
        ClassPathResource jaasClasspath = new ClassPathResource("security/jaas.conf");
        Path tempJaas = Files.createTempFile("lab1-jaas-", ".conf");
        tempJaas.toFile().deleteOnExit();
        try (InputStream in = jaasClasspath.getInputStream()) {
            Files.copy(in, tempJaas, StandardCopyOption.REPLACE_EXISTING);
        }
        BootClasspathJaasAuthenticationProvider provider = new BootClasspathJaasAuthenticationProvider();
        provider.setLoginContextName("BlpsLab");
        provider.setLoginConfig(new FileSystemResource(tempJaas.toFile()));
        provider.setAuthorityGranters(new RoleToPrivilegesAuthorityGranter[] { new RoleToPrivilegesAuthorityGranter() });
        provider.setCallbackHandlers(new JaasAuthenticationCallbackHandler[] {
            new JaasNameCallbackHandler(),
            new JaasPasswordCallbackHandler()
        });
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(BootClasspathJaasAuthenticationProvider jaasAuthenticationProvider) {
        return new ProviderManager(jaasAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationManager authenticationManager
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .authenticationManager(authenticationManager);
        return http.build();
    }
}
