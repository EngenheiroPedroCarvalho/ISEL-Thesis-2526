package costaber.com.github.omniflow

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleCloudDeployer
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleDeployContext
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.internalfunction.quickfaas.QuickFaasDeployer
import costaber.com.github.omniflow.model.HttpMethod
import costaber.com.github.omniflow.model.HttpMethod.*
import java.util.Scanner
import java.nio.file.Path


fun main() {
    val scan = Scanner(System.`in`)
    val option = getSelectedProvider(scan)
    if (option == 0) {
        deployAmazon()
        scan.next()
    }
    if (option == 1){
        deployGoogle()
    }
    if (option == 2){
        deployGoogleWithQuickFaaS()
    }
    if (option == 3){
        deployGoogleGreetingWithQuickFaaS()
    }
}

fun getSelectedProvider(scan: Scanner): Int {
    println("In which cloud provider would you like to deploy?")
    var option: Int
    do {
        println(" 0: Amazon Web Service")
        println(" 1: Google Cloud Platform")
        println(" 2: Google Cloud Platform (with QuickFaaS auto-deploy)")
        println(" 3: Google Cloud Platform (Greeting workflow with QuickFaaS)")
        println("99: Exit")
        print("Choose an option:")
        option = scan.nextInt()
    } while (!((option in 0..3) || option == 99));
    return option
}


/**
 * Deploy a Google Cloud Workflow with QuickFaaS auto-deployment for internal functions.
 *
 * Environment variables (all optional — prompts interactively if missing):
 *   GOOGLE_APPLICATION_CREDENTIALS - Path to GCP service account JSON key (MUST be set before JVM start)
 *   QUICKFAAS_JAR_PATH             - Path to QuickFaaS-Deployment-1.0-fat.jar
 *   GOOGLE_PROJECT_ID              - GCP project ID
 *   GOOGLE_ZONE                    - GCP region (e.g. europe-west1)
 *   GOOGLE_SERVICE_ACCOUNT         - Service account email
 */
fun deployGoogleWithQuickFaaS() {
    val scan = Scanner(System.`in`)

    val googleCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (googleCredentials.isNullOrBlank()) {
        println("WARNING: GOOGLE_APPLICATION_CREDENTIALS is not set.")
        println("Google Cloud SDK requires this env var to be set BEFORE starting the JVM.")
        println("Set it and restart:")
        println("  Linux/Mac: export GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa-key.json")
        println("  Windows:   set GOOGLE_APPLICATION_CREDENTIALS=C:\\path\\to\\sa-key.json")
        println()
        print("Continue anyway? (y/N): ")
        val answer = scan.nextLine().trim()
        if (!answer.equals("y", ignoreCase = true)) return
    } else {
        println("GOOGLE_APPLICATION_CREDENTIALS: $googleCredentials")
    }

    val quickFaasJarPath = System.getenv("QUICKFAAS_JAR_PATH")
        ?: run {
            print("Enter the path to QuickFaaS-Deployment JAR (QUICKFAAS_JAR_PATH): ")
            scan.nextLine().trim()
        }

    val jarFile = Path.of(quickFaasJarPath).toFile()
    require(jarFile.exists()) {
        "QuickFaaS JAR not found at '$quickFaasJarPath'"
    }

    val projectId = System.getenv("GOOGLE_PROJECT_ID")
        ?: run {
            print("Enter GCP Project ID (GOOGLE_PROJECT_ID) [workflow-test-omniflow]: ")
            scan.nextLine().trim().ifEmpty { "workflow-test-omniflow" }
        }

    val region = System.getenv("GOOGLE_ZONE")
        ?: run {
            print("Enter GCP region (GOOGLE_ZONE) [europe-west1]: ")
            scan.nextLine().trim().ifEmpty { "europe-west1" }
        }

    val serviceAccountEmail = System.getenv("GOOGLE_SERVICE_ACCOUNT")
        ?: run {
            print("Enter service account email (GOOGLE_SERVICE_ACCOUNT) [workflow-test@$projectId.iam.gserviceaccount.com]: ")
            scan.nextLine().trim().ifEmpty { "workflow-test@$projectId.iam.gserviceaccount.com" }
        }

    val deployer = GoogleCloudDeployer.Builder()
        .internalFunctionDeployer(
            QuickFaasDeployer(
                quickFaasJarPath = Path.of(quickFaasJarPath),
                projectId = projectId,
                region = region
            )
        )
        .build()

    val googleDeployContext = GoogleDeployContext(
        projectId = projectId,
        zone = region,
        serviceAccount = "projects/$projectId/serviceAccounts/$serviceAccountEmail",
        workflowId = "QuickFaasIntegrationTest",
        workflowDescription = "E2E test: auto-deploy internal function via QuickFaaS",
        workflowLabels = mapOf("environment" to "testing", "app" to "omni-flow", "test" to "quickfaas-integration"),
    )

    println()
    println("Deploying workflow with QuickFaaS auto-deploy enabled...")
    println("  GOOGLE_APPLICATION_CREDENTIALS: ${googleCredentials ?: "(not set)"}")
    println("  QUICKFAAS_JAR_PATH: $quickFaasJarPath")
    println("  GOOGLE_PROJECT_ID: $projectId")
    println("  GOOGLE_ZONE: $region")
    println("  GOOGLE_SERVICE_ACCOUNT: $serviceAccountEmail")
    println()

    deployer.deploy(QuickFaasTestWorkflow, googleDeployContext)

    println("Deployment completed successfully!")
}

