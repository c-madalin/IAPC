#!/usr/bin/env python3
"""Generates the Grafana dashboards in observability/grafana/dashboards. Edit here, run `make dashboards`, commit both.

Metric names follow Micrometer -> OTLP -> Prometheus translation (dots to underscores, unit suffix). The regex
matchers on __name__ tolerate the suffix differences between histogram flavours.
"""
import json
import pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "observability" / "grafana" / "dashboards"
PROM = {"type": "prometheus", "uid": "prometheus"}


def panel(pid, title, exprs, x, y, w=12, h=8, unit="short", kind="timeseries"):
    return {
        "id": pid,
        "type": kind,
        "title": title,
        "datasource": PROM,
        "gridPos": {"x": x, "y": y, "w": w, "h": h},
        "fieldConfig": {"defaults": {"unit": unit}, "overrides": []},
        "targets": [
            {"refId": chr(65 + i), "datasource": PROM, "expr": e, "legendFormat": legend}
            for i, (e, legend) in enumerate(exprs)
        ],
    }


def dashboard(uid, title, panels, variables):
    return {
        "uid": uid,
        "title": title,
        "schemaVersion": 39,
        "time": {"from": "now-30m", "to": "now"},
        "refresh": "10s",
        "tags": ["iapc"],
        "templating": {"list": variables},
        "panels": panels,
    }


job_var = {
    "name": "job",
    "type": "query",
    "datasource": PROM,
    "query": 'label_values({__name__=~"http_server_requests_seconds_count"}, job)',
    "includeAll": True,
    "multi": True,
    "current": {"text": "All", "value": "$__all"},
    "refresh": 2,
}

HTTP = '{__name__=~"http_server_requests_seconds_count", job=~"$job"}'
HTTP_BUCKET = '{__name__=~"http_server_requests_seconds_bucket", job=~"$job"}'

health = dashboard(
    "iapc-service-health",
    "IAPC service health",
    [
        panel(1, "Requests per second", [(f"sum by (job) (rate({HTTP}[1m]))", "{{job}}")], 0, 0, unit="reqps"),
        panel(
            2,
            "Error rate (5xx share)",
            [(
                f'sum by (job) (rate({{__name__=~"http_server_requests_seconds_count", job=~"$job", status=~"5.."}}[1m]))'
                f" / sum by (job) (rate({HTTP}[1m]))",
                "{{job}}",
            )],
            12, 0, unit="percentunit",
        ),
        panel(
            3,
            "Latency p95",
            [(f"histogram_quantile(0.95, sum by (le, job) (rate({HTTP_BUCKET}[5m])))", "{{job}}")],
            0, 8, unit="s",
        ),
        panel(
            4,
            "JVM heap used",
            [('sum by (job) (jvm_memory_used_bytes{job=~"$job", area="heap"})', "{{job}}")],
            12, 8, unit="bytes",
        ),
    ],
    [job_var],
)

STAGE_BUCKET = '{__name__=~"rag_stage_duration.*_bucket"}'
pipeline = dashboard(
    "iapc-rag-pipeline",
    "IAPC RAG pipeline",
    [
        panel(
            1,
            "Stage latency p95",
            [(f"histogram_quantile(0.95, sum by (le, stage) (rate({STAGE_BUCKET}[5m])))", "{{stage}}")],
            0, 0, unit="s",
        ),
        panel(
            2,
            "Questions per minute (/api/ask)",
            [('sum(rate({__name__=~"http_server_requests_seconds_count", job="rag-api", uri="/api/ask"}[1m])) * 60', "asks")],
            12, 0,
        ),
        panel(3, "Refusals per minute", [('sum(rate({__name__=~"rag_refusals.*"}[1m])) * 60', "refusals")], 0, 8),
        panel(
            4,
            "LLM tokens per minute",
            [('sum by (type) (rate({__name__=~"rag_llm_tokens.*"}[1m])) * 60', "{{type}}")],
            12, 8,
        ),
        panel(
            5,
            "Chunks ingested per minute",
            [('sum by (source_type) (rate({__name__=~"rag_ingest_chunks.*"}[1m])) * 60', "{{source_type}}")],
            0, 16,
        ),
        panel(
            6,
            "Ingestion failures per minute",
            [('sum by (source_type) (rate({__name__=~"rag_ingest_failures.*"}[1m])) * 60', "{{source_type}}")],
            12, 16,
        ),
    ],
    [],
)

OUT.mkdir(parents=True, exist_ok=True)
for d in (health, pipeline):
    (OUT / f"{d['uid']}.json").write_text(json.dumps(d, indent=2) + "\n")
    print("wrote", d["uid"])
