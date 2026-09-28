#!/bin/zsh
# Creates what the AWS case study needs before OmniFlow runs: the S3 bucket QuickFaaS uploads the
# Lambda packages to, and the IAM role Step Functions runs the state machine with.
# Safe to re-run: existing resources are kept. Writes the values to aws-case.env.
#
# Needs: AWS CLI v2 and credentials in AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
# (/ AWS_SESSION_TOKEN) - the same variables OmniFlow and QuickFaaS read.
set -euo pipefail
HERE=${0:A:h}

REGION=${AWS_REGION:-eu-west-1}
export AWS_REGION=${REGION} AWS_PAGER=""

: ${AWS_ACCESS_KEY_ID:?export AWS_ACCESS_KEY_ID first} ${AWS_SECRET_ACCESS_KEY:?export AWS_SECRET_ACCESS_KEY first}

ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
BUCKET="omniflow-quickfaas-${ACCOUNT}-${REGION}"
ROLE=OmniFlowStepFunctionsRole
echo "Account ${ACCOUNT}, region ${REGION}"

# --- S3 bucket --------------------------------------------------------------------------------
if aws s3api head-bucket --bucket "${BUCKET}" 2>/dev/null; then
  echo "Bucket ${BUCKET} already exists"
else
  echo "Creating bucket ${BUCKET}"
  if [[ ${REGION} == us-east-1 ]]; then
    aws s3api create-bucket --bucket "${BUCKET}" >/dev/null
  else
    aws s3api create-bucket --bucket "${BUCKET}" \
      --create-bucket-configuration LocationConstraint="${REGION}" >/dev/null
  fi
  aws s3api put-public-access-block --bucket "${BUCKET}" --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# --- Step Functions execution role ------------------------------------------------------------
TRUST='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"states.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
# Invoke the case-study Lambdas and the core-banking stub behind API Gateway, in this account and region only.
POLICY=$(cat <<JSON
{"Version":"2012-10-17","Statement":[
  {"Effect":"Allow","Action":"lambda:InvokeFunction","Resource":"arn:aws:lambda:${REGION}:${ACCOUNT}:function:*"},
  {"Effect":"Allow","Action":"execute-api:Invoke","Resource":"arn:aws:execute-api:${REGION}:${ACCOUNT}:*"}
]}
JSON
)

if aws iam get-role --role-name "${ROLE}" >/dev/null 2>&1; then
  echo "Role ${ROLE} already exists"
else
  echo "Creating role ${ROLE}"
  aws iam create-role --role-name "${ROLE}" --assume-role-policy-document "$TRUST" \
    --description "Step Functions execution role for the OmniFlow case study" >/dev/null
fi
aws iam put-role-policy --role-name "${ROLE}" --policy-name OmniFlowInvoke --policy-document "$POLICY"
ROLE_ARN=$(aws iam get-role --role-name "${ROLE}" --query Role.Arn --output text)

cat > "$HERE/aws-case.env" <<ENV
AWS_REGION=${REGION}
AWS_ACCOUNT_ID=${ACCOUNT}
QUICKFAAS_BUCKET=${BUCKET}
STEP_FUNCTIONS_ROLE_ARN=${ROLE_ARN}
ENV
echo "--- aws-case.env ---"; cat "$HERE/aws-case.env"
