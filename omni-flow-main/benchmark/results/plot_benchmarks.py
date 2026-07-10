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


def load_p7(path):
    """P7 has two @Param dimensions (n, m) and two methods (uncached/cached)."""
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


def _plot_p7_one(data, method, ms, ns, style, title, fname, out_dir, ylim):
    """Draw a single P7 strategy (uncached or cached): total resolution time vs N,
    one curve per registry size M, shared log-y range so the two figures are
    directly comparable."""
    colours = plt.cm.viridis([i / max(1, len(ms) - 1) for i in range(len(ms))])
    plt.figure(figsize=(8, 5))
    for i, m in enumerate(ms):
        pts = [(n, data[method][(m, n)][0]) for n in ns]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker=style["marker"], linestyle=style["linestyle"],
                 color=colours[i], label=f"M={m}")
    plt.yscale("log")
    plt.ylim(*ylim)
    plt.title(title)
    plt.xlabel("Número de chamadas internas (N)")
    plt.ylabel("Tempo total (µs/op, escala log)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_p7(csv_path, out_dir):
    """P7 - before/after of the registry-read optimization, as two separate figures
    on a shared log-y axis: uncached (per-call read, O(N*M)) vs cached (read once,
    O(N+M)); one curve per registry size M."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no P7 csv at {csv_path}")
        return None
    data = load_p7(csv_path)
    if not data.get("resolveNaive"):
        return None
    ms = sorted({m for (m, _) in data["resolveNaive"]})
    ns = sorted({n for (_, n) in data["resolveNaive"]})
    # shared log-y range across both strategies, with a small margin
    scores = [s for meth in ("resolveNaive", "resolveOptimized")
              for (s, _err) in data[meth].values()]
    ylim = (min(scores) / 1.5, max(scores) * 1.5)
    uncached = _plot_p7_one(
        data, "resolveNaive", ms, ns,
        {"marker": "o", "linestyle": "--"},
        "P7a — Resolução sem cache (leitura por chamada, Θ(N·M))",
        "P7a_resolution_sem_cache.png", out_dir, ylim)
    cached = _plot_p7_one(
        data, "resolveOptimized", ms, ns,
        {"marker": "s", "linestyle": "-"},
        "P7b — Resolução com cache (leitura única, Θ(N+M))",
        "P7b_resolution_com_cache.png", out_dir, ylim)
    return [uncached, cached]


_PIPELINE_SERIES = {
    "resolveNaive": "Sem cache (leitura por chamada, Θ(N·M))",
    "resolveOptimized": "Com cache (leitura única, Θ(N+M))",
}


def _plot_pipeline(csv_path, out_dir, cls, title, xlabel, fname, series=None):
    """P9 - workflow resolution, one CSV with a single @Param. Reuses the generic
    load(); log-y because uncached (Θ(N·M)) vs cached (Θ(N+M)) spans ~2 orders of
    magnitude. `series` maps @Benchmark method -> legend label."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no csv at {csv_path}")
        return None
    data = load(csv_path)
    if cls not in data:
        print(f"  [skip] no data for {cls}")
        return None
    series = series or _PIPELINE_SERIES
    plt.figure(figsize=(8, 5))
    for method, label in series.items():
        pts = sorted(data[cls].get(method, []))
        if not pts:
            continue
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        es = [p[2] for p in pts]
        plt.errorbar(xs, ys, yerr=es, marker="o", capsize=3, label=label)
    plt.yscale("log")
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel("Tempo médio (µs/op, escala log)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend()
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def load_p8(path):
    """P8 has two @Param dimensions (f, n) and two methods (uncached/cached)."""
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = defaultdict(dict)  # data[method][(f, n)] = (score, err)
    for r in rows:
        if r["Benchmark"].split(".")[-2] != "BenchmarkResolveWorkflowByFunctionsAndCalls":
            continue
        method = r["Benchmark"].split(".")[-1]
        f = int(float(r["Param: f"]))
        n = int(float(r["Param: n"]))
        score = float(r["Score"])
        err = float(r[err_col]) if err_col and (r.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[method][(f, n)] = (score, err)
    return data


def _plot_p8_one(data, method, fs, ns, style, title, fname, out_dir, ylim):
    """Draw a single P8 strategy (uncached or cached): total resolution time vs N,
    one curve per number of distinct functions F, shared log-y range so the two
    figures are directly comparable."""
    colours = plt.cm.viridis([i / max(1, len(fs) - 1) for i in range(len(fs))])
    plt.figure(figsize=(8, 5))
    for i, f in enumerate(fs):
        pts = [(n, data[method][(f, n)][0]) for n in ns if (f, n) in data[method]]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker=style["marker"], linestyle=style["linestyle"],
                 color=colours[i], label=f"F={f}")
    plt.yscale("log")
    plt.ylim(*ylim)
    plt.title(title)
    plt.xlabel("Número de chamadas no workflow (N)")
    plt.ylabel("Tempo total (µs/op, escala log)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_p8(csv_path, out_dir):
    """P8 - workflow resolution vs its two axes (F distinct functions x N calls,
    with registry M=F), as two figures on a shared log-y axis: uncached (per-call
    read, O(N*M)) vs cached (read once, O(N+M)); one curve per F."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no P8 csv at {csv_path}")
        return None
    data = load_p8(csv_path)
    if not data.get("resolveNaive"):
        return None
    fs = sorted({f for (f, _) in data["resolveNaive"]})
    ns = sorted({n for (_, n) in data["resolveNaive"]})
    scores = [s for meth in ("resolveNaive", "resolveOptimized")
              for (s, _err) in data[meth].values()]
    ylim = (min(scores) / 1.5, max(scores) * 1.5)
    uncached = _plot_p8_one(
        data, "resolveNaive", fs, ns,
        {"marker": "o", "linestyle": "--"},
        "P8a — Resolução sem cache (leitura por chamada, Θ(N·M)) — M=F",
        "P8a_resolution_sem_cache.png", out_dir, ylim)
    cached = _plot_p8_one(
        data, "resolveOptimized", fs, ns,
        {"marker": "s", "linestyle": "-"},
        "P8b — Resolução com cache (leitura única, Θ(N+M)) — M=F",
        "P8b_resolution_com_cache.png", out_dir, ylim)
    return [uncached, cached]


def plot_p9(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkResolveWorkflowByRegistrySize",
        "P9 — Resolução vs tamanho do registo (M) — F=10/N=50 fixos",
        "Nº de funções no registo (M)",
        "P9_resolution_by_registry.png")


def _load_single_method(csv_path, cls):
    """Generic loader for CSVs with two @Param dims (f, n) and a single @Benchmark method."""
    rows = list(csv.DictReader(open(csv_path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = {}  # data[(f, n)] = (score, err)
    for r in rows:
        if r["Benchmark"].split(".")[-2] != cls:
            continue
        f = int(float(r["Param: f"]))
        n = int(float(r["Param: n"]))
        score = float(r["Score"])
        err = float(r[err_col]) if err_col and (r.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[(f, n)] = (score, err)
    return data


def _plot_internal_resolution(csv_path, out_dir, cls, title, fname):
    """P10/P11 - cost of a REAL auto-deploy resolver (not a benchmark-only stand-in like
    NaiveEndpointResolver) vs N calls x M registry size (M=F), one curve per F. Unlike P8/P9
    there is only one strategy: neither resolver was ever fixed with the single-read
    optimization, so this measures the actual production cost of the unification glue."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no csv at {csv_path}")
        return None
    data = _load_single_method(csv_path, cls)
    if not data:
        print(f"  [skip] no data for {cls}")
        return None
    fs = sorted({f for (f, _) in data})
    ns = sorted({n for (_, n) in data})
    colours = plt.cm.viridis([i / max(1, len(fs) - 1) for i in range(len(fs))])
    plt.figure(figsize=(8, 5))
    for i, f in enumerate(fs):
        pts = [(n, data[(f, n)][0]) for n in ns if (f, n) in data]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker="o", linestyle="--", color=colours[i], label=f"F={f}")
    plt.yscale("log")
    plt.title(title)
    plt.xlabel("Número de chamadas internas (N)")
    plt.ylabel("Tempo total (µs/op, escala log)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_p10(csv_path, out_dir):
    return _plot_internal_resolution(
        csv_path, out_dir, "BenchmarkAwsInternalFunctionResolution",
        "P10 — Resolução real AWS (AwsInternalFunctionResolver) — M=F",
        "P10_aws_internal_resolution.png")


def plot_p11(csv_path, out_dir):
    return _plot_internal_resolution(
        csv_path, out_dir, "BenchmarkGoogleInternalFunctionResolution",
        "P11 — Resolução real GCP (WorkflowInternalFunctionResolver) — M=F",
        "P11_google_internal_resolution.png")


_BRANCH_WIDTH_SERIES = {
    "renderChoiceToAmazon": "AWS Choice",
    "renderChoiceToGoogle": "GCP Choice",
    "renderParallelToAmazon": "AWS Parallel",
    "renderParallelToGoogle": "GCP Parallel",
}


def plot_p12(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkRenderingByBranchWidth",
        "P12 — Tempo de renderização vs largura de Choice/Parallel",
        "Nº de condições (Choice) / branches (Parallel)",
        "P12_rendering_by_branch_width.png",
        series=_BRANCH_WIDTH_SERIES)


def _load_registry_write(csv_path):
    """P13 has two @Param dimensions (m0, k) and a single method (putKSequential)."""
    rows = list(csv.DictReader(open(csv_path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = {}  # data[(m0, k)] = (score, err)
    for r in rows:
        if r["Benchmark"].split(".")[-2] != "BenchmarkRegistryWriteScaling":
            continue
        m0 = int(float(r["Param: m0"]))
        k = int(float(r["Param: k"]))
        score = float(r["Score"])
        err = float(r[err_col]) if err_col and (r.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[(m0, k)] = (score, err)
    return data


_KEY_MATCH_SERIES = {
    "resolveExactMatch": "Exact match (O(1)/lookup)",
    "resolveSuffixMatch": "Suffix match, region-qualified (O(M)/lookup)",
}


def plot_p14(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkResolutionKeyMatchStrategy",
        "P14 — Exact vs. suffix match no resolver \"com cache\" — F=10/N=50 fixos",
        "Nº de funções no registo (M)",
        "P14_resolution_key_match_strategy.png",
        series=_KEY_MATCH_SERIES)


def plot_p13(csv_path, out_dir):
    """P13 - cost of K sequential FunctionRegistryStore.put() calls starting from a registry of
    M0 entries, one curve per M0. Complements P6-P11 (read side) with the write side: put()
    re-reads and rewrites the whole file per call, so K registrations cost Theta(K*M0 + K^2)."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no P13 csv at {csv_path}")
        return None
    data = _load_registry_write(csv_path)
    if not data:
        print("  [skip] no data for BenchmarkRegistryWriteScaling")
        return None
    m0s = sorted({m0 for (m0, _) in data})
    ks = sorted({k for (_, k) in data})
    colours = plt.cm.viridis([i / max(1, len(m0s) - 1) for i in range(len(m0s))])
    plt.figure(figsize=(8, 5))
    for i, m0 in enumerate(m0s):
        pts = [(k, data[(m0, k)][0]) for k in ks if (m0, k) in data]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker="o", linestyle="--", color=colours[i], label=f"M0={m0}")
    plt.yscale("log")
    plt.xscale("log")
    plt.title("P13 — Custo de escrita incremental no registo (put) — M0 × K")
    plt.xlabel("Nº de escritas sucessivas (K, escala log)")
    plt.ylabel("Tempo total (µs/op, escala log)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, "P13_registry_write_scaling.png")
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
        made.extend(p7_out)

    p8_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p8.csv")
    p8_out = plot_p8(p8_csv, OUT)
    if p8_out:
        made.extend(p8_out)

    p9_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p9.csv")
    p9_out = plot_p9(p9_csv, OUT)
    if p9_out:
        made.append(p9_out)

    p10_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p10.csv")
    p10_out = plot_p10(p10_csv, OUT)
    if p10_out:
        made.append(p10_out)

    p11_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p11.csv")
    p11_out = plot_p11(p11_csv, OUT)
    if p11_out:
        made.append(p11_out)

    p12_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p12.csv")
    p12_out = plot_p12(p12_csv, OUT)
    if p12_out:
        made.append(p12_out)

    p13_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p13.csv")
    p13_out = plot_p13(p13_csv, OUT)
    if p13_out:
        made.append(p13_out)

    p14_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-p14.csv")
    p14_out = plot_p14(p14_csv, OUT)
    if p14_out:
        made.append(p14_out)

    s2_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "artifact-size.csv")
    s2_out = plot_artifact_size(s2_csv, OUT)
    if s2_out:
        made.append(s2_out)

    print(f"\nGenerated {len(made)} graphs in {OUT}")


if __name__ == "__main__":
    main()
