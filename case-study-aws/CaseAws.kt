import costaber.com.github.omniflow.cloud.provider.amazon.deployer.AmazonCloudDeployer
import costaber.com.github.omniflow.cloud.provider.amazon.deployer.AmazonDeployContext
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.internalfunction.quickfaas.AwsLambdaDeployer
import costaber.com.github.omniflow.model.HttpMethod.*
import java.nio.file.Path

// Case-study workflow (Listing B.2, Appendix B) as run against AWS: region-qualified internal references,
// transaction fields passed as query parameters (the only input an AWS internal call takes), and
// the core-banking call aimed at a stub behind an API Gateway HTTP API.
fun paymentAuthorization(coreBankingHost: String) = workflow {
  name("PaymentAuthorization")
  description("Real-time fraud screening for card payment authorization")
  params("transaction")
  steps(
    step {
      name("fraud-check")
      description("Flag transactions that match known fraud patterns")
      context(call {
        method(POST)
        internalFunction("eu-west-1/fraud-check", "./functions/fraud-check/func-deployment.json")
        query("amount" to variable("transaction.amount"), "country" to variable("transaction.country"))
        result("fraudResult")
      })
    },
    step {
      name("risk-score")
      description("Score the transaction from its features")
      context(call {
        method(POST)
        internalFunction("eu-west-1/risk-score", "./functions/risk-score/func-deployment.json")
        query("amount" to variable("transaction.amount"))
        result("riskResult")
      })
    },
    step {
      name("decision")
      description("Decline flagged or high-risk transactions")
      context(switch {
        conditions(
          condition {
            match(variable("fraudResult").withKey("flagged") equalTo value(true))
            jump("decline-transaction")
          },
          condition {
            match(variable("riskResult").withKey("score") greaterThan value(75))
            jump("decline-transaction")
          }
        )
        default("approve-transaction")
      })
    },
    step {
      name("approve-transaction")
      description("Record the approval")
      context(assign { variables(variable("decision") equalTo value("approved")) })
      next("report-decision")
    },
    step {
      name("decline-transaction")
      description("Record the decline")
      context(assign { variables(variable("decision") equalTo value("declined")) })
    },
    step {
      name("report-decision")
      description("Report the decision to core banking")
      context(call {
        method(POST)
        host(coreBankingHost)
        path("/")
        body(variable("transaction"))
        query("decision" to variable("decision"))
        result("coreBankingResult")
      })
    }
  )
  result("decision")
}

// Usage: deploy <coreBankingHost> <stateMachineName>  (values from aws-case.env in the environment)
fun main(args: Array<String>) {
  val region = System.getenv("AWS_REGION")
  val stepFunctionsRole = System.getenv("STEP_FUNCTIONS_ROLE_ARN")
  AmazonCloudDeployer.Builder()
    .internalFunctionDeployer(AwsLambdaDeployer(
      quickFaasJarPath = Path.of("qf/QuickFaaS-Deployment-1.0-fat.jar").toAbsolutePath(),
      region = region, roleArn = stepFunctionsRole))
    .build()
    .deploy(paymentAuthorization(args[1]), AmazonDeployContext(
      roleArn = stepFunctionsRole, region = region,
      tags = mapOf("app" to "omni-flow", "case-study" to "payment-authorization"),
      stateMachineName = args[2]))
}
