package costaber.com.github.omniflow

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleCloudDeployer
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleDeployContext
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.internalfunction.quickfaas.QuickFaasDeployer
import costaber.com.github.omniflow.model.HttpMethod.*
import java.nio.file.Path
import java.util.Scanner

// ---------------------------------------------------------------------------
//  Main entry-point — interactive menu
// ---------------------------------------------------------------------------

fun main() {
    val scan = Scanner(System.`in`)
    when (getSelectedOption(scan)) {
        2 -> deployQuickFaasAutoDeployExample(scan)
        3 -> deployGreetingExample(scan)
        4 -> deployOrderValidationExample(scan)
        5 -> deployMapReduceExample(scan)
    }
}

fun getSelectedOption(scan: Scanner): Int {
    println()
    println("=== OmniFlow + QuickFaaS — Example Workflows ===")
    println()
    var option: Int
    do {
        println(" 2: Example 2 — QuickFaaS auto-deploy (single function)")
        println(" 3: Example 3 — Greeting with query parameters (QuickFaaS)")
        println(" 4: Example 4 — Order validation with conditional branching (QuickFaaS)")
        println(" 5: Example 5 — MapReduce with parallel iteration (registry functions)")
        println("99: Exit")
        print("Choose an option: ")
        option = scan.nextInt()
    } while (option !in listOf(2, 3, 4, 5, 99))
    return option
}

// ---------------------------------------------------------------------------
//  Shared helpers — collect GCP config from env vars / interactive prompts
// ---------------------------------------------------------------------------

data class GcpConfig(
    val quickFaasJarPath: String,
    val projectId: String,
    val region: String,
    val serviceAccountEmail: String,
)

private fun collectGcpConfig(scan: Scanner): GcpConfig? {
    val googleCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (googleCredentials.isNullOrBlank()) {
        println("WARNING: GOOGLE_APPLICATION_CREDENTIALS is not set.")
        println("Google Cloud SDK requires this env var BEFORE starting the JVM.")
        println("  Linux/Mac: export GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa-key.json")
        println("  Windows:   set GOOGLE_APPLICATION_CREDENTIALS=C:\\path\\to\\sa-key.json")
        println()
        print("Continue anyway? (y/N): ")
        if (!scan.next().trim().equals("y", ignoreCase = true)) return null
    } else {
        println("GOOGLE_APPLICATION_CREDENTIALS: $googleCredentials")
    }

    val quickFaasJarPath = System.getenv("QUICKFAAS_JAR_PATH")
        ?: run {
            print("Enter path to QuickFaaS-Deployment JAR (QUICKFAAS_JAR_PATH): ")
            scan.next().trim()
        }
    require(Path.of(quickFaasJarPath).toFile().exists()) {
        "QuickFaaS JAR not found at '$quickFaasJarPath'"
    }

    val projectId = System.getenv("GOOGLE_PROJECT_ID")
        ?: run {
            print("Enter GCP Project ID [workflow-test-omniflow]: ")
            scan.next().trim().ifEmpty { "workflow-test-omniflow" }
        }

    val region = System.getenv("GOOGLE_ZONE")
        ?: run {
            print("Enter GCP region [europe-west1]: ")
            scan.next().trim().ifEmpty { "europe-west1" }
        }

    val serviceAccountEmail = System.getenv("GOOGLE_SERVICE_ACCOUNT")
        ?: run {
            print("Enter service account email [workflow-test@$projectId.iam.gserviceaccount.com]: ")
            scan.next().trim().ifEmpty { "workflow-test@$projectId.iam.gserviceaccount.com" }
        }

    return GcpConfig(quickFaasJarPath, projectId, region, serviceAccountEmail)
}

private fun buildQuickFaasDeployer(config: GcpConfig): GoogleCloudDeployer =
    GoogleCloudDeployer.Builder()
        .internalFunctionDeployer(
            QuickFaasDeployer(
                quickFaasJarPath = Path.of(config.quickFaasJarPath),
                projectId = config.projectId,
                region = config.region,
            )
        )
        .build()

