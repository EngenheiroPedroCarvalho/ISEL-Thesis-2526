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
//  ANSI color helpers
// ---------------------------------------------------------------------------

private object C {
    const val RESET   = "[0m"
    const val BOLD    = "[1m"
    const val RED     = "[31m"
    const val GREEN   = "[32m"
    const val YELLOW  = "[33m"
    const val BLUE    = "[34m"
    const val MAGENTA = "[35m"
    const val CYAN    = "[36m"
    const val WHITE   = "[37m"

    fun stage(n: Int, total: Int, msg: String) =
        println("${CYAN}${BOLD}[STAGE $n/$total]${RESET}${CYAN} $msg${RESET}")

    fun ok(msg: String)      = println("  ${GREEN}✓${RESET} $msg")
    fun info(msg: String)    = println("  ${BLUE}→${RESET} $msg")
    fun warn(msg: String)    = println("  ${YELLOW}!${RESET} $msg")
    fun err(msg: String)     = println("  ${RED}✗${RESET} $msg")
    fun detail(msg: String)  = println("  ${WHITE}  $msg${RESET}")

    fun banner(title: String) {
        val line = "═".repeat(title.length + 4)
        println()
        println("${MAGENTA}${BOLD}╔${line}╗${RESET}")
        println("${MAGENTA}${BOLD}║  $title  ║${RESET}")
        println("${MAGENTA}${BOLD}╚${line}╝${RESET}")
        println()
    }

    fun separator() {
        println("${WHITE}${"─".repeat(60)}${RESET}")
    }

    fun success(msg: String) {
        println()
        println("${GREEN}${BOLD}  ✓ $msg${RESET}")
        println()
    }

    fun failure(msg: String) {
        println()
        println("${RED}${BOLD}  ✗ $msg${RESET}")
        println()
    }
}

// ---------------------------------------------------------------------------
//  Main entry-point — interactive menu
// ---------------------------------------------------------------------------

fun main() {
    val scan = Scanner(System.`in`)
    C.banner("OmniFlow + QuickFaaS — Example Workflows")
    when (getSelectedOption(scan)) {
        2 -> deployQuickFaasAutoDeployExample(scan)
        3 -> deployGreetingExample(scan)
        4 -> deployLoanApprovalExample(scan)
        5 -> deployMapReduceExample(scan)
    }
}

