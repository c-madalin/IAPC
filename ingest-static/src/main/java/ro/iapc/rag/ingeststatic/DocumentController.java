package ro.iapc.rag.ingeststatic;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ro.iapc.rag.store.ManifestEntry;
import ro.iapc.rag.store.ManifestRepository;

@RestController
public class DocumentController {

    private final PdfIngestionService ingestion;
    private final ManifestRepository manifest;

    public DocumentController(PdfIngestionService ingestion, ManifestRepository manifest) {
        this.ingestion = ingestion;
        this.manifest = manifest;
    }

    @PostMapping("/documents/sync")
    public List<ManifestEntry> sync() {
        return ingestion.syncAll();
    }

    @DeleteMapping("/documents/{docId}")
    public Map<String, Object> delete(@PathVariable String docId) {
        int removed = ingestion.delete(docId);
        if (removed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No document with doc_id " + docId);
        }
        return Map.of("docId", docId, "removedChunks", removed);
    }

    @GetMapping("/manifest")
    public List<ManifestEntry> manifest() {
        return manifest.findAll();
    }
}