private fun buildDeployContext(
    config: GcpConfig,
    workflowId: String,
    workflowDescription: String,
): GoogleDeployContext = GoogleDeployContext(
    projectId = config.projectId,
    zone = config.region,
    serviceAccount = "projects/${config.projectId}/serviceAccounts/${config.serviceAccountEmail}",
    workflowId = workflowId,
    workflowDescription = workflowDescription,
    workflowLabels = mapOf("environment" to "testing", "app" to "omni-flow"),
)

// ===========================================================================
//  Example 2 — QuickFaaS auto-deploy (single function)
//
//  The function "quickfaas-test-fn" does NOT exist yet. OmniFlow triggers
//  QuickFaaS to compile, upload, and deploy it as a Cloud Function before
//  deploying the workflow.
// ===========================================================================

private val Example2Workflow = workflow {
    name("QuickFaasAutoDeployExample")
    description("Auto-deploys an internal function via QuickFaaS, then calls it")
    steps(
        step {
            name("call-auto-deployed-function")
            description("Calls a function that QuickFaaS will deploy if not already running")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "quickfaas-test-fn",
                        deploymentDescriptorPath = "./functions/quickfaas-test-fn/func-deployment.json"
                    )
                    authentication(authentication { type("OIDC") })
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("result")
}

fun deployQuickFaasAutoDeployExample(scan: Scanner) {
    println()
    println("--- Example 2: QuickFaaS Auto-Deploy ---")
    println("Deploys a single Cloud Function via QuickFaaS, then deploys a workflow that calls it.")
    println()

    val config = collectGcpConfig(scan) ?: return
    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "QuickFaasAutoDeployExample", "Example 2: auto-deploy via QuickFaaS")

    println()
    println("Deploying...")
    deployer.deploy(Example2Workflow, context)
    println("Example 2 deployed successfully!")
}

// ===========================================================================
//  Example 3 — Greeting with query parameters (QuickFaaS)
//
//  Auto-deploys a multilingual greeting function, then calls it with
//  ?lang=pt to get a Portuguese greeting.
// ===========================================================================

private val Example3Workflow = workflow {
    name("GreetingWorkflow")
    description("Auto-deploys a greeting function via QuickFaaS and calls it with a query parameter")
    steps(
        step {
            name("greet-portuguese")
            description("Call the greeting function with lang=pt")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "greeting-fn",
                        deploymentDescriptorPath = "./functions/greeting-fn/func-deployment.json"
                    )
                    query("lang" to value("pt"))
                    authentication(authentication { type("OIDC") })
                    result("greetingResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("greetingResult")
}

fun deployGreetingExample(scan: Scanner) {
    println()
    println("--- Example 3: Greeting with Query Parameters ---")
    println("Auto-deploys a multilingual greeting function, calls it with ?lang=pt.")
    println()

    val config = collectGcpConfig(scan) ?: return
    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "GreetingWorkflow", "Example 3: greeting with query params")

    println()
    println("Deploying...")
    deployer.deploy(Example3Workflow, context)
    println("Example 3 deployed successfully!")
}

// ===========================================================================
//  Example 4 — Order validation with conditional branching (QuickFaaS)
//
//  Auto-deploys a validation function. The workflow:
//    1. init       — set orderStatus = "pending"
//    2. validate   — call validate-order-fn with ?amount=<input>
//    3. check      — switch: if valid → approve, else → reject
//    4. approve    — set orderStatus = "approved", jump to done
//    5. reject     — set orderStatus = "rejected"
//    6. done       — return orderStatus
// ===========================================================================

private val Example4Workflow = workflow {
    name("OrderValidationWorkflow")
    description("Validates an order amount via auto-deployed function, branches on the result")
    params("input")
    steps(
        step {
            name("init")
            description("Initialize variables")
            context(
                assign {
                    variables(
                        variable("orderStatus") equalTo value("pending")
                    )
                }
            )
        },
        step {
            name("validate-order")
            description("Call the validation function with the order amount")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "validate-order-fn",
                        deploymentDescriptorPath = "./functions/validate-order-fn/func-deployment.json"
                    )
                    query("amount" to variable("input.amount"))
                    authentication(authentication { type("OIDC") })
                    result("validationResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("check-validation")
            description("Branch based on validation result")
            context(
                switch {
                    conditions(
                        condition {
                            match(variable("validationResult.body.valid") equalTo value(true))
                            jump("approve-order")
                        }
                    )
                    default("reject-order")
                }
            )
        },
        step {
            name("approve-order")
            description("Mark order as approved")
            context(
                assign {
                    variables(
                        variable("orderStatus") equalTo value("approved")
                    )
                }
            )
            next("done")
        },
        step {
            name("reject-order")
            description("Mark order as rejected")
            context(
                assign {
                    variables(
                        variable("orderStatus") equalTo value("rejected")
                    )
                }
            )
        },
        step {
            name("done")
            description("Return final status")
            context(
                assign {
                    variables(
                        variable("finalResult") equalTo variable("orderStatus")
                    )
                }
            )
        }
    )
    result("finalResult")
}

fun deployOrderValidationExample(scan: Scanner) {
    println()
    println("--- Example 4: Order Validation with Conditional Branching ---")
    println("Auto-deploys a validation function. Workflow branches based on amount validity.")
    println("  Input:  {\"amount\": 500}   -> approved")
    println("  Input:  {\"amount\": 99999} -> rejected")
    println()

    val config = collectGcpConfig(scan) ?: return
    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "OrderValidationWorkflow", "Example 4: order validation + branching")

    println()
    println("Deploying...")
    deployer.deploy(Example4Workflow, context)
    println("Example 4 deployed successfully!")
}

