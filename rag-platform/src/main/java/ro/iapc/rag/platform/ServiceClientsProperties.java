package ro.iapc.rag.platform;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param urls service id to base URL, for calls that must skip Eureka */
@ConfigurationProperties("rag.clients")
public record ServiceClientsProperties(Map<String, String> urls) {}