fun getSelectedOption(scan: Scanner): Int {
    var option: Int
    do {
        println("  ${C.CYAN}2${C.RESET}: QuickFaaS auto-deploy (single function)")
        println("  ${C.CYAN}3${C.RESET}: Greeting with query parameters (QuickFaaS)")
        println("  ${C.CYAN}4${C.RESET}: Loan approval with conditional branching (QuickFaaS)")
        println("  ${C.CYAN}5${C.RESET}: Text analysis with parallel iteration (QuickFaaS — 3 functions)")
        println(" ${C.RED}99${C.RESET}: Exit")
        println()
        print("  ${C.BOLD}Choose an option:${C.RESET} ")
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

private fun collectGcpConfig(scan: Scanner, stageTotal: Int): GcpConfig? {
    C.stage(1, stageTotal, "Configuration")

    val googleCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (googleCredentials.isNullOrBlank()) {
        C.warn("GOOGLE_APPLICATION_CREDENTIALS is not set.")
        C.detail("Google Cloud SDK requires this env var BEFORE starting the JVM.")
        C.detail("  Linux/Mac: export GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa-key.json")
        C.detail("  Windows:   set GOOGLE_APPLICATION_CREDENTIALS=C:\\path\\to\\sa-key.json")
        println()
        print("  Continue anyway? (y/N): ")
        if (!scan.next().trim().equals("y", ignoreCase = true)) return null
    } else {
        C.ok("GOOGLE_APPLICATION_CREDENTIALS set")
    }

    val quickFaasJarPath = System.getenv("QUICKFAAS_JAR_PATH")
        ?: run {
            print("  Enter path to QuickFaaS-Deployment JAR: ")
            scan.next().trim()
        }
    require(Path.of(quickFaasJarPath).toFile().exists()) {
        "QuickFaaS JAR not found at '$quickFaasJarPath'"
    }
    C.ok("QuickFaaS JAR found")

    val projectId = System.getenv("GOOGLE_PROJECT_ID")
        ?: run {
            print("  Enter GCP Project ID [workflow-test-omniflow]: ")
            scan.next().trim().ifEmpty { "workflow-test-omniflow" }
        }

    val region = System.getenv("GOOGLE_ZONE")
        ?: run {
            print("  Enter GCP region [europe-west1]: ")
            scan.next().trim().ifEmpty { "europe-west1" }
        }

    val serviceAccountEmail = System.getenv("GOOGLE_SERVICE_ACCOUNT")
        ?: run {
            print("  Enter service account email [workflow-test@$projectId.iam.gserviceaccount.com]: ")
            scan.next().trim().ifEmpty { "workflow-test@$projectId.iam.gserviceaccount.com" }
        }

    C.ok("Project: ${C.BOLD}$projectId${C.RESET}")
    C.ok("Region:  ${C.BOLD}$region${C.RESET}")
    println()

    return GcpConfig(quickFaasJarPath, projectId, region, serviceAccountEmail)
}

private fun buildQuickFaasDeployer(config: GcpConfig): GoogleCloudDeployer =
    GoogleCloudDeployer.Builder()
        .internalFunctionDeployer(
            QuickFaasDeployer(
                quickFaasJarPath = Path.of(config.quickFaasJarPath),
                projectId = config.projectId,
                region = config.region,
                invokerServiceAccount = config.serviceAccountEmail,
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
    val stages = 3
    C.banner("Example 2 — QuickFaaS Auto-Deploy")
    C.info("Deploys a single Cloud Function via QuickFaaS, then deploys a workflow that calls it.")
    println()

    val config = collectGcpConfig(scan, stages) ?: return

    C.stage(2, stages, "Function deployment + Workflow resolution")
    C.info("Deploying ${C.BOLD}quickfaas-test-fn${C.RESET} via QuickFaaS...")
    C.info("Resolving internal functions and rendering workflow YAML...")

    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "QuickFaasAutoDeployExample", "Example 2: auto-deploy via QuickFaaS")
    deployer.deploy(Example2Workflow, context)

    C.ok("Function deployed and workflow rendered")
    println()

    C.stage(3, stages, "Complete")
    C.success("Example 2 deployed successfully!")
    C.separator()
    C.detail("Workflow ID: ${C.BOLD}QuickFaasAutoDeployExample${C.RESET}")
    C.separator()
}

// ===========================================================================
//  Example 3 — Greeting with query parameters (QuickFaaS)
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
    val stages = 3
    C.banner("Example 3 — Greeting with Query Parameters")
    C.info("Auto-deploys a multilingual greeting function, calls it with ${C.BOLD}?lang=pt${C.RESET}.")
    println()

    val config = collectGcpConfig(scan, stages) ?: return

    C.stage(2, stages, "Function deployment + Workflow resolution")
    C.info("Deploying ${C.BOLD}greeting-fn${C.RESET} via QuickFaaS...")
    C.info("Resolving internal functions and rendering workflow YAML...")

    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "GreetingWorkflow", "Example 3: greeting with query params")
    deployer.deploy(Example3Workflow, context)

    C.ok("Function deployed and workflow rendered")
    println()

    C.stage(3, stages, "Complete")
    C.success("Example 3 deployed successfully!")
    C.separator()
    C.detail("Workflow ID: ${C.BOLD}GreetingWorkflow${C.RESET}")
    C.separator()
}

// ===========================================================================
//  Example 4 — Loan Approval with conditional branching (QuickFaaS)
//
//  Scenario: A bank customer submits a loan application with their annual
//  income and requested loan amount. The system auto-deploys a loan
//  evaluator function that applies the 5x-income rule, then the workflow
//  branches on the result: approved or rejected.
//
//  Workflow steps:
//    1. receive-application — set applicationStatus = "under_review"
//    2. evaluate-loan       — call loan-evaluator-fn(?income=X&loanAmount=Y)
//    3. parse-evaluation    — json.decode the response body
//    4. decision            — switch: approved → approve, else → reject
//    5. approve-loan        — applicationStatus = "approved"
//    6. reject-loan         — applicationStatus = "rejected"
//    7. finalize            — return decision with status and reason
// ===========================================================================

