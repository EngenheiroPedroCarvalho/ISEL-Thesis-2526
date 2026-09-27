#!/usr/bin/env bash
#
# Phase 1 of the Cloud Run functions (2nd gen) experiment: deploy a function with QuickFaaS
# alone, without OmniFlow, so that a failure points at the v2 API changes and nothing else.
#
# Needs an OAuth access token, either in GCP_ACCESS_TOKEN or from gcloud on the PATH.
#
set -euo pipefail

JAVA_HOME_17=${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
BUCKET=${BUCKET:-omniflow-quickfaas-deploy}
PROJECT=${PROJECT:-}

OMNIFLOW_DIR=$(cd "$(dirname "$0")" && pwd)
QUICKFAAS_DIR="$OMNIFLOW_DIR/quickfaas-essentials/QuickFaaS-Deployment"
FUNCTION_DIR="$OMNIFLOW_DIR/functions/quickfaas-test-fn"
JAR_NAME=QuickFaaS-Deployment-1.0-fat.jar

TOKEN=${GCP_ACCESS_TOKEN:-}
if [ -z "$TOKEN" ]; then
    if command -v gcloud >/dev/null 2>&1; then
        TOKEN=$(gcloud auth print-access-token)
    else
        echo "No access token. Set GCP_ACCESS_TOKEN, or put gcloud on the PATH." >&2
        exit 1
    fi
fi

# QuickFaaS is run as 'java -jar' from the directory holding the JAR, and expects the
# descriptor, the function source and a 'function-deployment' directory to sit beside it.
echo "==> Building the QuickFaaS fat JAR"
(cd "$QUICKFAAS_DIR" && JAVA_HOME="$JAVA_HOME_17" ./gradlew --quiet fatJar)
cp "$QUICKFAAS_DIR/build/libs/$JAR_NAME" "$OMNIFLOW_DIR/$JAR_NAME"

echo "==> Staging the descriptor (bucket=$BUCKET, project=${PROJECT:-from descriptor}) and the function source"
cp "$FUNCTION_DIR/MyFunctionClass.java" "$OMNIFLOW_DIR/"
python3 - "$FUNCTION_DIR/func-deployment.json" "$OMNIFLOW_DIR/func-deployment.json" "$TOKEN" "$BUCKET" "$PROJECT" <<'PY'
import json, sys
src, dst, token, bucket, project = sys.argv[1:6]
descriptor = json.load(open(src))
descriptor["accessToken"] = token
descriptor["function"]["bucket"] = bucket
if project:
    descriptor["project"] = project
json.dump(descriptor, open(dst, "w"), indent=2)
PY

trap 'rm -f "$OMNIFLOW_DIR/func-deployment.json" "$OMNIFLOW_DIR/MyFunctionClass.java"' EXIT

echo "==> Deploying"
cd "$OMNIFLOW_DIR"
"$JAVA_HOME_17/bin/java" -jar "$JAR_NAME"

cat <<'EOF'

Deployed. What to check, in order:
  gcloud run services list --region=europe-west1
      The function must show up here. If it does not, it was not created as 2nd gen.
  gcloud functions describe quickfaas-test-fn --gen2 --region=europe-west1 \
      --format='value(state,serviceConfig.uri,serviceConfig.service)'
      The uri must be a run.app URL, and 'service' is what the registry will key on.
EOF
