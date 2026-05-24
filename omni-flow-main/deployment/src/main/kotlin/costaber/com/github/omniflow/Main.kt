package costaber.com.github.omniflow

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.cloud.provider.amazon.deployer.AmazonCloudDeployer
import costaber.com.github.omniflow.cloud.provider.amazon.deployer.AmazonDeployContext
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleCloudDeployer
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleDeployContext
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.internalfunction.quickfaas.AwsLambdaDeployer
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
    printPrerequisites()
    when (getSelectedOption(scan)) {
        2 -> deployQuickFaasAutoDeployExample(scan)
        3 -> deployGreetingExample(scan)
        4 -> deployLoanApprovalExample(scan)
        5 -> deployMapReduceExample(scan)
        6 -> deployAwsGreetingExample(scan)
        7 -> deployAwsLambdaAutoDeployExample(scan)
        8 -> deployAwsTextAnalysisExample(scan)
    }
}

fun printPrerequisites() {
    C.separator()
    println("  ${C.BOLD}${C.WHITE}PRE-REQUISITES${C.RESET}")
    C.separator()
    println()

    println("  ${C.CYAN}${C.BOLD}[ Common ]${C.RESET}")
    C.detail("Java 17+ JDK installed and on PATH")
    C.detail("Maven 3.6+ installed and on PATH  (required by QuickFaaS to build JARs)")
    println()

    println("  ${C.CYAN}${C.BOLD}[ GCP Examples — options 2, 3, 4, 5 ]${C.RESET}")
    C.detail("GCP project with the following APIs enabled:")
    C.detail("  • Cloud Functions API")
    C.detail("  • Cloud Workflows API")
    C.detail("  • Cloud Storage API")
    C.detail("Service account with roles:")
    C.detail("  • Cloud Functions Developer")
    C.detail("  • Workflows Admin")
    C.detail("  • Service Account Token Creator")
    C.detail("  • Storage Admin  (only if QuickFaaS needs to create a bucket)")
    C.detail("Environment variables:")
    C.detail("  export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json")
    C.detail("  export QUICKFAAS_JAR_PATH=/path/to/QuickFaaS-Deployment-fat.jar")
    C.detail("  export GOOGLE_PROJECT_ID=your-gcp-project-id          # optional (prompted)")
    C.detail("  export GOOGLE_ZONE=europe-west1                        # optional (prompted)")
    C.detail("  export GOOGLE_SERVICE_ACCOUNT=sa@project.iam...        # optional (prompted)")
    C.detail("Build QuickFaaS JAR:")
    C.detail("  cd quickfaas-essentials/QuickFaaS-Deployment")
    C.detail("  ./gradlew fatJar  →  build/libs/QuickFaaS-Deployment-fat.jar")
    println()

    println("  ${C.YELLOW}${C.BOLD}[ AWS Examples — option 6 ]${C.RESET}")
    C.detail("Lambda function already deployed and exposed via API Gateway")
    C.detail("IAM role for Step Functions with permission: lambda:InvokeFunction")
    C.detail("Environment variables:")
    C.detail("  export AWS_ACCESS_KEY_ID=AKIA...")
    C.detail("  export AWS_SECRET_ACCESS_KEY=wJalr...")
    C.detail("  export AWS_REGION=eu-west-1                            # optional (prompted)")
    println()

    println("  ${C.YELLOW}${C.BOLD}[ AWS QuickFaaS Auto-Deploy — options 7 and 8 ]${C.RESET}")
    C.detail("S3 bucket to store Lambda deployment packages")
    C.detail("IAM role for Lambda execution (trust: lambda.amazonaws.com) with:")
    C.detail("  • AWSLambdaBasicExecutionRole")
    C.detail("IAM role for Step Functions with permission: lambda:InvokeFunction")
    C.detail("func-deployment.json files updated with real values (or left as placeholders):")
    C.detail("  • iamRoleArn   → Lambda execution role ARN (auto-created if blank/placeholder)")
    C.detail("  • project      → AWS account ID  (12-digit number)")
    C.detail("  • bucket       → S3 bucket name")
    C.detail("Environment variables:")
    C.detail("  export AWS_ACCESS_KEY_ID=AKIA...")
    C.detail("  export AWS_SECRET_ACCESS_KEY=wJalr...")
    C.detail("  export AWS_REGION=eu-west-1                            # optional (prompted)")
    C.detail("  export QUICKFAAS_JAR_PATH=/path/to/QuickFaaS-Deployment-fat.jar")
    C.detail("  export STEP_FUNCTIONS_ROLE_ARN=arn:aws:iam::ACCOUNT:role/StepFunctionsRole")
    C.detail("  export AWS_S3_BUCKET=my-lambda-bucket                  # optional (prompted)")
    C.detail("  export AWS_ACCOUNT_ID=123456789012                     # optional (prompted)")
    C.detail("  export STATE_MACHINE_NAME=AwsLambdaAutoDeployGreeting  # optional")
    println()

    C.separator()
    println()
}

