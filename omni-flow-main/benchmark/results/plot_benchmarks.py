#!/usr/bin/env python3
"""
Generate the P1-P5 performance graphs from the JMH CSV results.

Usage:
    python3 plot_benchmarks.py [jmh-results.csv] [output-dir]

Reads the JMH CSV (AverageTime mode, microseconds/op) and produces one PNG per
experiment. All measurements are LOCAL rendering/resolution (no cloud).
"""
import csv
import os
import sys
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

CSV = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "jmh-results.csv")
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.dirname(os.path.abspath(CSV))

# class short name -> (file, title, x-label, {method: series-label})
PLOTS = {
    "BenchmarkRenderingScalability": (
        "P1_rendering_scalability.png",
        "P1 — Tempo de renderização vs número de funções",
        "Número de funções (passos do workflow)",
        {"renderToAmazon": "AWS (ASL JSON)", "renderToGoogle": "GCP (YAML)"},
    ),
    "BenchmarkRenderingByParameterCount": (
        "P2_rendering_by_parameters.png",
        "P2 — Tempo de renderização vs número de parâmetros por função",
        "Número de parâmetros por função",
        {"renderToAmazon": "AWS (ASL JSON)", "renderToGoogle": "GCP (YAML)"},
    ),
    "BenchmarkInternalCallResolution": (
        "P3_resolution_overhead.png",
        "P3 — Custo da unificação: resolução de funções internas",
        "Número de chamadas no workflow",
        {"resolveAllInternal": "Internas (dispara resolução)",
         "resolveAllExternal": "Externas (sem resolução)"},
    ),
    "BenchmarkRenderingAwsVsGcp": (
        "P4_aws_vs_gcp.png",
        "P4 — Renderizador AWS vs GCP",
        "Número de funções (passos do workflow)",
        {"renderAslJson": "AWS (ASL JSON)", "renderGcpYaml": "GCP (YAML)"},
    ),
    "BenchmarkRenderingByNesting": (
        "P5_rendering_by_nesting.png",
        "P5 — Tempo de renderização vs profundidade de aninhamento",
        "Profundidade de aninhamento (parallel/iteration encadeados)",
        {"renderToAmazon": "AWS (ASL JSON)", "renderToGoogle": "GCP (YAML)"},
    ),
}
YLABEL = "Tempo médio (µs/op)"


def load(path):
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    param_cols = [c for c in rows[0].keys() if c.startswith("Param:")]
    score_col = next(c for c in rows[0].keys() if c.startswith("Score") and "Error" not in c)
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    # data[class][method] = list of (x, score, err)
    data = defaultdict(lambda: defaultdict(list))
    for r in rows:
        full = r["Benchmark"]                       # ...metrics.ClassName.method
        cls = full.split(".")[-2]
        method = full.split(".")[-1]
        x = None
        for pc in param_cols:
            v = (r.get(pc) or "").strip()
            if v:
                x = float(v)
                break
        if x is None:
            continue
        score = float(r[score_col])
        err = float(r[err_col]) if err_col and (r.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[cls][method].append((x, score, err))
    return data


def main():
    data = load(CSV)
    made = []
    for cls, (fname, title, xlabel, series) in PLOTS.items():
        if cls not in data:
            print(f"  [skip] no data for {cls}")
            continue
        plt.figure(figsize=(8, 5))
        for method, label in series.items():
            pts = sorted(data[cls].get(method, []))
            if not pts:
                continue
            xs = [p[0] for p in pts]
            ys = [p[1] for p in pts]
            es = [p[2] for p in pts]
            plt.errorbar(xs, ys, yerr=es, marker="o", capsize=3, label=label)
        plt.title(title)
        plt.xlabel(xlabel)
        plt.ylabel(YLABEL)
        plt.grid(True, alpha=0.3)
        plt.legend()
        plt.tight_layout()
        out = os.path.join(OUT, fname)
        plt.savefig(out, dpi=130)
        plt.close()
        made.append(out)
        print(f"  [ok] {out}")
    print(f"\nGenerated {len(made)} graphs in {OUT}")


if __name__ == "__main__":
    main()
