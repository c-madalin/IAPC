package ro.iapc.rag.ingeststatic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IngestStaticApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestStaticApplication.class, args);
    }
}