fun getSelectedOption(scan: Scanner): Int {
    var option: Int
    do {
        println("  ${C.CYAN}${C.BOLD}— Google Cloud Workflows (GCP) —${C.RESET}")
        println("  ${C.CYAN}2${C.RESET}: QuickFaaS auto-deploy (single function)")
        println("  ${C.CYAN}3${C.RESET}: Greeting with query parameters (QuickFaaS)")
        println("  ${C.CYAN}4${C.RESET}: Loan approval with conditional branching (QuickFaaS)")
        println("  ${C.CYAN}5${C.RESET}: Text analysis with parallel iteration (QuickFaaS — 3 functions)")
        println()
        println("  ${C.YELLOW}${C.BOLD}— AWS Step Functions —${C.RESET}")
        println("  ${C.YELLOW}6${C.RESET}: Greeting via API Gateway + Lambda")
        println("  ${C.YELLOW}7${C.RESET}: Greeting with QuickFaaS auto-deploy (single Lambda)")
        println("  ${C.YELLOW}8${C.RESET}: Text analysis pipeline with QuickFaaS auto-deploy (3 Lambdas)")
        println()
        println(" ${C.RED}99${C.RESET}: Exit")
        println()
        print("  ${C.BOLD}Choose an option:${C.RESET} ")
        option = scan.nextInt()
    } while (option !in listOf(2, 3, 4, 5, 6, 7, 8, 99))
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

// ---------------------------------------------------------------------------
//  Shared helpers — collect AWS config from env vars / interactive prompts
// ---------------------------------------------------------------------------

data class AwsConfig(
    val region: String,
    val roleArn: String,
    val apiGatewayHost: String,
    val apiStagePath: String,
)

private fun collectAwsConfig(scan: Scanner, stageTotal: Int): AwsConfig? {
    C.stage(1, stageTotal, "Configuration")

    val awsAccessKey = System.getenv("AWS_ACCESS_KEY_ID")
    val awsSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
    if (awsAccessKey.isNullOrBlank() || awsSecretKey.isNullOrBlank()) {
        C.warn("AWS_ACCESS_KEY_ID and/or AWS_SECRET_ACCESS_KEY not set.")
        C.detail("AWS SDK requires these env vars BEFORE starting the JVM.")
        C.detail("  Linux/Mac: export AWS_ACCESS_KEY_ID=AKIA...")
        C.detail("             export AWS_SECRET_ACCESS_KEY=wJalr...")
        C.detail("  Windows:   set AWS_ACCESS_KEY_ID=AKIA...")
        C.detail("             set AWS_SECRET_ACCESS_KEY=wJalr...")
        println()
        print("  Continue anyway? (y/N): ")
        if (!scan.next().trim().equals("y", ignoreCase = true)) return null
    } else {
        C.ok("AWS credentials set")
    }

    val region = System.getenv("AWS_REGION")
        ?: run {
            print("  Enter AWS region [eu-west-1]: ")
            scan.next().trim().ifEmpty { "eu-west-1" }
        }

    print("  Enter Step Functions IAM Role ARN: ")
    val roleArn = scan.next().trim()

    print("  Enter API Gateway Invoke URL (e.g. https://abc123.execute-api.$region.amazonaws.com/prod/greeting): ")
    val rawUrl = scan.next().trim()
        .removePrefix("https://")
        .removePrefix("http://")

    val endpointHost = rawUrl.substringBefore("/")
    val apiStagePath = "/" + rawUrl.substringAfter("/", "")

    C.ok("Region:   ${C.BOLD}$region${C.RESET}")
    C.ok("Role:     ${C.BOLD}$roleArn${C.RESET}")
    C.ok("Endpoint: ${C.BOLD}$endpointHost${C.RESET}")
    C.ok("Path:     ${C.BOLD}$apiStagePath${C.RESET}")
    println()

    return AwsConfig(region, roleArn, endpointHost, apiStagePath)
}

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
//    - splitter-fn:    GET ?text=...  → {"words": [...]}
//    - word-length-fn: GET ?word=...  → {"word": "...", "length": N}
//    - word-count-fn:  GET ?text=...  → {"totalLength": N, "wordCount": N, "averageLength": N}
//
//  Workflow:
//    1. call-split       — split input text into words
//    2. parse-split      — json.decode the response
//    3. parallel-map     — for each word, call word-length-fn in parallel
//    4. call-reduce      — call word-count-fn with the original text
//    5. parse-reduce     — json.decode the response
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
            name("call-reduce")
            description("Call the reduce function with the original text")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "word-count-fn",
                        deploymentDescriptorPath = "./functions/word-count-fn/func-deployment.json"
                    )
                    query("text" to variable("input.text"))
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

