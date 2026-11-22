package ro.iapc.rag.platform;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds a {@link RestClient} for another service. By default calls go through Eureka ({@code http://<service-id>}).
 * A URL in {@code rag.clients.urls.<service-id>} bypasses Eureka; the {@code hostdev} profile uses that to reach
 * containers on localhost, because the names they register under don't resolve on the host.
 */
public class ServiceClients {

    private static final Logger log = LoggerFactory.getLogger(ServiceClients.class);

    private final RestClient.Builder loadBalanced;
    private final RestClient.Builder direct;
    private final Map<String, String> urlOverrides;

    public ServiceClients(
            RestClient.Builder loadBalanced, RestClient.Builder direct, Map<String, String> urlOverrides) {
        this.loadBalanced = loadBalanced;
        this.direct = direct;
        this.urlOverrides = urlOverrides == null ? Map.of() : urlOverrides;
    }

    public RestClient forService(String serviceId) {
        String override = urlOverrides.get(serviceId);
        if (override != null) {
            log.info("Calling {} directly at {}", serviceId, override);
            return direct.clone().baseUrl(override).build();
        }
        return loadBalanced.clone().baseUrl("http://" + serviceId).build();
    }
}
