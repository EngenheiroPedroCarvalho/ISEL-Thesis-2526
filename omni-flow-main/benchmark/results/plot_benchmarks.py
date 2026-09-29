#!/usr/bin/env python3
"""
Generate the T1-T18 performance graphs from the JMH CSV results.

Usage:
    python3 plot_benchmarks.py [jmh-results-f3.csv] [output-dir]

Reads the JMH CSV (AverageTime mode, microseconds/op) and produces one PNG per
experiment. All measurements are LOCAL rendering/resolution (no cloud).

The default CSV is jmh-results-f3.csv, the three-fork run the dissertation
reports. It does not cover the rendering experiments (T15-T17), which are only
in the older single-fork jmh-results.csv; those classes fall back to that file.
"""
import csv
import os
import sys
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

CSV = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "jmh-results-f3.csv")
FALLBACK_CSV = "jmh-results.csv"   # holds T15-T17, which the three-fork run does not
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.dirname(os.path.abspath(CSV))

# class short name -> (file, title, x-label, {method: series-label})
PLOTS = {
    "BenchmarkRenderingScalability": (
        "T15_rendering_scalability.png",
        "T15 — Rendering time vs number of functions",
        "Number of functions (workflow steps)",
        {"renderToAmazon": "AWS (ASL JSON)", "renderToGoogle": "GCP (YAML)"},
    ),
    "BenchmarkRenderingByParameterCount": (
        "T16_rendering_by_parameters.png",
        "T16 — Rendering time vs number of function inputs",
        "Number of function inputs (arguments passed in the call)",
        {"renderToAmazon": "AWS (ASL JSON)", "renderToGoogle": "GCP (YAML)"},
    ),
    "BenchmarkInternalCallResolution": (
        "T1_resolution_overhead.png",
        "T1 — Cost of unification: resolving internal functions",
        "Number of calls in the workflow",
        {"resolveAllInternal": "Internal (triggers resolution)",
         "resolveAllExternal": "External (no resolution)"},
    ),
    "BenchmarkRenderingByNesting": (
        "T17_rendering_by_nesting.png",
        "T17 — Rendering time vs nesting depth",
        "Nesting depth (chained parallel/iteration)",
        {"renderToAmazon": "AWS (ASL JSON)", "renderToGoogle": "GCP (YAML)"},
    ),
}
YLABEL = "Mean time (µs/op)"


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


