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
        "P2 — Tempo de renderização vs número de inputs da função",
        "Número de inputs da função (argumentos passados na chamada)",
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


def load_p6(path):
    """P6 has two @Param dimensions (n, m); load raw (m, n, method) -> (score, err)."""
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    data = defaultdict(dict)  # data[method][(m, n)] = (score, err)
    for r in rows:
        full = r["Benchmark"]
        method = full.split(".")[-1]
        m = int(float(r["Param: m"]))
        n = int(float(r["Param: n"]))
        score = float(r["Score"])
        err_col = next((c for c in rows[0].keys() if "Error" in c), None)
        err = float(r[err_col]) if err_col and (r.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[method][(m, n)] = (score, err)
    return data


def plot_p6(csv_path, out_dir):
    """P6 - per-call registry resolution cost vs registry size (m), one line per
    n. Plotting T(n,m)/n (cost PER resolveUrl call) makes the n-curves collapse
    onto a single curve, which is the direct visual evidence that resolution
    cost factors as n * f(m) - i.e. the O(N*M) mechanism from a per-call,
    unbounded registry re-read/re-parse (no cache)."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no P6 csv at {csv_path}")
        return None
    data = load_p6(csv_path)
    ns = sorted({n for (_, n) in data.get("resolveAllInternal", {})})
    plt.figure(figsize=(8, 5))
    for n in ns:
        pts = sorted((m, score / n) for (m, nn), (score, _err) in data["resolveAllInternal"].items() if nn == n)
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        plt.plot(xs, ys, marker="o", label=f"internal, n={n}")
    # external control: per-call cost should stay ~0 regardless of m
    ext_ns = sorted({n for (_, n) in data.get("resolveAllExternal", {})})
    if ext_ns:
        max_n = max(ext_ns)
        pts = sorted((m, score / max_n) for (m, nn), (score, _err) in data["resolveAllExternal"].items() if nn == max_n)
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        plt.plot(xs, ys, marker="s", linestyle="--", color="gray", label=f"external, n={max_n} (control)")
    plt.xscale("log")
    plt.title("P6 — Custo por chamada de resolveUrl() vs tamanho do registo (m)")
    plt.xlabel("Funções no registo (m, escala log)")
    plt.ylabel("Tempo por chamada (µs/op ÷ n)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend()
    plt.tight_layout()
    out = os.path.join(out_dir, "P6_registry_scaling.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_p8(csv_path, out_dir):
    """P8 - rendering cost of an internal (lambda:invoke) call vs an external
    (apigateway:invoke) call, at scale. Own CSV (single Param: n), reuses the
    generic load() used by P1/P4."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no P8 csv at {csv_path}")
        return None
    data = load(csv_path)
    cls = "BenchmarkRenderingLambdaVsApiGateway"
    if cls not in data:
        return None
    plt.figure(figsize=(8, 5))
    series = {"renderApiGateway": "Externa (apigateway:invoke)", "renderLambda": "Interna (lambda:invoke)"}
    for method, label in series.items():
        pts = sorted(data[cls].get(method, []))
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        es = [p[2] for p in pts]
        plt.errorbar(xs, ys, yerr=es, marker="o", capsize=3, label=label)
    plt.title("P8 — Renderização: interna (lambda:invoke) vs externa (apigateway:invoke)")
    plt.xlabel("Número de funções (passos do workflow)")
    plt.ylabel(YLABEL)
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    out = os.path.join(out_dir, "P8_lambda_vs_apigateway.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def load_p7(path):
    """P7 has two @Param dimensions (n, m) and two methods (naive/optimized)."""
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = defaultdict(dict)  # data[method][(m, n)] = (score, err)
    for r in rows:
        if r["Benchmark"].split(".")[-2] != "BenchmarkResolutionOptimization":
            continue
        method = r["Benchmark"].split(".")[-1]
        m = int(float(r["Param: m"]))
        n = int(float(r["Param: n"]))
        score = float(r["Score"])
        err = float(r[err_col]) if err_col and (r.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[method][(m, n)] = (score, err)
    return data


def plot_p7(csv_path, out_dir):
    """P7 - before/after of the registry-read optimization. Total resolution time
    vs N, dashed = naive (per-call read, O(N*M)), solid = optimized (read once,
    O(N+M)); one colour per registry size M. Log-y makes the ~200x gap legible."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no P7 csv at {csv_path}")
        return None
    data = load_p7(csv_path)
    if not data.get("resolveNaive"):
        return None
    ms = sorted({m for (m, _) in data["resolveNaive"]})
    ns = sorted({n for (_, n) in data["resolveNaive"]})
    colours = plt.cm.viridis([i / max(1, len(ms) - 1) for i in range(len(ms))])
    plt.figure(figsize=(8, 5))
    for i, m in enumerate(ms):
        nav = [(n, data["resolveNaive"][(m, n)][0]) for n in ns]
        opt = [(n, data["resolveOptimized"][(m, n)][0]) for n in ns]
        plt.plot([x for x, _ in nav], [y for _, y in nav],
                 marker="o", linestyle="--", color=colours[i], label=f"antes O(N·M), M={m}")
        plt.plot([x for x, _ in opt], [y for _, y in opt],
                 marker="s", linestyle="-", color=colours[i], label=f"depois O(N+M), M={m}")
    plt.yscale("log")
    plt.title("P7 — Resolução: antes (leitura por chamada) vs depois (leitura única)")
    plt.xlabel("Número de chamadas internas (N)")
    plt.ylabel("Tempo total (µs/op, escala log)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8, ncol=2)
    plt.tight_layout()
    out = os.path.join(out_dir, "P7_resolution_optimization.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_artifact_size(csv_path, out_dir):
    """S2 - size in KB of the rendered workflow artifact (ASL JSON vs GCP YAML) vs N."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no artifact-size csv at {csv_path}")
        return None
    rows = list(csv.DictReader(open(csv_path, encoding="utf-8")))
    ns = [int(r["n"]) for r in rows]
    aws = [int(r["aws_bytes"]) / 1024 for r in rows]
    gcp = [int(r["gcp_bytes"]) / 1024 for r in rows]
    plt.figure(figsize=(8, 5))
    plt.plot(ns, aws, marker="o", label="AWS (ASL JSON)")
    plt.plot(ns, gcp, marker="s", label="GCP (YAML)")
    plt.title("S2 — Tamanho do artefacto renderizado vs número de funções")
    plt.xlabel("Número de funções (passos do workflow)")
    plt.ylabel("Tamanho do artefacto (KB)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    out = os.path.join(out_dir, "S2_artifact_size.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


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

    p6_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p6.csv")
    p6_out = plot_p6(p6_csv, OUT)
    if p6_out:
        made.append(p6_out)

    p7_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p7.csv")
    p7_out = plot_p7(p7_csv, OUT)
    if p7_out:
        made.append(p7_out)

    p8_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p8.csv")
    p8_out = plot_p8(p8_csv, OUT)
    if p8_out:
        made.append(p8_out)

    s2_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "artifact-size.csv")
    s2_out = plot_artifact_size(s2_csv, OUT)
    if s2_out:
        made.append(s2_out)

    print(f"\nGenerated {len(made)} graphs in {OUT}")


if __name__ == "__main__":
    main()
