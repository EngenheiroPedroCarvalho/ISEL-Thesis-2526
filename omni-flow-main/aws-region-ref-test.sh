#!/usr/bin/env bash
#
# Tests the AwsLambdaDeployer fix for "region/name" references (b296e87) against AWS, with
# OmniFlow's Example 7 calling internalFunction("$REGION/$FUNCTION").
#
#   ./aws-region-ref-test.sh run       deploy through the cascade and check the registry
#   ./aws-region-ref-test.sh execute   run the last state machine deployed and check the result
#
# First 'run' (no registry in WORK_DIR): the function must not exist in REGION, so the cascade
# reaches Level 3. Before the fix, Level 3 polled GetFunction with the prefixed name and timed
# out after 180 s. Expect a registry entry "$REGION/$FUNCTION" whose serviceName is the bare
# "$FUNCTION".
#
# Second 'run' (registry left by the first): the call is a registry hit. Before the fix, the
# stored serviceName was the prefixed name, which GetFunction rejects, so validation aborted.
# Expect the hit to validate with no write, so the registry's updatedAt does not change.
#
# Each 'run' creates a new state machine (the deployer only creates, so names cannot repeat).
# Delete WORK_DIR/function-registry.aws.json and the Lambda to start over.
#
# Needs AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY for a user with the permissions listed in
# AWS-Setup-Guide.md.
#
set -euo pipefail

JAVA_HOME_MVN=${JAVA_HOME_MVN:-/opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home}
JAVA_HOME_17=${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
: "${AWS_ACCESS_KEY_ID:?Set AWS_ACCESS_KEY_ID}" "${AWS_SECRET_ACCESS_KEY:?Set AWS_SECRET_ACCESS_KEY}"
STEP_FUNCTIONS_ROLE_ARN=${STEP_FUNCTIONS_ROLE_ARN:?Set STEP_FUNCTIONS_ROLE_ARN to a role with lambda:InvokeFunction}
BUCKET=${BUCKET:?Set BUCKET to an S3 bucket in REGION}
REGION=${REGION:-eu-west-1}
FUNCTION=${FUNCTION:-hello-lambda-fn}
WORK_DIR=${WORK_DIR:?Set WORK_DIR to a directory for the registry and descriptor}
MODE=${1:-run}

OMNIFLOW_DIR=$(cd "$(dirname "$0")" && pwd)
FUNCTION_DIR="$OMNIFLOW_DIR/functions/hello-lambda-fn"
JAR="$OMNIFLOW_DIR/QuickFaaS-Deployment-1.0-fat.jar"
CHECK="$OMNIFLOW_DIR/aws-region-ref-check.java"
REGISTRY="$WORK_DIR/function-registry.aws.json"
KEY="$REGION/$FUNCTION"
# The account ID is the fifth field of the role ARN (arn:aws:iam::<account>:role/<name>).
ACCOUNT_ID=$(cut -d: -f5 <<<"$STEP_FUNCTIONS_ROLE_ARN")

mkdir -p "$WORK_DIR"

echo "==> Building OmniFlow"
CP_FILE=$(mktemp)
(cd "$OMNIFLOW_DIR" && JAVA_HOME="$JAVA_HOME_MVN" ./mvnw -o -q -pl deployment compile \
    dependency:build-classpath -Dmdep.outputFile="$CP_FILE")
CLASSPATH="$OMNIFLOW_DIR/deployment/target/classes:$(cat "$CP_FILE")"
rm -f "$CP_FILE"

check() { "$JAVA_HOME_MVN/bin/java" -cp "$CLASSPATH" "$CHECK" "$@"; }

# Prints the registry's updatedAt and the entry for KEY (empty fields when absent).
registry_state() {
    python3 - "$REGISTRY" "$KEY" <<'PY'
import json, os, sys
path, key = sys.argv[1:3]
if not os.path.exists(path):
    print("||")
    sys.exit()
registry = json.load(open(path))
entry = registry.get("functions", {}).get(key, {})
print(f'{registry.get("updatedAt", "")}|{entry.get("serviceName", "")}|{entry.get("url", "")}')
PY
}

case "$MODE" in
run)
    if [[ -f "$REGISTRY" ]]; then
        LEVEL="registry hit (Level 1)"
    else
        LEVEL="deployment (Level 3)"
        echo "==> Checking that $FUNCTION does not exist in $REGION"
        if ! check absent "$REGION" "$FUNCTION"; then
            echo "FAIL: $FUNCTION already exists in $REGION, so the cascade would stop at Level 2." >&2
            echo "      Delete it first, or pick another FUNCTION name." >&2
            exit 1
        fi
    fi
    BEFORE=$(registry_state)

    echo "==> Staging the descriptor in $WORK_DIR (account=$ACCOUNT_ID, bucket=$BUCKET, region=$REGION)"
    mkdir -p "$WORK_DIR/functions/hello-lambda-fn"
    cp "$FUNCTION_DIR/MyFunctionClass.java" "$WORK_DIR/functions/hello-lambda-fn/"
    python3 - "$FUNCTION_DIR/func-deployment.json" "$WORK_DIR/functions/hello-lambda-fn/func-deployment.json" \
        "$ACCOUNT_ID" "$BUCKET" "$REGION" "$FUNCTION" <<'PY'