fun deployGoogleGreetingWithQuickFaaS() {
    val scan = Scanner(System.`in`)

    val googleCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (googleCredentials.isNullOrBlank()) {
        println("WARNING: GOOGLE_APPLICATION_CREDENTIALS is not set.")
        println("Set it and restart. See option 2 for details.")
        return
    }

    val quickFaasJarPath = System.getenv("QUICKFAAS_JAR_PATH")
        ?: run {
            print("Enter the path to QuickFaaS-Deployment JAR (QUICKFAAS_JAR_PATH): ")
            scan.nextLine().trim()
        }

    val jarFile = Path.of(quickFaasJarPath).toFile()
    require(jarFile.exists()) { "QuickFaaS JAR not found at '$quickFaasJarPath'" }

    val projectId = System.getenv("GOOGLE_PROJECT_ID")
        ?: run {
            print("Enter GCP Project ID [workflow-test-omniflow]: ")
            scan.nextLine().trim().ifEmpty { "workflow-test-omniflow" }
        }

    val region = System.getenv("GOOGLE_ZONE")
        ?: run {
            print("Enter GCP region [europe-west1]: ")
            scan.nextLine().trim().ifEmpty { "europe-west1" }
        }

    val serviceAccountEmail = System.getenv("GOOGLE_SERVICE_ACCOUNT")
        ?: run {
            print("Enter service account email [workflow-test@$projectId.iam.gserviceaccount.com]: ")
            scan.nextLine().trim().ifEmpty { "workflow-test@$projectId.iam.gserviceaccount.com" }
        }

    val deployer = GoogleCloudDeployer.Builder()
        .internalFunctionDeployer(
            QuickFaasDeployer(
                quickFaasJarPath = Path.of(quickFaasJarPath),
                projectId = projectId,
                region = region
            )
        )
        .build()

    val googleDeployContext = GoogleDeployContext(
        projectId = projectId,
        zone = region,
        serviceAccount = "projects/$projectId/serviceAccounts/$serviceAccountEmail",
        workflowId = "GreetingWorkflow",
        workflowDescription = "Multilingual greeting via QuickFaaS auto-deploy",
        workflowLabels = mapOf("environment" to "testing", "app" to "omni-flow", "test" to "greeting-quickfaas"),
    )

    println()
    println("Deploying Greeting workflow with QuickFaaS auto-deploy...")
    println("  Project: $projectId | Region: $region")
    println()

    deployer.deploy(GreetingWorkflow, googleDeployContext)

    println("Greeting workflow deployed successfully!")
}

fun deployAmazon(){

}

fun deployGoogle(){
    val deployer = GoogleCloudDeployer.Builder()
        .build()
    val googleDeployContext = GoogleDeployContext(
        projectId = "workflow-test-omniflow",
        zone = "us-east1",
        serviceAccount = "projects/workflow-test-omniflow/serviceAccounts/" +
                "workflow-test@workflow-test-omniflow.iam.gserviceaccount.com",
        workflowId = "TestWorkflow0",
        workflowDescription = "Chamada de uma função interna",
        workflowLabels = mapOf("environment" to "testing", "app" to "omni-flow"),
    )
    deployer.deploy(TestWorkflow0, googleDeployContext)
}