private val Example4Workflow = workflow {
    name("LoanApprovalWorkflow")
    description("Evaluates a loan application and auto-approves or rejects based on debt-to-income ratio")
    params("input")
    steps(
        step {
            name("receive-application")
            description("Register the loan application and mark it as under review")
            context(
                assign {
                    variables(
                        variable("applicationStatus") equalTo value("under_review")
                    )
                }
            )
        },
        step {
            name("evaluate-loan")
            description("Send the application to the loan evaluator function")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "loan-evaluator-fn",
                        deploymentDescriptorPath = "./functions/loan-evaluator-fn/func-deployment.json"
                    )
                    query(
                        "income" to variable("input.income"),
                        "loanAmount" to variable("input.loanAmount")
                    )
                    authentication(authentication { type("OIDC") })
                    result("evaluationResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("parse-evaluation")
            description("Parse the JSON response body into a dictionary")
            context(
                assign {
                    variables(
                        variable("evaluation") equalTo variable("json.decode(evaluationResult.body)")
                    )
                }
            )
        },
        step {
            name("decision")
            description("Branch based on the loan evaluator's decision")
            context(
                switch {
                    conditions(
                        condition {
                            match(variable("evaluation.approved") equalTo value(true))
                            jump("approve-loan")
                        }
                    )
                    default("reject-loan")
                }
            )
        },
        step {
            name("approve-loan")
            description("Mark the loan application as approved")
            context(
                assign {
                    variables(
                        variable("applicationStatus") equalTo value("approved")
                    )
                }
            )
            next("finalize")
        },
        step {
            name("reject-loan")
            description("Mark the loan application as rejected")
            context(
                assign {
                    variables(
                        variable("applicationStatus") equalTo value("rejected")
                    )
                }
            )
        },
        step {
            name("finalize")
            description("Build the final decision result")
            context(
                assign {
                    variables(
                        variable("result") equalTo variable("applicationStatus")
                    )
                }
            )
        }
    )
    result("result")
}

fun deployLoanApprovalExample(scan: Scanner) {
    val stages = 3
    C.banner("Example 4 — Loan Approval Workflow")
    println("  ${C.WHITE}Scenario: A bank customer submits a loan application.${C.RESET}")
    println("  ${C.WHITE}The system evaluates it against the 5x-income rule${C.RESET}")
    println("  ${C.WHITE}and auto-approves or rejects the application.${C.RESET}")
    println()
    C.separator()
    println("  ${C.YELLOW}Workflow steps:${C.RESET}")
    C.detail("1. ${C.BOLD}receive-application${C.RESET}  — mark status as \"under_review\"")
    C.detail("2. ${C.BOLD}evaluate-loan${C.RESET}        — call loan-evaluator-fn")
    C.detail("3. ${C.BOLD}parse-evaluation${C.RESET}     — json.decode response body")
    C.detail("4. ${C.BOLD}decision${C.RESET}             — switch: approved? -> approve / reject")
    C.detail("5. ${C.BOLD}approve-loan${C.RESET}         — set status = \"approved\"")
    C.detail("   ${C.BOLD}reject-loan${C.RESET}          — set status = \"rejected\"")
    C.detail("6. ${C.BOLD}finalize${C.RESET}             — return decision")
    C.separator()
    println()

    val config = collectGcpConfig(scan, stages) ?: return

    C.stage(2, stages, "Function deployment + Workflow resolution")
    C.info("Deploying ${C.BOLD}loan-evaluator-fn${C.RESET} via QuickFaaS...")
    C.info("Function applies the 5x-income rule: maxLoan = income * 5")
    C.info("Resolving internal functions and rendering workflow YAML...")

    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "LoanApprovalWorkflow", "Loan approval with conditional branching")
    deployer.deploy(Example4Workflow, context)

    C.ok("Function deployed and workflow rendered")
    println()

    C.stage(3, stages, "Complete")
    C.success("Loan Approval Workflow deployed successfully!")
    C.separator()
    C.detail("Workflow ID: ${C.BOLD}LoanApprovalWorkflow${C.RESET}")
    println()
    C.detail("${C.GREEN}Try (approved):${C.RESET}  {\"income\": 50000, \"loanAmount\": 150000}")
    C.detail("${C.RED}Try (rejected):${C.RESET}  {\"income\": 50000, \"loanAmount\": 500000}")
    C.separator()
}

// ===========================================================================
//  Example 5 — Text Analysis with parallel iteration (QuickFaaS auto-deploy)
//
//  All three functions are auto-deployed via QuickFaaS:
//    - splitter-fn:    GET ?text=...     → {"words": [...]}
//    - word-length-fn: GET ?word=...     → {"word": "...", "length": N}
//    - word-count-fn:  GET ?values=5,3,8 → {"totalLength": 16, "wordCount": 3, ...}
//
//  Workflow:
//    1. call-split       — split input text into words
//    2. parse-split      — json.decode the response
//    3. parallel-map     — for each word, call word-length-fn in parallel
//    4. build-values     — concatenate lengths into comma-separated string
//    5. call-reduce      — call word-count-fn with the values string
//    6. parse-reduce     — json.decode the response
// ===========================================================================

