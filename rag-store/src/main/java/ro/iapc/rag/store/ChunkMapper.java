package ro.iapc.rag.store;

import java.util.Map;
import org.springframework.ai.document.Document;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.common.api.Chunk;

/** Converts between Spring AI documents and the {@link Chunk} record services exchange. */
public final class ChunkMapper {

    private ChunkMapper() {}

    public static Chunk toChunk(Document doc) {
        Map<String, Object> m = doc.getMetadata();
        return new Chunk(
                doc.getId(),
                string(m.get(MetadataKeys.DOC_ID)),
                string(m.get(MetadataKeys.VERSION)),
                string(m.get(MetadataKeys.SOURCE)),
                integer(m.get(MetadataKeys.PAGE)),
                integer(m.get(MetadataKeys.CHUNK_INDEX)),
                doc.getText(),
                doc.getScore());
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer integer(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return value == null ? null : Integer.valueOf(value.toString());
    }
}