// ===========================================================================
//  Example 6 — AWS Step Functions: Greeting via API Gateway + Lambda
//
//  Calls a pre-existing Lambda function exposed through API Gateway.
//  The Lambda should accept a query parameter ?lang=... and return a greeting.
//
//  Authentication: IAM_ROLE (Step Functions assumes its execution role)
// ===========================================================================

private fun buildAwsGreetingWorkflow(config: AwsConfig) = workflow {
    name("AwsGreetingStepFunction")
    description("Calls a greeting Lambda via API Gateway from AWS Step Functions")
    steps(
        step {
            name("call-greeting")
            description("Call the greeting Lambda via API Gateway")
            context(
                call {
                    method(GET)
                    host(config.apiGatewayHost)
                    path(config.apiStagePath)
                    query("lang" to value("pt"))
                    authentication(authentication { type("IAM_ROLE") })
                    result("greetingResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("greetingResult")
}

fun deployAwsGreetingExample(scan: Scanner) {
    val stages = 3
    C.banner("Example 6 — AWS Step Functions: Greeting")
    C.info("Deploys a Step Function that calls a greeting Lambda via API Gateway.")
    C.info("The Lambda must already be deployed and exposed via API Gateway.")
    println()

    val config = collectAwsConfig(scan, stages) ?: return

    C.stage(2, stages, "Building state machine definition")
    C.info("Rendering workflow DSL to AWS Step Functions JSON...")

    val deployer = AmazonCloudDeployer.Builder().build()
    val context = AmazonDeployContext(
        roleArn = config.roleArn,
        region = config.region,
        tags = mapOf("environment" to "testing", "app" to "omni-flow"),
        stateMachineName = "AwsGreetingStepFunction",
    )
    val workflow = buildAwsGreetingWorkflow(config)
    deployer.deploy(workflow, context)

    C.ok("State machine definition rendered")
    println()

    C.stage(3, stages, "Complete")
    C.success("AWS Step Function deployed successfully!")
    C.separator()
    C.detail("State Machine: ${C.BOLD}AwsGreetingStepFunction${C.RESET}")
    C.detail("Region:        ${C.BOLD}${config.region}${C.RESET}")
    C.separator()
}

// ===========================================================================
//  Example 7 — AWS Step Functions: Greeting with QuickFaaS auto-deploy
//
//  Auto-deploys a Java Lambda function via QuickFaaS and creates a Step
//  Function that calls it using the Lambda Function URL (AuthType=AWS_IAM).
//
//  Prerequisites:
//    - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY set in the environment
//    - An IAM role for Lambda execution (LAMBDA_EXEC_ROLE_ARN)
//    - An IAM role for Step Functions execution (STEP_FUNCTIONS_ROLE_ARN)
//    - An S3 bucket for the Lambda deployment package
//    - The QuickFaaS deployment JAR (QUICKFAAS_JAR_PATH)
// ===========================================================================

data class AwsAutoDeployConfig(
    val quickFaasJarPath: String,
    val region: String,
    val stepFunctionsRoleArn: String,
    val stateMachineName: String,
    val s3Bucket: String = "",
    val accountId: String = "",
)

private fun collectAwsAutoDeployConfig(
    scan: Scanner,
    stageTotal: Int,
    defaultStateMachineName: String = "AwsLambdaAutoDeployGreeting",
): AwsAutoDeployConfig? {
    C.stage(1, stageTotal, "Configuration")

    val awsAccessKey = System.getenv("AWS_ACCESS_KEY_ID")
    val awsSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
    if (awsAccessKey.isNullOrBlank() || awsSecretKey.isNullOrBlank()) {
        C.warn("AWS_ACCESS_KEY_ID and/or AWS_SECRET_ACCESS_KEY not set.")
        C.detail("AWS SDK requires these env vars BEFORE starting the JVM.")
        C.detail("  Linux/Mac: export AWS_ACCESS_KEY_ID=AKIA...")
        C.detail("             export AWS_SECRET_ACCESS_KEY=wJalr...")
        println()
        print("  Continue anyway? (y/N): ")
        if (!scan.next().trim().equals("y", ignoreCase = true)) return null
    } else {
        C.ok("AWS credentials set")
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

    val region = System.getenv("AWS_REGION")
        ?: run {
            print("  Enter AWS region [eu-west-1]: ")
            scan.next().trim().ifEmpty { "eu-west-1" }
        }

    val stepFunctionsRoleArn = System.getenv("STEP_FUNCTIONS_ROLE_ARN")
        ?: run {
            print("  Enter Step Functions IAM Role ARN: ")
            scan.next().trim()
        }

    val s3Bucket = System.getenv("AWS_S3_BUCKET")
        ?: run {
            print("  Enter S3 bucket name (or '-' to use value from func-deployment.json): ")
            scan.next().trim().let { if (it == "-") "" else it }
        }

    val accountId = System.getenv("AWS_ACCOUNT_ID")
        ?: run {
            print("  Enter AWS account ID (or '-' to use value from func-deployment.json): ")
            scan.next().trim().let { if (it == "-") "" else it }
        }

    val stateMachineName = System.getenv("STATE_MACHINE_NAME")
        ?: run {
            print("  Enter State Machine name [$defaultStateMachineName]: ")
            scan.next().trim().ifEmpty { defaultStateMachineName }
        }

    C.ok("QuickFaaS JAR: ${C.BOLD}$quickFaasJarPath${C.RESET}")
    C.ok("Region:        ${C.BOLD}$region${C.RESET}")
    C.ok("Role:          ${C.BOLD}$stepFunctionsRoleArn${C.RESET}")
    C.ok("S3 Bucket:     ${C.BOLD}${s3Bucket.ifEmpty { "(from descriptor)" }}${C.RESET}")
    C.ok("Account ID:    ${C.BOLD}${accountId.ifEmpty { "(from descriptor)" }}${C.RESET}")
    C.ok("State Machine: ${C.BOLD}$stateMachineName${C.RESET}")
    println()

    return AwsAutoDeployConfig(quickFaasJarPath, region, stepFunctionsRoleArn, stateMachineName, s3Bucket, accountId)
}

private val Example7Workflow = workflow {
    name("AwsLambdaAutoDeployGreeting")
    description("Auto-deploys a greeting Lambda via QuickFaaS and calls it from Step Functions")
    steps(
        step {
            name("call-hello-lambda")
            description("Call the auto-deployed greeting Lambda with lang=pt")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "hello-lambda-fn",
                        deploymentDescriptorPath = "./functions/hello-lambda-fn/func-deployment.json"
                    )
                    query("lang" to value("pt"))
                    authentication(authentication { type("IAM_ROLE") })
                    result("greetingResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("greetingResult")
}

fun deployAwsLambdaAutoDeployExample(scan: Scanner) {
    val stages = 3
    C.banner("Example 7 — AWS QuickFaaS Auto-Deploy (Lambda Function URL)")
    C.info("Auto-deploys a greeting Lambda via QuickFaaS, then creates a Step Function")
    C.info("that calls it using a Lambda Function URL with IAM authentication.")
    println()
    C.separator()
    println("  ${C.YELLOW}Flow:${C.RESET}  QuickFaaS deploy → Lambda Function URL → Step Functions state machine")
    C.separator()
    println()

    val config = collectAwsAutoDeployConfig(scan, stages) ?: return

    C.stage(2, stages, "Lambda deployment + Workflow resolution")
    C.info("Deploying ${C.BOLD}hello-lambda-fn${C.RESET} via QuickFaaS...")
    C.info("Resolving internal functions and rendering state machine JSON...")

    val lambdaDeployer = AwsLambdaDeployer(
        quickFaasJarPath = Path.of(config.quickFaasJarPath),
        region = config.region,
        roleArn = config.stepFunctionsRoleArn,
        s3Bucket = config.s3Bucket,
        accountId = config.accountId,
    )
    val deployer = AmazonCloudDeployer.Builder()
        .internalFunctionDeployer(lambdaDeployer)
        .build()
    val context = AmazonDeployContext(
        roleArn = config.stepFunctionsRoleArn,
        region = config.region,
        tags = mapOf("environment" to "testing", "app" to "omni-flow"),
        stateMachineName = config.stateMachineName,
    )
    deployer.deploy(Example7Workflow, context)

    C.ok("Lambda deployed and state machine created")
    println()

    C.stage(3, stages, "Complete")
    C.success("Example 7 deployed successfully!")
    C.separator()
    C.detail("State Machine: ${C.BOLD}${config.stateMachineName}${C.RESET}")
    C.detail("Region:        ${C.BOLD}${config.region}${C.RESET}")
    println()
    C.detail("${C.GREEN}Try:${C.RESET} Start execution with input  {}")
    C.detail("${C.GREEN}Expected:${C.RESET} {\"greeting\": \"Ola, Mundo!\", \"language\": \"pt\"}")
    C.separator()
}

// ===========================================================================
//  Example 8 — AWS Step Functions: Text Analysis Pipeline (3 Lambdas)
//
//  Auto-deploys 3 Java Lambda functions via QuickFaaS and creates a Step
//  Function that calls them sequentially on the same input text:
//
//    1. aws-preprocess-fn  — normalizes text (lowercase) and counts words
//    2. aws-char-stats-fn  — computes character and word-length statistics
//    3. aws-summary-fn     — generates a multilingual summary
//
//  All 3 functions receive the input text via query parameters.
//  Input: {"text": "Hello World OmniFlow"}
// ===========================================================================

private val Example8Workflow = workflow {
    name("AwsTextAnalysisPipeline")
    description("Sequential text analysis pipeline with 3 Lambda functions auto-deployed via QuickFaaS")
    steps(
        step {
            name("call-preprocess")
            description("Normalize and preprocess the input text")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "aws-preprocess-fn",
                        deploymentDescriptorPath = "./functions/aws-preprocess-fn/func-deployment.json"
                    )
                    query("text" to variable("text"))
                    result("preprocessResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("call-char-stats")
            description("Compute character and word-length statistics")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "aws-char-stats-fn",
                        deploymentDescriptorPath = "./functions/aws-char-stats-fn/func-deployment.json"
                    )
                    query("text" to variable("text"))
                    result("charStatsResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("call-summary")
            description("Generate a multilingual text summary")
            context(
                call {
                    method(GET)
                    internalFunction(
                        "aws-summary-fn",
                        deploymentDescriptorPath = "./functions/aws-summary-fn/func-deployment.json"
                    )
                    query("text" to variable("text"), "lang" to value("pt"))
                    result("summaryResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("summaryResult")
}

fun deployAwsTextAnalysisExample(scan: Scanner) {
    val stages = 3
    C.banner("Example 8 — AWS Step Functions: Text Analysis Pipeline (3 Lambdas)")
    C.info("Auto-deploys 3 Lambda functions via QuickFaaS:")
    C.detail("${C.BOLD}aws-preprocess-fn${C.RESET}  — normalizes text and counts words")
    C.detail("${C.BOLD}aws-char-stats-fn${C.RESET}  — computes character and word statistics")
    C.detail("${C.BOLD}aws-summary-fn${C.RESET}     — generates a multilingual text summary")
    println()
    C.separator()
    println("  ${C.YELLOW}Flow:${C.RESET}  preprocess → char stats → summary (sequential, same input)")
    C.separator()
    println()

    val config = collectAwsAutoDeployConfig(
        scan,
        stages,
        defaultStateMachineName = "AwsTextAnalysisPipeline"
    ) ?: return

    C.stage(2, stages, "Lambda deployment + Workflow resolution")
    C.info("Deploying ${C.BOLD}aws-preprocess-fn${C.RESET}, ${C.BOLD}aws-char-stats-fn${C.RESET}, ${C.BOLD}aws-summary-fn${C.RESET} via QuickFaaS...")
    C.info("Resolving internal functions and rendering state machine JSON...")

    val lambdaDeployer = AwsLambdaDeployer(
        quickFaasJarPath = Path.of(config.quickFaasJarPath),
        region = config.region,
        roleArn = config.stepFunctionsRoleArn,
        s3Bucket = config.s3Bucket,
        accountId = config.accountId,
    )
    val deployer = AmazonCloudDeployer.Builder()
        .internalFunctionDeployer(lambdaDeployer)
        .build()
    val context = AmazonDeployContext(
        roleArn = config.stepFunctionsRoleArn,
        region = config.region,
        tags = mapOf("environment" to "testing", "app" to "omni-flow"),
        stateMachineName = config.stateMachineName,
    )
    deployer.deploy(Example8Workflow, context)

    C.ok("All 3 Lambdas deployed and state machine created")
    println()

    C.stage(3, stages, "Complete")
    C.success("AWS Text Analysis Pipeline deployed successfully!")
    C.separator()
    C.detail("State Machine: ${C.BOLD}${config.stateMachineName}${C.RESET}")
    C.detail("Region:        ${C.BOLD}${config.region}${C.RESET}")
    println()
    C.detail("${C.GREEN}Try:${C.RESET}     Start execution with input  {\"text\": \"Hello World OmniFlow\"}")
    C.detail("${C.GREEN}Expected:${C.RESET} {\"summary\": \"O texto tem 3 palavras e 5.7 caracteres em média.\", ...}")
    C.separator()
}
