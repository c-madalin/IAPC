package ro.iapc.rag.platform;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestClient;

/**
 * Wiring every service gets by depending on rag-platform. Defaults live in {@code rag-platform-defaults.properties};
 * a service's own application.yaml overrides them.
 */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@PropertySource("classpath:rag-platform-defaults.properties")
@EnableConfigurationProperties(ServiceClientsProperties.class)
public class RagPlatformAutoConfiguration {

    /**
     * Eureka-aware clients for service-to-service calls. The load-balancing interceptor is added to a private builder
     * instead of publishing a {@code @LoadBalanced RestClient.Builder} bean: Eureka's own HTTP client picks up any
     * {@code RestClient.Builder} bean and would then try to look up the Eureka server through Eureka.
     */
    @Bean
    @ConditionalOnMissingBean
    ServiceClients serviceClients(
            RestClientBuilderConfigurer configurer,
            ObjectProvider<LoadBalancerClient> loadBalancerClient,
            ServiceClientsProperties properties) {
        RestClient.Builder loadBalanced = configurer.configure(RestClient.builder());
        loadBalancerClient.ifAvailable(lb -> loadBalanced.requestInterceptor(new LoadBalancerInterceptor(lb)));
        return new ServiceClients(loadBalanced, configurer.configure(RestClient.builder()), properties.urls());
    }

    @Bean
    @ConditionalOnMissingBean
    StageTimer stageTimer(MeterRegistry meterRegistry) {
        return new StageTimer(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    ApiExceptionHandler apiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OpenTelemetryAppender.class)
    static class OtelLogs {

        /** Connects the OTEL appender in logback-spring.xml to the SDK, so logs reach Loki with trace ids. */
        @Bean
        InitializingBean otelLogbackInstaller(ObjectProvider<OpenTelemetry> openTelemetry) {
            return () -> openTelemetry.ifAvailable(OpenTelemetryAppender::install);
        }
    }
}
