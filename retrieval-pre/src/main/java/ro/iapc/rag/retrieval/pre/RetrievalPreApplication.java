package ro.iapc.rag.retrieval.pre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RetrievalPreApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetrievalPreApplication.class, args);
    }
}
