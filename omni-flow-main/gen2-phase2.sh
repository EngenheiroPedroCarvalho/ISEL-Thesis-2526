#!/usr/bin/env bash
#
# Phase 2 of the Cloud Run functions (2nd gen) experiment: run OmniFlow's Example 2 end to end,
# so that the resolution cascade deploys quickfaas-test-fn through QuickFaaS (Level 3), then
# validates it (Level 1) or rediscovers it (Level 2) on later runs, and deploys the workflow.
#
# Runs in WORK_DIR, which holds the function registry and the descriptor, so repeated runs see
# the registry the previous run left behind. Delete WORK_DIR/function-registry.gcp.json to start
# over; replace it with an empty registry to force Level 2.
#
# Needs Application Default Credentials for an account that can deploy to PROJECT.
#
set -euo pipefail

JAVA_HOME_MVN=${JAVA_HOME_MVN:-/opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home}
JAVA_HOME_17=${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
PROJECT=${PROJECT:?Set PROJECT to the GCP project ID}
BUCKET=${BUCKET:?Set BUCKET to a Cloud Storage bucket in PROJECT}
REGION=${REGION:-europe-west1}
SERVICE_ACCOUNT=${SERVICE_ACCOUNT:-workflow-test@$PROJECT.iam.gserviceaccount.com}
WORK_DIR=${WORK_DIR:?Set WORK_DIR to a directory for the registry and descriptor}

OMNIFLOW_DIR=$(cd "$(dirname "$0")" && pwd)
FUNCTION_DIR="$OMNIFLOW_DIR/functions/quickfaas-test-fn"
JAR="$OMNIFLOW_DIR/QuickFaaS-Deployment-1.0-fat.jar"  # built by gen2-phase1.sh

echo "==> Building OmniFlow"
CP_FILE=$(mktemp)
(cd "$OMNIFLOW_DIR" && JAVA_HOME="$JAVA_HOME_MVN" ./mvnw -o -q -pl deployment compile \
    dependency:build-classpath -Dmdep.outputFile="$CP_FILE")
CLASSPATH="$OMNIFLOW_DIR/deployment/target/classes:$(cat "$CP_FILE")"
rm -f "$CP_FILE"

# Example 2 reads its descriptor from ./functions/quickfaas-test-fn, relative to the working
# directory, and the descriptor names the project and bucket.
echo "==> Staging the descriptor in $WORK_DIR (project=$PROJECT, bucket=$BUCKET)"
mkdir -p "$WORK_DIR/functions/quickfaas-test-fn"
cp "$FUNCTION_DIR/MyFunctionClass.java" "$WORK_DIR/functions/quickfaas-test-fn/"
python3 - "$FUNCTION_DIR/func-deployment.json" "$WORK_DIR/functions/quickfaas-test-fn/func-deployment.json" \
    "$PROJECT" "$BUCKET" "$REGION" <<'PY'
import json, sys
src, dst, project, bucket, region = sys.argv[1:6]
descriptor = json.load(open(src))
descriptor["project"] = project
descriptor["function"]["bucket"] = bucket
descriptor["function"]["location"] = region
json.dump(descriptor, open(dst, "w"), indent=2)
PY

echo "==> Running Example 2 (registry: $WORK_DIR/function-registry.gcp.json)"
cd "$WORK_DIR"
# QuickFaaS runs as a 'java -jar' subprocess resolved from the PATH, like in Phase 1.
export PATH="$JAVA_HOME_17/bin:$PATH" JAVA_HOME="$JAVA_HOME_17"
# Option 2, then 'y' to continue without GOOGLE_APPLICATION_CREDENTIALS (ADC is used instead).
printf '2\ny\n' | QUICKFAAS_JAR_PATH="$JAR" GOOGLE_PROJECT_ID="$PROJECT" GOOGLE_ZONE="$REGION" \
    GOOGLE_SERVICE_ACCOUNT="$SERVICE_ACCOUNT" \
    "$JAVA_HOME_MVN/bin/java" -cp "$CLASSPATH" costaber.com.github.omniflow.MainKt