private val TestWorkflow0 = workflow {
    name("TestWorkflow")
    description("This is a test of the workflow. Call to an internal function")
    steps(
        step {
            name("HTTP call")
            description("Internal Function Test")
            context(
                call {
                    method(GET)
                    internalFunction("svc-upper")
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("result")
}

/**
 * E2E test workflow for QuickFaaS integration.
 *
 * References an internal function with a deploymentDescriptorPath.
 * When the function is not yet deployed, OmniFlow triggers QuickFaaS
 * to deploy it automatically before deploying the workflow.
 *
 * Prerequisites:
 *   - GOOGLE_APPLICATION_CREDENTIALS env var set to a service account JSON key
 *   - QUICKFAAS_JAR_PATH env var set to the QuickFaaS-Deployment-1.0-fat.jar path
 *   - A valid func-deployment.json at the path specified in deploymentDescriptorPath
 *     (relative to the project root, e.g. ./functions/quickfaas-test-fn/func-deployment.json)
 *   - The function source code referenced in the descriptor's functionFile field
 *
 * To run: select option 2 in the CLI menu.
 */
private val QuickFaasTestWorkflow = workflow {
    name("QuickFaasIntegrationTest")
    description("E2E test: workflow that auto-deploys an internal function via QuickFaaS")
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
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("result")
}

/**
 * Greeting workflow: auto-deploys a multilingual greeting function via QuickFaaS,
 * then calls it with ?lang=pt to get a Portuguese greeting.
 */
private val GreetingWorkflow = workflow {
    name("GreetingWorkflow")
    description("Auto-deploys a greeting function via QuickFaaS and calls it")
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
                    result("greetingResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("greetingResult")
}

private val TestWorkflow1 = workflow {
    name("TestWorkflow")
    description("This is a test of the workflow. Call to an internal function that isn't in the registry (Not deployed)")
    steps(
        step {
            name("HTTP call")
            description("Function exists")
            context(
                call {
                    method(GET)
                    host("test.host")
                    path("test.path")
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("result")
}

private val TestWorkflow2 = workflow {
    name("TestWorkflow")
    description("This is a test of the workflow. 2 calls to 2 internal functions")
    steps(
        step {
            name("HTTP call1")
            description("Function exists")
            context(
                call {
                    method(GET)
                    host("fraud-check.host")
                    path("fraud-check.path")
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        },
        step {
            name("HTTP call2")
            description("Function exists")
            context(
                call {
                    method(GET)
                    host("risk-score.host")
                    path("risk-score.path")
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("result")
}

private val TestWorkflow3 = workflow {
    name("TestWorkflow")
    description("This is a test of the workflow. Call to an internal function that isn't in the registry (Not deployed)")
    steps(
        step {
            name("HTTP call")
            description("Function exists")
            context(
                call {
                    method(GET)
                    host("test.host")
                    path("Ola.path")
                    result("result")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("result")
}






//"text": "Hello World, Hello Omniflow"
//TODO Map Reduce
private val MapReduceWorkflow = workflow {
    name("MapReduce")
    description("MapReduce in a text")
    params("input")
    steps(
        step {
            name("init")
            description("Init variables")
            context(
                assign {
                    variables(
                        variable("splitResult") equalTo value(listOf<String>()) //fazer um call step à função Splitt
                    )
                    variables(
                        variable("lengths") equalTo value(mapOf<String,Int>())
                    )
                }
            )
        },

        step {
            name("call_split")
            description("Calls the Split function")
            context(
                call {
                    method(POST)
                    host("https://splitter-592487988123.europe-west1.run.app")
                    path("")
                    header(
                        "Content-Type" to value("application/json"),
                    )
                    body(mapOf("text" to variable("input.text")))
                    result("splitResult")
                    resultType(ResultType.BODY)
                }
            )
        },
        /*
        {
          "words": [
            "Hello2",
            "World,",
            "Omniflow"
          ]
        }
        */

        step {
            name("call_map")
            description("Calls the Map function")
            context(
                parallel {
                    iteration {
                        key("word")
                        forEach(variable("splitResult.body.words"))
                        steps(
                            step {
                                name("callMap")
                                description("Call Map function")
                                context(
                                    call {
                                        method(HttpMethod.POST)
                                        host("https://map-592487988123.europe-west1.run.app")
                                        path("")
                                        header(
                                            "Content-Type" to value("application/json"),
                                        )
                                        body(mapOf("name" to variable("word")))
                                        result("mapResult")
                                        resultType(ResultType.BODY)
                                    }
                                )
                            },
                            step {
                                name("extract_length")
                                description("Extract the length")
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
            name("callReduce")
            description("Call Reduce function")
            context(
                call {
                    method(HttpMethod.POST)
                    host("https://reduce-592487988123.europe-west1.run.app")
                    path("")
                    header(
                        "Content-Type" to value("application/json"),
                    )
                    body(mapOf("lengths" to variable("lengths")))
                    result("sumResult")
                    resultType(ResultType.BODY)
                }
            )
        }
    )
    result("sumResult")
}

        /*step {
            name("firstWord")
            description("Assign first word")
            context(
                iteration {
                    key("word")
                    forEach(variable("text"))
                    steps(
                        step {
                            name("checkIfExists")
                            description("Checks if the word already exists")
                            context(
                                switch {
                                    conditions(
                                        condition {
                                            match(
                                                variable("mappedResults").withKey("word") greaterThanOrEqual value(1)
                                            )
                                            jump("addAnotherValue")
                                        }
                                    )
                                    default("addWord")
                                }
                            )
                        },
                        step {
                            name("addWord")
                            description("Assign word")
                            context(
                                assign {
                                    variables(
                                        variable("mappedResults").withKey("word") equalTo value(1)
                                    )
                                }
                            )
                            next("Continue")
                        },

                        step {
                            name("addAnotherValue")
                            description("Add value")
                            context(
                                assign {
                                    variables(
                                        variable("mappedResults").withKey("word") equalTo value(2)
                                    )
                                }
                            )
                        },

                        step {
                            name("Continue")
                            description("Continue")
                            context(
                                assign {
                                    variables(
                                        variable("ok") equalTo value("ok")
                                    )
                                }
                            )
                        }
                assign {
                    variables(
                        variable("mappedResults").withKey("text[0]") equalTo value(1)
                    )
                    variables(
                        variable("list").withKey("text[0]") equalTo value(vari)
                    )
                }
                    )
                },
            )
        },*/


