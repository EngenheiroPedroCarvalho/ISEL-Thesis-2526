#!/usr/bin/env bash
# Runs the Chapter 5 case study (PaymentAuthorization) against GCP project tfm26-509910.
#   Phase 0: deploy the core-banking stub with QuickFaaS alone (called through a literal URL).
#   Phase 1: first deployment, no registry: bootstrap, Level 3 for both scoring functions.
#   Phase 2: execute the workflow with an approved and a declined transaction.
#   Phase 3: delete the workflow (the deployer only creates) and deploy again: Level 1 hits.
#   Phase 4: execute again.
# Logs and registry snapshots go to work/logs.
set -euo pipefail
C=$(cd "$(dirname "$0")" && pwd); W="$C/work"; L="$W/logs"; mkdir -p "$L"
J17=/opt/homebrew/opt/openjdk@17/bin/java
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"   # QuickFaaS runs as 'java -jar'
CP="/Users/pedrocarvalho/IdeaProjects/ISEL-Thesis-2526/omni-flow-main/deployment/target/classes:$(cat "$C/../cp.txt")"
P=tfm26-509910; R=europe-west1; WF=payment-authorization
cd "$W"

echo "== Phase 0: core-banking stub"
if ! gcloud functions describe core-banking-stub --gen2 --region=$R --project=$P >/dev/null 2>&1; then
  (cd qf && cp ../functions/core-banking-stub/MyFunctionClass.java . &&
   python3 -c 'import json,sys; d=json.load(open("../functions/core-banking-stub/func-deployment.json")); d["accessToken"]=sys.argv[1]; json.dump(d,open("func-deployment.json","w"),indent=2)' "$(gcloud auth print-access-token)" &&
   { $J17 -jar QuickFaaS-Deployment-1.0-fat.jar > "$L/0-stub.log" 2>&1 || true; }; rm -f func-deployment.json MyFunctionClass.java
   grep -q "Deployment finished successfully" "$L/0-stub.log")   # QuickFaaS can exit non-zero on success
fi
STUB=$(gcloud functions describe core-banking-stub --gen2 --region=$R --project=$P --format='value(serviceConfig.uri)')
echo "stub: $STUB"

echo "== Phase 1: first deployment (no registry)"
rm -f function-registry.gcp.json
gcloud workflows delete $WF --location=$R --project=$P --quiet 2>/dev/null || true
$J17 -cp "$C/out:$CP" CaseKt deploy "$STUB" $WF 2>&1 | tee "$L/1-first-deploy.log"
cp function-registry.gcp.json "$L/registry-after-first.json"
gcloud workflows describe $WF --location=$R --project=$P --format='value(sourceContents)' > "$L/rendered-first.yaml"

run() { gcloud workflows run $WF --location=$R --project=$P --data="$1" --format='value(state,result)'; }
echo "== Phase 2: executions"
{ echo "tx-001:"; run '{"id":"tx-001","amount":120,"country":"PT"}'
  echo "tx-002:"; run '{"id":"tx-002","amount":4800,"country":"ES"}'; } 2>&1 | tee "$L/2-executions.log"

echo "== Phase 3: second deployment (registry kept, workflow recreated)"
gcloud workflows delete $WF --location=$R --project=$P --quiet
$J17 -cp "$C/out:$CP" CaseKt deploy "$STUB" $WF 2>&1 | tee "$L/3-second-deploy.log"
cp function-registry.gcp.json "$L/registry-after-second.json"
cmp -s "$L/registry-after-first.json" "$L/registry-after-second.json" && echo "registry unchanged" | tee -a "$L/3-second-deploy.log"

echo "== Phase 4: executions after the second deployment"
{ echo "tx-003:"; run '{"id":"tx-003","amount":300,"country":"PT"}'; } 2>&1 | tee "$L/4-executions.log"
echo "done: logs in $L"
