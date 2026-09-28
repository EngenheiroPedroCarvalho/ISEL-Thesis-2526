#!/bin/zsh
# Runs the Chapter 5 case study (PaymentAuthorization) against AWS, mirroring the GCP run.
#   Phase 0: Lambda execution role, and the core-banking stub (Lambda behind an HTTP API).
#   Phase 1: first deployment, no registry: bootstrap, Level 3 for both scoring functions.
#   Phase 2: execute the state machine with an approved and a declined transaction.
#   Phase 3: delete the state machine (the deployer only creates) and deploy again: Level 1 hits.
#   Phase 4: execute again.
# Needs the credentials exported and aws-case.env (from setup-aws.sh). Logs go to work/logs.
set -euo pipefail
C=${0:A:h}; W=$C/work; L=$W/logs; mkdir -p $L
set -a; source $C/aws-case.env; set +a
export AWS_PAGER=""
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"          # QuickFaaS runs as 'java -jar' and builds with Maven
CP="/Users/pedrocarvalho/IdeaProjects/ISEL-Thesis-2526/omni-flow-main/deployment/target/classes:$(cat $C/cp.txt)"
SM=payment-authorization
SM_ARN=arn:aws:states:${AWS_REGION}:${AWS_ACCOUNT_ID}:stateMachine:${SM}
cd $W

echo "== Phase 0: Lambda execution role and core-banking stub" | tee $L/0-stub.log
LROLE=OmniFlowLambdaExecutionRole
if ! aws iam get-role --role-name ${LROLE} >/dev/null 2>&1; then
  aws iam create-role --role-name ${LROLE} --description "Lambda execution role managed by OmniFlow" \
    --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
  aws iam attach-role-policy --role-name ${LROLE} \
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
  echo "created ${LROLE}; waiting for IAM propagation" | tee -a $L/0-stub.log; sleep 15
fi
LROLE_ARN=$(aws iam get-role --role-name ${LROLE} --query Role.Arn --output text)

if ! aws lambda get-function --function-name core-banking-stub >/dev/null 2>&1; then
  (cd stub && rm -f ../stub.zip && zip -q ../stub.zip handler.py)
  for i in 1 2 3 4 5; do   # a new role can take a while before Lambda may assume it
    aws lambda create-function --function-name core-banking-stub --runtime python3.12 \
      --handler handler.handler --role ${LROLE_ARN} --zip-file fileb://stub.zip >/dev/null && break
    [[ $i == 5 ]] && exit 1; sleep 10
  done
  aws lambda wait function-active-v2 --function-name core-banking-stub
fi
STUB_ARN=$(aws lambda get-function --function-name core-banking-stub --query Configuration.FunctionArn --output text)

API_ID=$(aws apigatewayv2 get-apis --query "Items[?Name=='core-banking-stub'].ApiId | [0]" --output text)
if [[ ${API_ID} == None ]]; then
  API_ID=$(aws apigatewayv2 create-api --name core-banking-stub --protocol-type HTTP --target ${STUB_ARN} \
    --query ApiId --output text)
  aws lambda add-permission --function-name core-banking-stub --statement-id apigateway-invoke \
    --action lambda:InvokeFunction --principal apigateway.amazonaws.com \
    --source-arn "arn:aws:execute-api:${AWS_REGION}:${AWS_ACCOUNT_ID}:${API_ID}/*" >/dev/null
  sleep 5
fi
STUB=${API_ID}.execute-api.${AWS_REGION}.amazonaws.com
echo "stub: ${STUB}" | tee -a $L/0-stub.log
curl -s -X POST "https://${STUB}/?decision=probe" -H 'Content-Type: application/json' -d '{"id":"probe"}' \
  | tee -a $L/0-stub.log; echo | tee -a $L/0-stub.log

delete_sm() {
  aws stepfunctions delete-state-machine --state-machine-arn ${SM_ARN} 2>/dev/null || return 0
  while aws stepfunctions describe-state-machine --state-machine-arn ${SM_ARN} >/dev/null 2>&1; do sleep 3; done
}
deploy() { java -cp "$C/out:$CP" CaseAwsKt deploy "${STUB}" ${SM}; }

if [[ ${1:-} != from2 ]]; then   # 'from2' resumes after a completed first deployment
  echo "== Phase 1: first deployment (no registry)"
  rm -f function-registry.aws.json
  delete_sm
  deploy 2>&1 | tee $L/1-first-deploy.log
  cp function-registry.aws.json $L/registry-after-first.json
fi
for i in 1 2 3 4 5; do   # the new state machine can take a moment to be readable
  aws stepfunctions describe-state-machine --state-machine-arn ${SM_ARN} --query definition --output text \
    > $L/rendered-first.asl.json 2>>$L/describe.err && break
  [[ $i == 5 ]] && exit 1; sleep 5
done

run() {  # name, input
  local arn st
  arn=$(aws stepfunctions start-execution --state-machine-arn ${SM_ARN} --name "$1-$(date +%s)" \
    --input "$2" --query executionArn --output text)
  while st=$(aws stepfunctions describe-execution --execution-arn $arn --query status --output text); \
        [[ $st == RUNNING ]]; do sleep 2; done
  echo "$1: $st"
  if [[ $st == SUCCEEDED ]]; then
    aws stepfunctions describe-execution --execution-arn $arn --query output --output text
  else
    aws stepfunctions get-execution-history --execution-arn $arn --reverse-order --max-items 3 \
      --query 'events[].[type, executionFailedEventDetails.error, executionFailedEventDetails.cause]' --output text
  fi
}
echo "== Phase 2: executions"
{ run tx-001 '{"transaction":{"id":"tx-001","amount":120,"country":"PT"}}'
  run tx-002 '{"transaction":{"id":"tx-002","amount":4800,"country":"ES"}}'; } 2>&1 | tee $L/2-executions.log

echo "== Phase 3: second deployment (registry kept, state machine recreated)"
delete_sm
deploy 2>&1 | tee $L/3-second-deploy.log
cp function-registry.aws.json $L/registry-after-second.json
cmp -s $L/registry-after-first.json $L/registry-after-second.json && echo "registry unchanged" | tee -a $L/3-second-deploy.log

echo "== Phase 4: executions after the second deployment"
run tx-003 '{"transaction":{"id":"tx-003","amount":300,"country":"PT"}}' 2>&1 | tee $L/4-executions.log
echo "done: logs in $L"