import json, sys
src, dst, account, bucket, region, function = sys.argv[1:7]
descriptor = json.load(open(src))
descriptor["project"] = account
descriptor["function"]["name"] = function
descriptor["function"]["bucket"] = bucket
descriptor["function"]["location"] = region
# A placeholder makes AwsLambdaDeployer create (or reuse) OmniFlowLambdaExecutionRole.
descriptor["iamRoleArn"] = "<auto>"
json.dump(descriptor, open(dst, "w"), indent=2)
PY

    STATE_MACHINE_NAME="RegionRefTest-$(date +%Y%m%d-%H%M%S)"
    echo "$STATE_MACHINE_NAME" > "$WORK_DIR/last-state-machine"
    echo "==> Running Example 7 with internalFunction(\"$KEY\"), expecting a $LEVEL"
    cd "$WORK_DIR"
    # QuickFaaS runs as a 'java -jar' subprocess resolved from the PATH and builds with Java 17.
    export PATH="$JAVA_HOME_17/bin:$PATH" JAVA_HOME="$JAVA_HOME_17"
    START=$(date +%s)
    printf '7\n' | EXAMPLE7_FUNCTION_REF="$KEY" QUICKFAAS_JAR_PATH="$JAR" AWS_REGION="$REGION" \
        STEP_FUNCTIONS_ROLE_ARN="$STEP_FUNCTIONS_ROLE_ARN" STATE_MACHINE_NAME="$STATE_MACHINE_NAME" \
        "$JAVA_HOME_MVN/bin/java" -cp "$CLASSPATH" costaber.com.github.omniflow.MainKt \
        | tee "$WORK_DIR/last-run.log"
    echo "==> Run took $(( $(date +%s) - START )) s"

    AFTER=$(registry_state)
    IFS='|' read -r BEFORE_UPDATED _ _ <<<"$BEFORE"
    IFS='|' read -r AFTER_UPDATED SERVICE_NAME URL <<<"$AFTER"
    echo "==> Registry entry \"$KEY\": serviceName=$SERVICE_NAME url=$URL"
    STATUS=0
    [[ "$SERVICE_NAME" == "$FUNCTION" ]] \
        || { echo "FAIL: serviceName should be \"$FUNCTION\", without the region prefix" >&2; STATUS=1; }
    [[ "$URL" == arn:aws:lambda:$REGION:$ACCOUNT_ID:function:$FUNCTION ]] \
        || { echo "FAIL: url should be the ARN of $FUNCTION in $REGION" >&2; STATUS=1; }
    if [[ -n "$BEFORE_UPDATED" ]]; then
        [[ "$AFTER_UPDATED" == "$BEFORE_UPDATED" ]] \
            || { echo "FAIL: the registry was rewritten, so the hit did not validate cleanly" >&2; STATUS=1; }
    fi
    [[ $STATUS -eq 0 ]] && echo "PASS: $LEVEL. State machine: $STATE_MACHINE_NAME (next: $0 execute)"
    exit $STATUS
    ;;
execute)
    STATE_MACHINE_NAME=$(cat "$WORK_DIR/last-state-machine")
    echo "==> Executing $STATE_MACHINE_NAME in $REGION with input {}"
    if check execute "$REGION" "$STATE_MACHINE_NAME"; then
        echo "PASS: the execution succeeded"
    else
        echo "FAIL: the execution did not succeed" >&2
        exit 1
    fi
    ;;
*)
    echo "usage: $0 [run|execute]" >&2
    exit 2
    ;;
esac
