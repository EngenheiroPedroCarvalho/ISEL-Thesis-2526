import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleCloudDeployer
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleDeployContext
import costaber.com.github.omniflow.cloud.provider.google.provider.GoogleDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleRenderingContext
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleTermContext
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.internalfunction.quickfaas.QuickFaasDeployer
import costaber.com.github.omniflow.model.HttpMethod.*
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.resource.util.joinToStringNewLines
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
import java.nio.file.Path

// Case-study workflow (Listing 5.1) as run against GCP: region-qualified internal references,
// transaction fields passed as query parameters, and the core-banking call aimed at a stub.
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
        internalFunction("europe-west1/fraud-check", "./functions/fraud-check/func-deployment.json")
        query("amount" to variable("transaction.amount"), "country" to variable("transaction.country"))
        result("fraudResult")
      })
    },
    step {
      name("risk-score")
      description("Score the transaction from its features")
      context(call {
        method(POST)
        internalFunction("europe-west1/risk-score", "./functions/risk-score/func-deployment.json")
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

fun render(wf: Workflow) = DepthFirstNodeVisitorTraversor().traverse(
  NodeContextVisitor(createNodeRendererStrategyDecider()), wf,
  GoogleRenderingContext(termContext = GoogleTermContext()))
  .filterNot(String::isEmpty).joinToStringNewLines()

// Usage: render <coreBankingHost> | deploy <coreBankingHost> <workflowId>
fun main(args: Array<String>) {
  val project = "tfm26-509910"
  val region = "europe-west1"
  val sa = "workflow-test@$project.iam.gserviceaccount.com"
  val wf = paymentAuthorization(args[1])
  when (args[0]) {
    "render" -> println(render(wf))
    "deploy" -> GoogleCloudDeployer.Builder()
      .internalFunctionDeployer(QuickFaasDeployer(
        quickFaasJarPath = Path.of("qf/QuickFaaS-Deployment-1.0-fat.jar").toAbsolutePath(),
        projectId = project, region = region, invokerServiceAccount = sa))
      .build()
      .deploy(wf, GoogleDeployContext(
        projectId = project, zone = region,
        serviceAccount = "projects/$project/serviceAccounts/$sa",
        workflowId = args[2],
        workflowDescription = "Case study: real-time fraud detection for card payments",
        workflowLabels = mapOf("app" to "omni-flow", "case-study" to "payment-authorization")))
  }
}
