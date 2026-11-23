package ro.iapc.rag.ingeststatic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param resources Spring resource pattern for the PDFs, e.g. {@code file:./resources/*.pdf}
 * @param syncOnStartup ingest everything under {@code resources} once the service is up
 * @param chunkSize target chunk size in words
 * @param chunkOverlap number of words repeated between adjacent chunks
 */
@ConfigurationProperties("rag.ingest")
public record IngestProperties(String resources, boolean syncOnStartup, int chunkSize, int chunkOverlap) {

    public IngestProperties {
        if (resources == null) {
            resources = "file:./resources/*.pdf";
        }
        if (chunkSize <= 0) {
            chunkSize = 400;
        }
        if (chunkOverlap < 0) {
            chunkOverlap = 0;
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be smaller than chunkSize");
        }
    }
}