// ===========================================================================
//  Example 5 — MapReduce with parallel iteration (registry functions)
//
//  Uses three already-deployed Cloud Run functions from function-registry.json:
//    - splitter: splits input text into words
//    - map:      returns the length of a word
//    - reduce:   sums all lengths
//
//  The workflow:
//    1. init         — initialize empty lengths map
//    2. call_split   — POST to splitter with input text
//    3. parallel_map — iterate words in parallel, call map for each
//    4. callReduce   — POST to reduce with the collected lengths
// ===========================================================================

private val Example5Workflow = workflow {
    name("MapReduceWorkflow")
    description("MapReduce: split text, map word lengths in parallel, reduce to total")
    params("input")
    steps(
        step {
            name("init")
            description("Initialize variables")
            context(
                assign {
                    variables(
                        variable("lengths") equalTo value(mapOf<String, Int>())
                    )
                }
            )
        },
        step {
            name("call-split")
            description("Split input text into words")
            context(
                call {
                    method(POST)
                    internalFunction("splitter")
                    header("Content-Type" to value("application/json"))
                    body(mapOf("text" to variable("input.text")))
                    result("splitResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("parallel-map")
            description("Map each word to its length in parallel")
            context(
                parallel {
                    iteration {
                        key("word")
                        forEach(variable("splitResult.body.words"))
                        steps(
                            step {
                                name("callMap")
                                description("Get word length")
                                context(
                                    call {
                                        method(POST)
                                        internalFunction("map")
                                        header("Content-Type" to value("application/json"))
                                        body(mapOf("name" to variable("word")))
                                        result("mapResult")
                                        resultType(ResultType.BODY)
                                    }
                                )
                            },
                            step {
                                name("store-length")
                                description("Store the word length")
                                context(
                                    assign {
                                        variables(
                                            variable("lengths").withKey("word") equalTo variable("mapResult.length")
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            )
        },
        step {
            name("call-reduce")
            description("Sum all word lengths")
            context(
                call {
                    method(POST)
                    internalFunction("reduce")
                    header("Content-Type" to value("application/json"))
                    body(mapOf("lengths" to variable("lengths")))
                    result("sumResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("sumResult")
}

fun deployMapReduceExample(scan: Scanner) {
    println()
    println("--- Example 5: MapReduce with Parallel Iteration ---")
    println("Uses already-deployed Cloud Run functions (splitter, map, reduce).")
    println("  Input: {\"text\": \"Hello World Omniflow\"}")
    println("  Flow:  split -> parallel map (word lengths) -> reduce (sum)")
    println()

    val config = collectGcpConfig(scan) ?: return

    val deployer = GoogleCloudDeployer.Builder().build()
    val context = buildDeployContext(config, "MapReduceWorkflow", "Example 5: MapReduce parallel iteration")

    println()
    println("Deploying...")
    deployer.deploy(Example5Workflow, context)
    println("Example 5 deployed successfully!")
}