def load_t2(path):
    """T2 has two @Param dimensions (n, r); load raw (r, n, method) -> (score, err)."""
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    data = defaultdict(dict)  # data[method][(r, n)] = (score, err)
    for row in rows:
        full = row["Benchmark"]
        method = full.split(".")[-1]
        r = int(float(row["Param: r"]))
        n = int(float(row["Param: n"]))
        score = float(row["Score"])
        err_col = next((c for c in rows[0].keys() if "Error" in c), None)
        err = float(row[err_col]) if err_col and (row.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[method][(r, n)] = (score, err)
    return data


def plot_t2(csv_path, out_dir):
    """T2 - per-call registry resolution cost vs registry size (r), one line per
    n. Plotting T(n,r)/n (cost PER resolveUrl call) makes the n-curves collapse
    onto a single curve, which is the direct visual evidence that resolution
    cost factors as n * f(r) - i.e. the O(N*R) mechanism from a per-call,
    unbounded registry re-read/re-parse (no cache)."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no T2 csv at {csv_path}")
        return None
    data = load_t2(csv_path)
    ns = sorted({n for (_, n) in data.get("resolveAllInternal", {})})
    plt.figure(figsize=(8, 5))
    for n in ns:
        pts = sorted((r, score / n) for (r, nn), (score, _err) in data["resolveAllInternal"].items() if nn == n)
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        plt.plot(xs, ys, marker="o", label=f"internal, n={n}")
    # external control: per-call cost should stay ~0 regardless of r
    ext_ns = sorted({n for (_, n) in data.get("resolveAllExternal", {})})
    if ext_ns:
        max_n = max(ext_ns)
        pts = sorted((r, score / max_n) for (r, nn), (score, _err) in data["resolveAllExternal"].items() if nn == max_n)
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        plt.plot(xs, ys, marker="s", linestyle="--", color="gray", label=f"external, n={max_n} (control)")
    plt.xscale("log")
    plt.title("T2 — Per-call cost of resolveUrl() vs registry size (r)")
    plt.xlabel("Functions in the registry (r, log scale)")
    plt.ylabel("Time per call (µs/op ÷ n)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend()
    plt.tight_layout()
    out = os.path.join(out_dir, "T2_registry_scaling.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def load_t3(path):
    """T3 has two @Param dimensions (n, r) and two methods (uncached/cached)."""
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = defaultdict(dict)  # data[method][(r, n)] = (score, err)
    for row in rows:
        if row["Benchmark"].split(".")[-2] != "BenchmarkResolutionOptimization":
            continue
        method = row["Benchmark"].split(".")[-1]
        r = int(float(row["Param: r"]))
        n = int(float(row["Param: n"]))
        score = float(row["Score"])
        err = float(row[err_col]) if err_col and (row.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[method][(r, n)] = (score, err)
    return data


def _plot_t3_one(data, method, rs, ns, style, title, fname, out_dir, ylim):
    """Draw a single T3 strategy (uncached or cached): total resolution time vs N,
    one curve per registry size R, shared log-y range so the two figures are
    directly comparable."""
    colours = plt.cm.viridis([i / max(1, len(rs) - 1) for i in range(len(rs))])
    plt.figure(figsize=(8, 5))
    for i, r in enumerate(rs):
        pts = [(n, data[method][(r, n)][0]) for n in ns]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker=style["marker"], linestyle=style["linestyle"],
                 color=colours[i], label=f"R={r}")
    plt.yscale("log")
    plt.ylim(*ylim)
    plt.title(title)
    plt.xlabel("Number of internal calls (N)")
    plt.ylabel("Total time (µs/op, log scale)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_t3(csv_path, out_dir):
    """T3 - before/after of the registry-read optimization, as two separate figures
    on a shared log-y axis: uncached (per-call read, O(N*R)) vs cached (read once,
    O(N+R)); one curve per registry size R."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no T3 csv at {csv_path}")
        return None
    data = load_t3(csv_path)
    if not data.get("resolveNaive"):
        return None
    rs = sorted({r for (r, _) in data["resolveNaive"]})
    ns = sorted({n for (_, n) in data["resolveNaive"]})
    # shared log-y range across both strategies, with a small margin
    scores = [s for meth in ("resolveNaive", "resolveOptimized")
              for (s, _err) in data[meth].values()]
    ylim = (min(scores) / 1.5, max(scores) * 1.5)
    uncached = _plot_t3_one(
        data, "resolveNaive", rs, ns,
        {"marker": "o", "linestyle": "--"},
        "T3a — Resolution without cache (one read per call, Θ(N·R))",
        "T3a_resolution_sem_cache.png", out_dir, ylim)
    cached = _plot_t3_one(
        data, "resolveOptimized", rs, ns,
        {"marker": "s", "linestyle": "-"},
        "T3b — Resolution with cache (single read, Θ(N+R))",
        "T3b_resolution_com_cache.png", out_dir, ylim)
    return [uncached, cached]


_PIPELINE_SERIES = {
    "resolveNaive": "Without cache (one read per call, Θ(N·R))",
    "resolveOptimized": "With cache (single read, Θ(N+R))",
}


