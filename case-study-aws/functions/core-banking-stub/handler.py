import base64
import json


# Stand-in for the core banking system, behind an API Gateway HTTP API: acknowledges the decision
# (query parameter) for the transaction it receives as the JSON body.
def handler(event, context):
    body = event.get("body") or "{}"
    if event.get("isBase64Encoded"):
        body = base64.b64decode(body).decode()
    transaction = json.loads(body)
    decision = (event.get("queryStringParameters") or {}).get("decision")
    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps({"recorded": True, "id": transaction.get("id"), "decision": decision}),
    }
