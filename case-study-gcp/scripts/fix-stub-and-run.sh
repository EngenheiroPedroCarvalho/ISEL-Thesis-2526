#!/usr/bin/env bash
# Replaces the core-banking stub (the first version failed on Cloud Workflows' JSON content type)
# and runs the three transactions against the already deployed payment-authorization workflow.
# The run.app URL depends only on the service name and project, so the workflow needs no change.
set -euo pipefail
C=$(cd "$(dirname "$0")" && pwd); W="$C/work"; L="$W/logs"
J17=/opt/homebrew/opt/openjdk@17/bin/java
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH" JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home   # QuickFaaS runs Maven
P=tfm26-509910; R=europe-west1; WF=payment-authorization
echo "== Deleting the old stub"
if gcloud functions describe core-banking-stub --gen2 --region=$R --project=$P >/dev/null 2>&1; then
  gcloud functions delete core-banking-stub --gen2 --region=$R --project=$P --quiet
fi
echo "== Deploying the new stub with QuickFaaS"
cd "$W/qf" && cp ../functions/core-banking-stub/MyFunctionClass.java .
python3 -c 'import json,sys; d=json.load(open("../functions/core-banking-stub/func-deployment.json")); d["accessToken"]=sys.argv[1]; json.dump(d,open("func-deployment.json","w"),indent=2)' "$(gcloud auth print-access-token)"
{ $J17 -jar QuickFaaS-Deployment-1.0-fat.jar > "$L/5-stub-redeploy.log" 2>&1 || true; }
rm -f func-deployment.json MyFunctionClass.java
grep -q "Deployment finished successfully" "$L/5-stub-redeploy.log"
gcloud functions describe core-banking-stub --gen2 --region=$R --project=$P --format='value(state,serviceConfig.uri)'
run() { gcloud workflows run $WF --location=$R --project=$P --data="$1" --format='value(name,state,result)'; }
echo "== Executions"
{ echo "tx-001:"; run '{"id":"tx-001","amount":120,"country":"PT"}'
  echo "tx-002:"; run '{"id":"tx-002","amount":4800,"country":"ES"}'
  echo "tx-003:"; run '{"id":"tx-003","amount":300,"country":"PT"}'; } 2>&1 | tee "$L/6-executions.log"
