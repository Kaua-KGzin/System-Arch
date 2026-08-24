package dev.kauakgzin.archhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kauakgzin.archhub.web.ApiTokenFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({HubProperties.class, HubSecurityProperties.class, HubPersistenceProperties.class})
public class HubConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public FilterRegistrationBean<ApiTokenFilter> apiTokenFilter(HubSecurityProperties properties, ObjectMapper objectMapper) {
        FilterRegistrationBean<ApiTokenFilter> registration = new FilterRegistrationBean<>(
                new ApiTokenFilter(properties, objectMapper));
        registration.addUrlPatterns("/api/v1/systems/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public RestClient healthCheckRestClient(HubProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.timeout())
                .withReadTimeout(properties.timeout());

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
