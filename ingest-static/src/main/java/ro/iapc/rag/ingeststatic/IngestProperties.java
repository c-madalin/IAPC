package ro.iapc.rag.ingeststatic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param resources Spring resource pattern for the PDFs, e.g. {@code file:./resources/*.pdf}
 * @param syncOnStartup ingest everything under {@code resources} once the service is up
 * @param chunkSize target chunk size in tokens
 */
@ConfigurationProperties("rag.ingest")
public record IngestProperties(String resources, boolean syncOnStartup, int chunkSize) {

    public IngestProperties {
        if (resources == null) {
            resources = "file:./resources/*.pdf";
        }
        if (chunkSize <= 0) {
            chunkSize = 400;
        }
    }
}
