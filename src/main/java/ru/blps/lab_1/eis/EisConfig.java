package ru.blps.lab_1.eis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EisConfig {

    @Bean
    public OnlyOfficeManagedConnectionFactory onlyOfficeManagedConnectionFactory(
        @Value("${eis.nextcloud.webdav-url:http://nginx/cloud/remote.php/dav/files/admin}") String webdavUrl,
        @Value("${eis.nextcloud.user:admin}") String user,
        @Value("${eis.nextcloud.password:admin}") String password
    ) {
        return new OnlyOfficeManagedConnectionFactory(webdavUrl, user, password);
    }

    @Bean
    public OnlyOfficeConnectionFactory onlyOfficeConnectionFactory(OnlyOfficeManagedConnectionFactory mcf) {
        return (OnlyOfficeConnectionFactory) mcf.createConnectionFactory();
    }
}
