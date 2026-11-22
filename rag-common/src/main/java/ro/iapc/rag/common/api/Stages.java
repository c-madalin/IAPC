package ro.iapc.rag.common.api;

/**
 * Per-request switches for optional pipeline stages. {@code null} means "use the service default". The eval harness
 * sets them explicitly to measure each stage with and without it.
 */
public record Stages(Boolean expand, Boolean rerank, Boolean stitch) {

    public static final Stages DEFAULTS = new Stages(null, null, null);

    public static Stages orDefaults(Stages stages) {
        return stages == null ? DEFAULTS : stages;
    }

    public boolean expandOr(boolean fallback) {
        return expand == null ? fallback : expand;
    }

    public boolean rerankOr(boolean fallback) {
        return rerank == null ? fallback : rerank;
    }

    public boolean stitchOr(boolean fallback) {
        return stitch == null ? fallback : stitch;
    }
}
