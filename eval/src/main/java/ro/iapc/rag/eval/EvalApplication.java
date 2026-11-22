package ro.iapc.rag.eval;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/** Command-line eval harness. Run with {@code make eval}; it exits non-zero when any question errored. */
@SpringBootApplication
@EnableConfigurationProperties(EvalProperties.class)
public class EvalApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(EvalApplication.class, args)));
    }

    @Bean
    CommandLineRunner runEval(RestClient.Builder builder, EvalProperties props) {
        return args -> {
            EvalReport report = new EvalRunner(builder.baseUrl(props.baseUrl()).build(), props).run();
            if (report.metrics().getOrDefault("errors", 0.0) > 0) {
                throw new IllegalStateException(report.metrics().get("errors").intValue() + " questions failed");
            }
        };
    }
}
