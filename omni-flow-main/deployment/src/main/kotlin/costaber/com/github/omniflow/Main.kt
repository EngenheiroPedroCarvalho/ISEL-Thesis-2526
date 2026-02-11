package costaber.com.github.omniflow

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleCloudDeployer
import costaber.com.github.omniflow.cloud.provider.google.deployer.GoogleDeployContext
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.model.HttpMethod
import costaber.com.github.omniflow.model.HttpMethod.*
import java.util.Scanner


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
}

fun getSelectedProvider(scan: Scanner): Int {
    println("In which cloud provider would you like to deploy?")
    var option: Int
    do {
        println(" 0: Amazon Web Service")
        println(" 1: Google Cloud Platform")
        println("99: Exit")
        print("Choose an option:")
        option = scan.nextInt()
    } while (!((option in 0..1) || option == 99));
    return option
}


fun deployAmazon(){

}

fun deployGoogle(){
    val deployer = GoogleCloudDeployer.Builder().build()
    val googleDeployContext = GoogleDeployContext(
        projectId = "workflow-test-omniflow",
        zone = "us-east1",
        serviceAccount = "projects/workflow-test-omniflow/serviceAccounts/" +
                "workflow-test@workflow-test-omniflow.iam.gserviceaccount.com",
        workflowId = "MapReduceWorkflow",
        workflowDescription = "Faz um MapReduce num texto",
        workflowLabels = mapOf("environment" to "testing", "app" to "omni-flow"),
    )
    deployer.deploy(MapReduceWorkflow, googleDeployContext)
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