private val Example5Workflow = workflow {
    name("TextAnalysisWorkflow")
    description("Split text, map word lengths in parallel, reduce to summary — all via QuickFaaS")
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
                    method(GET)
                    internalFunction(
                        "splitter-fn",
                        deploymentDescriptorPath = "./functions/splitter-fn/func-deployment.json"
                    )
                    query("text" to variable("input.text"))
                    authentication(authentication { type("OIDC") })
                    result("splitResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("parse-split")
            description("Parse the split response body")
            context(
                assign {
                    variables(
                        variable("splitData") equalTo variable("json.decode(splitResult.body)")
                    )
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
                        forEach(variable("splitData.words"))
                        steps(
                            step {
                                name("call-word-length")
                                description("Get word length")
                                context(
                                    call {
                                        method(GET)
                                        internalFunction(
                                            "word-length-fn",
                                            deploymentDescriptorPath = "./functions/word-length-fn/func-deployment.json"
                                        )
                                        query("word" to variable("word"))
                                        authentication(authentication { type("OIDC") })
                                        result("lengthResult")
                                        resultType(ResultType.BODY)
                                    }
                                )
                            },
                            step {
                                name("parse-length")
                                description("Parse the length response")
                                context(
                                    assign {
                                        variables(
                                            variable("parsedLength") equalTo variable("json.decode(lengthResult.body)")
                                        )
                                    }
                                )
                            },
                            step {
                                name("store-length")
                                description("Store the word length")
                                context(
                                    assign {
                                        variables(
                                            variable("lengths").withKey("word") equalTo variable("parsedLength.length")
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
            name("build-values")
            description("Build a comma-separated string of lengths for the reduce function")
            context(
                assign {
                    variables(
                        variable("valuesStr") equalTo variable("text.join(\",\", map.values(lengths))")
                    )
                }
            )
        },
        step {
            name("call-reduce")
            description("Call the reduce function with the accumulated lengths")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "word-count-fn",
                        deploymentDescriptorPath = "./functions/word-count-fn/func-deployment.json"
                    )
                    query("values" to variable("valuesStr"))
                    authentication(authentication { type("OIDC") })
                    result("reduceResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("parse-reduce")
            description("Parse the reduce response")
            context(
                assign {
                    variables(
                        variable("summary") equalTo variable("json.decode(reduceResult.body)")
                    )
                }
            )
        }
    )
    result("summary")
}

fun deployMapReduceExample(scan: Scanner) {
    val stages = 3
    C.banner("Example 5 — Text Analysis with Parallel Iteration")
    C.info("All functions auto-deployed via QuickFaaS:")
    C.detail("${C.BOLD}splitter-fn${C.RESET}    — splits input text into words")
    C.detail("${C.BOLD}word-length-fn${C.RESET} — returns the length of a word")
    C.detail("${C.BOLD}word-count-fn${C.RESET}  — sums lengths and computes average")
    println()
    C.separator()
    println("  ${C.YELLOW}Flow:${C.RESET}  split → parallel map (word lengths) → reduce (summary)")
    C.separator()
    println()

    val config = collectGcpConfig(scan, stages) ?: return

    C.stage(2, stages, "Function deployment + Workflow resolution")
    C.info("Deploying ${C.BOLD}splitter-fn${C.RESET}, ${C.BOLD}word-length-fn${C.RESET}, ${C.BOLD}word-count-fn${C.RESET} via QuickFaaS...")
    C.info("Resolving internal functions and rendering workflow YAML...")

    val deployer = buildQuickFaasDeployer(config)
    val context = buildDeployContext(config, "TextAnalysisWorkflow", "Example 5: text analysis with parallel iteration")
    deployer.deploy(Example5Workflow, context)

    C.ok("All functions deployed and workflow rendered")
    println()

    C.stage(3, stages, "Complete")
    C.success("Text Analysis Workflow deployed successfully!")
    C.separator()
    C.detail("Workflow ID: ${C.BOLD}TextAnalysisWorkflow${C.RESET}")
    C.detail("Execute with: {\"text\": \"Hello World Omniflow\"}")
    C.separator()
}
