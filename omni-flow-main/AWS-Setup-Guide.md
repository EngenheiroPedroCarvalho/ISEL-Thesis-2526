# AWS Setup Guide — OmniFlow Step Functions Deployment

## 1. Create IAM User

1. **IAM** -> **Users** -> **Create user**
2. Name: `omniflow-quickfaas`
3. **Attach policies directly**:
   - `AWSStepFunctionsFullAccess`
4. **Create user**
5. Go to **Security credentials** -> **Create access key** -> **CLI**
6. Save:
   ```
   AWS_ACCESS_KEY_ID=AKIA...
   AWS_SECRET_ACCESS_KEY=wJalr...
   ```

## 2. Create Step Functions Execution Role

1. **IAM** -> **Roles** -> **Create role**
2. **Trusted entity**: AWS Service -> **Step Functions**
3. Click **Next** (default policy is fine)
4. Role name: `StepFunctionsExecutionRole`
5. **Create role**
6. Save the Role ARN: `arn:aws:iam::<account-id>:role/StepFunctionsExecutionRole`

## 3. Add Inline Policies

### 3.1 On the USER (`omniflow-quickfaas`):

**IAM** -> **Users** -> **omniflow-quickfaas** -> **Add permissions** -> **Create inline policy** -> **JSON**

Policy name: `AllowPassStepFunctionsRole`
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "iam:PassRole",
            "Resource": "arn:aws:iam::<account-id>:role/StepFunctionsExecutionRole"
        }
    ]
}
```

### 3.2 On the ROLE (`StepFunctionsExecutionRole`):

**IAM** -> **Roles** -> **StepFunctionsExecutionRole** -> **Add permissions** -> **Create inline policy** -> **JSON**

Policy name: `AllowInvokeApiGateway`
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "execute-api:Invoke",
            "Resource": "arn:aws:execute-api:*:<account-id>:*"
        }
    ]
}
```

> **IMPORTANT**: If the role has a **Permissions Boundary**, it must also include `execute-api:Invoke`. 
> Either remove the boundary or edit it to include the action.

## 4. Create Lambda Function

1. **Lambda** -> **Create function**
2. **Author from scratch**
3. Name: `greeting-fn`
4. Runtime: **Node.js 20.x**
5. Architecture: **x86_64**
6. **Create function**
7. In the inline editor, replace the code with:

```javascript
export const handler = async (event) => {
    const lang = event.queryStringParameters?.lang || "en";
    
    const greetings = {
        pt: "Ola, Mundo!",
        es: "Hola, Mundo!",
        fr: "Bonjour, le Monde!",
        en: "Hello, World!"
    };

    return {
        statusCode: 200,
        body: JSON.stringify({
            greeting: greetings[lang] || greetings.en,
            language: lang
        })
    };
};
```

8. Click **Deploy**

## 5. Create API Gateway

1. **API Gateway** -> **Create API** -> **REST API** -> **Build**
2. API name: `omniflow-api`
3. **Create API**
4. **Create resource**: Resource path = `/greeting`
5. On `/greeting`: **Create method** -> **GET**
   - Integration type: **Lambda Function**
   - Lambda function: `greeting-fn`
   - **Create method**
6. Click **Deploy API**
   - Stage name: `prod`
7. Copy the **Invoke URL**, e.g.:
   ```
   https://abc123.execute-api.us-east-1.amazonaws.com/prod/greeting
   ```

## 6. Set Environment Variables

```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=wJalr...
export AWS_REGION=us-east-1
```

## 7. Run OmniFlow Example 6

Run `Main.kt`, choose option **6**, and provide:

| Prompt | Value |
|--------|-------|
| Step Functions IAM Role ARN | `arn:aws:iam::<account-id>:role/StepFunctionsExecutionRole` |
| API Gateway Invoke URL | `https://abc123.execute-api.us-east-1.amazonaws.com/prod/greeting` |

## 8. Test the Step Function

### Via AWS Console:
1. **Step Functions** -> **State machines** -> **AwsGreetingStepFunction**
2. **Start execution**
3. Input: `{}`
4. Click **Start execution**
5. Check the output

### Via AWS CLI:
```bash
aws stepfunctions start-execution \
  --state-machine-arn arn:aws:states:us-east-1:<account-id>:stateMachine:AwsGreetingStepFunction \
  --input '{}' \
  --region us-east-1
```

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `states:CreateStateMachine not authorized` | User missing Step Functions permissions | Add `AWSStepFunctionsFullAccess` to user |
| `iam:PassRole not authorized` | User can't assign role to Step Functions | Add `AllowPassStepFunctionsRole` inline policy to user |
| `execute-api:Invoke not authorized` | Role can't call API Gateway | Add `AllowInvokeApiGateway` inline policy to role |
| `permissions boundary` blocks action | Role has a boundary limiting actions | Remove boundary or add `execute-api:Invoke` to it |
| `No method found matching route /` | Path is empty in state machine JSON | Check the Invoke URL includes `/prod/greeting` |
| `StateMachineAlreadyExists` | Step function was already created | Delete it first, then re-run |
| `NoClassDefFoundError: commons-logging` | Missing dependency | Already fixed in pom.xml |

## Delete Step Function (if needed)

```bash
aws stepfunctions delete-state-machine \
  --state-machine-arn arn:aws:states:us-east-1:<account-id>:stateMachine:AwsGreetingStepFunction \
  --region us-east-1
```
