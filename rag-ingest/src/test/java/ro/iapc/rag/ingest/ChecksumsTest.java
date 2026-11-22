package ro.iapc.rag.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class ChecksumsTest {

    @Test
    void matchesTheKnownSha256AndAgreesBetweenBytesAndResource() {
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);

        assertThat(Checksums.sha256(abc))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .isEqualTo(Checksums.sha256(new ByteArrayResource(abc)));
    }

    @Test
    void failsClearlyOnAMissingResource() {
        assertThatThrownBy(() -> Checksums.sha256(new ClassPathResource("pdfs/missing.pdf")))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("missing.pdf");
    }
}
