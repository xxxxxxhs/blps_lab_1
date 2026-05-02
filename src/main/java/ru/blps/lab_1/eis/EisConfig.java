package ru.blps.lab_1.eis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class EisConfig {

    @Bean
    public OnlyOfficeManagedConnectionFactory onlyOfficeManagedConnectionFactory(
        @Value("${eis.onlyoffice.storage-dir:/app/reports}") String storageDir,
        @Value("${eis.onlyoffice.public-base-url:http://nginx}") String publicBaseUrl
    ) {
        Path dir = Paths.get(storageDir);
        return new OnlyOfficeManagedConnectionFactory(dir, publicBaseUrl);
    }

    @Bean
    public OnlyOfficeConnectionFactory onlyOfficeConnectionFactory(OnlyOfficeManagedConnectionFactory mcf) {
        return (OnlyOfficeConnectionFactory) mcf.createConnectionFactory();
    }
}