def _plot_pipeline(csv_path, out_dir, cls, title, xlabel, fname, series=None):
    """T5 - workflow resolution, one CSV with a single @Param. Reuses the generic
    load(); log-y because uncached (Θ(N·R)) vs cached (Θ(N+R)) spans ~2 orders of
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
    plt.ylabel("Mean time (µs/op, log scale)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend()
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def load_t4(path):
    """T4 has two @Param dimensions (f, n) and two methods (uncached/cached)."""
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


def _plot_t4_one(data, method, fs, ns, style, title, fname, out_dir, ylim):
    """Draw a single T4 strategy (uncached or cached): total resolution time vs N,
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
    plt.xlabel("Number of calls in the workflow (N)")
    plt.ylabel("Total time (µs/op, log scale)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_t4(csv_path, out_dir):
    """T4 - workflow resolution vs its two axes (F distinct functions x N calls,
    with registry R=F), as two figures on a shared log-y axis: uncached (per-call
    read, O(N*R)) vs cached (read once, O(N+R)); one curve per F."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no T4 csv at {csv_path}")
        return None
    data = load_t4(csv_path)
    if not data.get("resolveNaive"):
        return None
    fs = sorted({f for (f, _) in data["resolveNaive"]})
    ns = sorted({n for (_, n) in data["resolveNaive"]})
    scores = [s for meth in ("resolveNaive", "resolveOptimized")
              for (s, _err) in data[meth].values()]
    ylim = (min(scores) / 1.5, max(scores) * 1.5)
    uncached = _plot_t4_one(
        data, "resolveNaive", fs, ns,
        {"marker": "o", "linestyle": "--"},
        "T4a — Resolution without cache (one read per call, Θ(N·R)) — R=F",
        "T4a_resolution_sem_cache.png", out_dir, ylim)
    cached = _plot_t4_one(
        data, "resolveOptimized", fs, ns,
        {"marker": "s", "linestyle": "-"},
        "T4b — Resolution with cache (single read, Θ(N+R)) — R=F",
        "T4b_resolution_com_cache.png", out_dir, ylim)
    return [uncached, cached]


def plot_t5(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkResolveWorkflowByRegistrySize",
        "T5 — Resolution vs registry size (R) — F=10/N=50 fixed",
        "Functions in the registry (R)",
        "T5_resolution_by_registry.png")


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
    """T6/T7 - cost of a REAL auto-deploy resolver (not a benchmark-only stand-in like
    NaiveEndpointResolver/OptimizedEndpointResolver) vs N calls x R registry size (R=F), one
    curve per F. Unlike T4/T5 there is only one strategy: both resolvers now carry the
    single-read optimization, so this measures the actual (optimized) production cost of the
    unification glue."""
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
    plt.xlabel("Number of internal calls (N)")
    plt.ylabel("Total time (µs/op, log scale)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, fname)
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_t6(csv_path, out_dir):
    return _plot_internal_resolution(
        csv_path, out_dir, "BenchmarkAwsInternalFunctionResolution",
        "T6 — Real AWS resolution (AwsInternalFunctionResolver) — R=F",
        "T6_aws_internal_resolution.png")


def plot_t7(csv_path, out_dir):
    return _plot_internal_resolution(
        csv_path, out_dir, "BenchmarkGoogleInternalFunctionResolution",
        "T7 — Real GCP resolution (WorkflowInternalFunctionResolver) — R=F",
        "T7_google_internal_resolution.png")


_BRANCH_WIDTH_SERIES = {
    "renderChoiceToAmazon": "AWS Choice",
    "renderChoiceToGoogle": "GCP Choice",
    "renderParallelToAmazon": "AWS Parallel",
    "renderParallelToGoogle": "GCP Parallel",
}


def plot_t18(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkRenderingByBranchWidth",
        "T18 — Rendering time vs Choice/Parallel width",
        "Number of conditions (Choice) / branches (Parallel)",
        "T18_rendering_by_branch_width.png",
        series=_BRANCH_WIDTH_SERIES)


def _load_registry_write(csv_path):
    """T8 has two @Param dimensions (r0, k) and a single method (putKSequential)."""
    rows = list(csv.DictReader(open(csv_path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = {}  # data[(r0, k)] = (score, err)
    for row in rows:
        if row["Benchmark"].split(".")[-2] != "BenchmarkRegistryWriteScaling":
            continue
        r0 = int(float(row["Param: r0"]))
        k = int(float(row["Param: k"]))
        score = float(row["Score"])
        err = float(row[err_col]) if err_col and (row.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[(r0, k)] = (score, err)
    return data


_KEY_MATCH_SERIES = {
    "resolveExactMatch": "Exact match (O(1)/lookup)",
    "resolveSuffixMatch": "Suffix match, region-qualified (O(R)/lookup)",
}


def plot_t10(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkResolutionKeyMatchStrategy",
        "T10 — Exact vs. suffix match in the cached resolver — F=10/N=50 fixed",
        "Functions in the registry (R)",
        "T10_resolution_key_match_strategy.png",
        series=_KEY_MATCH_SERIES)


def plot_t8(csv_path, out_dir):
    """T8 - cost of K sequential FunctionRegistryStore.put() calls starting from a registry of
    R0 entries, one curve per R0. Complements T2-T7 (read side) with the write side: put()
    re-reads and rewrites the whole file per call, so K registrations cost Theta(K*R0 + K^2)."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no T8 csv at {csv_path}")
        return None
    data = _load_registry_write(csv_path)
    if not data:
        print("  [skip] no data for BenchmarkRegistryWriteScaling")
        return None
    r0s = sorted({r0 for (r0, _) in data})
    ks = sorted({k for (_, k) in data})
    colours = plt.cm.viridis([i / max(1, len(r0s) - 1) for i in range(len(r0s))])
    plt.figure(figsize=(8, 5))
    for i, r0 in enumerate(r0s):
        pts = [(k, data[(r0, k)][0]) for k in ks if (r0, k) in data]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker="o", linestyle="--", color=colours[i], label=f"R0={r0}")
    plt.yscale("log")
    plt.xscale("log")
    plt.title("T8 — Incremental registry write cost (put) — R0 × K")
    plt.xlabel("Number of successive writes (K, log scale)")
    plt.ylabel("Total time (µs/op, log scale)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, "T8_registry_write_scaling.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def _load_registry_miss_and_deploy(csv_path):
    """T9 has two @Param dimensions (r0, k) and a single method
    (missThenDeployKSequential) - same shape as T8's loader."""
    rows = list(csv.DictReader(open(csv_path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = {}  # data[(r0, k)] = (score, err)
    for row in rows:
        if row["Benchmark"].split(".")[-2] != "BenchmarkRegistryMissAndDeploy":
            continue
        r0 = int(float(row["Param: r0"]))
        k = int(float(row["Param: k"]))
        score = float(row["Score"])
        err = float(row[err_col]) if err_col and (row.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[(r0, k)] = (score, err)
    return data


def plot_t9(csv_path, out_dir):
    """T9 - cost of K sequential (tryResolveEntry miss + put) pairs starting from a registry of
    R0 entries, one curve per R0. Same (R0, K) grid as T8 (put-only), so the two are directly
    comparable on the same axes - the gap between them is the added cost of the miss lookup."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no T9 csv at {csv_path}")
        return None
    data = _load_registry_miss_and_deploy(csv_path)
    if not data:
        print("  [skip] no data for BenchmarkRegistryMissAndDeploy")
        return None
    r0s = sorted({r0 for (r0, _) in data})
    ks = sorted({k for (_, k) in data})
    colours = plt.cm.viridis([i / max(1, len(r0s) - 1) for i in range(len(r0s))])
    plt.figure(figsize=(8, 5))
    for i, r0 in enumerate(r0s):
        pts = [(k, data[(r0, k)][0]) for k in ks if (r0, k) in data]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker="o", linestyle="--", color=colours[i], label=f"R0={r0}")
    plt.yscale("log")
    plt.xscale("log")
    plt.title("T9 — Cost of miss+deploy (tryResolveEntry + put) — R0 × K")
    plt.xlabel("Number of new functions resolved+registered (K, log scale)")
    plt.ylabel("Total time (µs/op, log scale)")
    plt.grid(True, alpha=0.3, which="both")
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, "T9_registry_miss_and_deploy.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def _load_ie(csv_path, cls):
    """Generic loader for CSVs with two @Param dims (i, e) and a single @Benchmark method."""
    rows = list(csv.DictReader(open(csv_path, encoding="utf-8")))
    err_col = next((c for c in rows[0].keys() if "Error" in c), None)
    data = {}  # data[(i, e)] = (score, err)
    for row in rows:
        if row["Benchmark"].split(".")[-2] != cls:
            continue
        i_val = int(float(row["Param: i"]))
        e_val = int(float(row["Param: e"]))
        score = float(row["Score"])
        err = float(row[err_col]) if err_col and (row.get(err_col) or "").strip() not in ("", "NaN") else 0.0
        data[(i_val, e_val)] = (score, err)
    return data


def plot_t11(csv_path, out_dir):
    """T11 - resolution cost of a workflow mixing internal (I) and external (E) calls, N=I+E,
    R=F=10 fixed. Only the cached/optimized resolver is measured (naive-vs-optimized already
    established in T2-T5). One curve per I value, x-axis E, showing whether cost tracks I (the
    only calls that touch the registry) rather than the full N=I+E."""
    if not os.path.exists(csv_path):
        print(f"  [skip] no T11 csv at {csv_path}")
        return None
    data = _load_ie(csv_path, "BenchmarkResolveWorkflowByInternalExternalMix")
    if not data:
        print("  [skip] no data for BenchmarkResolveWorkflowByInternalExternalMix")
        return None
    i_values = sorted({i_val for (i_val, _) in data})
    e_values = sorted({e_val for (_, e_val) in data})
    colours = plt.cm.viridis([k / max(1, len(i_values) - 1) for k in range(len(i_values))])
    plt.figure(figsize=(8, 5))
    for k, i_val in enumerate(i_values):
        pts = [(e_val, data[(i_val, e_val)][0]) for e_val in e_values if (i_val, e_val) in data]
        plt.plot([x for x, _ in pts], [y for _, y in pts],
                 marker="o", linestyle="-", color=colours[k], label=f"I={i_val}")
    plt.title("T11 — Resolution vs internal/external mix (I × E) — R=F=10 fixed")
    plt.xlabel("Number of external calls (E)")
    plt.ylabel("Total time (µs/op)")
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    out = os.path.join(out_dir, "T11_resolution_internal_external_mix.png")
    plt.savefig(out, dpi=130)
    plt.close()
    print(f"  [ok] {out}")
    return out


def plot_t12(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkResolveWorkflowByRegistrySizeMixed",
        "T12 — Resolution vs registry size (R), mixed workflow — I=40/E=10/F=10 fixed",
        "Functions in the registry (R)",
        "T12_resolution_by_registry_mixed.png",
        series={"resolveOptimized": "With cache (mixed workflow)"})


def plot_t13(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkInternalResolutionByNesting",
        "T13 — Internal resolution vs nesting depth — 20 calls/F=10 fixed",
        "Nesting depth (chained parallel/iteration)",
        "T13_internal_resolution_by_nesting.png",
        series={"resolveOptimized": "With cache (nested workflow)"})


def plot_t14(csv_path, out_dir):
    return _plot_pipeline(
        csv_path, out_dir, "BenchmarkInternalResolutionByBranchWidth",
        "T14 — Internal resolution vs Parallel width — 5 calls/branch, F=10 fixed",
        "Number of Parallel branches",
        "T14_internal_resolution_by_branch_width.png",
        series={"resolveOptimized": "With cache (wide-Parallel workflow)"})


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
    plt.title("S2 — Size of the rendered artifact vs number of functions")
    plt.xlabel("Number of functions (workflow steps)")
    plt.ylabel("Artifact size (KB)")
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
    missing = [cls for cls in PLOTS if cls not in data]
    if missing:
        fallback = os.path.join(os.path.dirname(os.path.abspath(CSV)), FALLBACK_CSV)
        if os.path.abspath(fallback) != os.path.abspath(CSV) and os.path.exists(fallback):
            extra = load(fallback)
            for cls in missing:
                if cls in extra:
                    data[cls] = extra[cls]
                    print(f"  [note] {cls} taken from {FALLBACK_CSV}")
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

    t2_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t2.csv")
    t2_out = plot_t2(t2_csv, OUT)
    if t2_out:
        made.append(t2_out)

    t3_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t3.csv")
    t3_out = plot_t3(t3_csv, OUT)
    if t3_out:
        made.extend(t3_out)

    t4_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t4.csv")
    t4_out = plot_t4(t4_csv, OUT)
    if t4_out:
        made.extend(t4_out)

    t5_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t5.csv")
    t5_out = plot_t5(t5_csv, OUT)
    if t5_out:
        made.append(t5_out)

    t6_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t6.csv")
    t6_out = plot_t6(t6_csv, OUT)
    if t6_out:
        made.append(t6_out)

    t7_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t7.csv")
    t7_out = plot_t7(t7_csv, OUT)
    if t7_out:
        made.append(t7_out)

    t18_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t18.csv")
    t18_out = plot_t18(t18_csv, OUT)
    if t18_out:
        made.append(t18_out)

    t8_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t8.csv")
    t8_out = plot_t8(t8_csv, OUT)
    if t8_out:
        made.append(t8_out)

    t10_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t10.csv")
    t10_out = plot_t10(t10_csv, OUT)
    if t10_out:
        made.append(t10_out)

    t11_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t11.csv")
    t11_out = plot_t11(t11_csv, OUT)
    if t11_out:
        made.append(t11_out)

    t12_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t12.csv")
    t12_out = plot_t12(t12_csv, OUT)
    if t12_out:
        made.append(t12_out)

    t9_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t9.csv")
    t9_out = plot_t9(t9_csv, OUT)
    if t9_out:
        made.append(t9_out)

    t13_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t13.csv")
    t13_out = plot_t13(t13_csv, OUT)
    if t13_out:
        made.append(t13_out)

    t14_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "jmh-results-t14.csv")
    t14_out = plot_t14(t14_csv, OUT)
    if t14_out:
        made.append(t14_out)

    s2_csv = os.path.join(os.path.dirname(os.path.abspath(CSV)), "artifact-size.csv")
    s2_out = plot_artifact_size(s2_csv, OUT)
    if s2_out:
        made.append(s2_out)

    print(f"\nGenerated {len(made)} graphs in {OUT}")


if __name__ == "__main__":
    main()
