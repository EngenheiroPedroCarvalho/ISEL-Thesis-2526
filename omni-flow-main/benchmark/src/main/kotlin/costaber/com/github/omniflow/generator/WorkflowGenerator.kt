package costaber.com.github.omniflow.generator

import costaber.com.github.omniflow.generator.StepGenerator.STEP_NAME
import costaber.com.github.omniflow.generator.StepGenerator.addPetToStore
import costaber.com.github.omniflow.generator.StepGenerator.assign
import costaber.com.github.omniflow.generator.StepGenerator.assignTranslation
import costaber.com.github.omniflow.generator.StepGenerator.binaryConditional
import costaber.com.github.omniflow.generator.StepGenerator.checkSuccess
import costaber.com.github.omniflow.generator.StepGenerator.independent
import costaber.com.github.omniflow.generator.StepGenerator.iterationWithForEach
import costaber.com.github.omniflow.generator.StepGenerator.iterationWithRange
import costaber.com.github.omniflow.generator.StepGenerator.multipleDecision
import costaber.com.github.omniflow.generator.StepGenerator.newTranslation
import costaber.com.github.omniflow.generator.StepGenerator.notifyWatchers
import costaber.com.github.omniflow.generator.StepGenerator.parallelIteration
import costaber.com.github.omniflow.generator.StepGenerator.parallelMultipleBranch
import costaber.com.github.omniflow.generator.StepGenerator.parallelOneBranch
import costaber.com.github.omniflow.generator.StepGenerator.petsFromStore
import costaber.com.github.omniflow.generator.StepGenerator.usingVariables
import costaber.com.github.omniflow.model.Range
import costaber.com.github.omniflow.model.Step
import costaber.com.github.omniflow.model.StepType
import costaber.com.github.omniflow.model.Variable
import costaber.com.github.omniflow.model.Workflow

object WorkflowGenerator {
    private const val WORKFLOW_NAME = "testWorkflow"
    private const val WORKFLOW_DESCRIPTION = "Workflow Example"
    private const val WORKFLOW_INPUT = "args"
    private const val WORKFLOW_RESULT = "result"

    /**
     * Generates a workflow with @stepsNumber steps
     * without any relation, independent.
     *
     * @param stepsNumber number of steps to generate
     * @return a workflow with independent steps
     */
    @JvmStatic
    fun withIndependentSteps(stepsNumber: Int): Workflow {
        val steps = (0 until stepsNumber).map { independent(STEP_NAME, it) }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * Generates a workflow with at least @stepsNumber
     * steps, where the maximum steps generated are
     * `stepsNumber` + 1. It will create steps,
     * where half are calls and the other half are assigns.
     * The last step is always a call.
     *
     * @param stepsNumber number of steps to generate
     * @return a workflow with steps that use variables
     */
    @JvmStatic
    fun usingVariables(stepsNumber: Int): Workflow {
        val steps = mutableListOf<Step>()
        var idx = 0
        while (idx < stepsNumber) {
            steps.add(assign(STEP_NAME, idx++))
            steps.add(usingVariables(STEP_NAME, idx++))
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * Generates a workflow with at least @stepsNumber
     * steps, where the maximum steps generated are
     * `stepsNumber` + 2. It will create steps,
     * where 1/3 are binary conditions and the rest
     * 2/3 are calls. The last 2 steps always calls
     *
     * @param stepsNumber number of steps to generate
     * @return a workflow with steps that use binary conditions
     */
    @JvmStatic
    fun withBinaryConditions(stepsNumber: Int): Workflow {
        val steps: MutableList<Step> = mutableListOf()
        val firstStep = independent(STEP_NAME, 0)
        steps.add(firstStep)
        var idx = 1
        while (idx < stepsNumber) {
            steps.add(binaryConditional(STEP_NAME, idx++))
            steps.add(independent(STEP_NAME, idx++))
            steps.add(independent(STEP_NAME, idx++))
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * Generates a workflow with at least @stepsNumber
     * steps, where the maximum steps generated are
     * `stepsNumber` + 4. It will create steps,
     * where 1/5 are switch conditions and the rest
     * 4/5 are calls. The last 2 steps always calls
     *
     * @param stepsNumber number of steps to generate
     * @return a workflow with steps that use binary conditions
     */
    @JvmStatic
    fun withMultipleDecisions(stepsNumber: Int): Workflow {
        val steps: MutableList<Step> = mutableListOf()
        val firstStep = independent(STEP_NAME, 0)
        steps.add(firstStep)
        var idx = 1
        while (idx < stepsNumber) {
            steps.add(multipleDecision(STEP_NAME, idx++))
            steps.add(independent(STEP_NAME, idx++))
            steps.add(independent(STEP_NAME, idx++))
            steps.add(independent(STEP_NAME, idx++))
            steps.add(independent(STEP_NAME, idx++))
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    @JvmStatic
    fun textTranslator(): Workflow {
        val steps: MutableList<Step> = mutableListOf()
        steps.add(newTranslation())
        steps.add(assignTranslation())
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            "translation_result"
        )
    }

    @JvmStatic
    fun saveAndGetPetFromStore(): Workflow {
        val steps: MutableList<Step> = mutableListOf()
        steps.add(addPetToStore())
        steps.add(checkSuccess())
        steps.add(petsFromStore)
        steps.add(notifyWatchers())
        return Workflow(
            WORKFLOW_NAME,
            "Calling APIGW HTTP Endpoint",
            WORKFLOW_INPUT,
            steps,
            "NotificationStatus"
        )
    }

    fun withIterationsRange(stepsNumber: Int): Workflow = Workflow(
        WORKFLOW_NAME,
        WORKFLOW_DESCRIPTION,
        WORKFLOW_INPUT,
        listOf(
            iterationWithRange(
                STEP_NAME, 0, (0 until stepsNumber).map { independent("INNER$STEP_NAME", it) },
                Range(1, 10)
            )
        ),
        WORKFLOW_RESULT
    )

    fun withIterationsForEach(stepsNumber: Int): Workflow = Workflow(
        WORKFLOW_NAME,
        WORKFLOW_DESCRIPTION,
        WORKFLOW_INPUT,
        listOf(
            iterationWithForEach(
                STEP_NAME, 0, (0 until stepsNumber).map { independent("INNER$STEP_NAME", it) },
                Variable("number")
            )
        ),
        WORKFLOW_RESULT
    )

    fun withParallelOneBranch(stepsNumber: Int): Workflow = Workflow(
        WORKFLOW_NAME,
        WORKFLOW_DESCRIPTION,
        WORKFLOW_INPUT,
        listOf(parallelOneBranch((0 until stepsNumber).map { independent("INNER$STEP_NAME", it) })),
        WORKFLOW_RESULT
    )

    fun withParallelMultipleBranches(stepsNumber: Int, bucketSize: Int = 10): Workflow {
        val steps = mutableListOf<Step>()
        0.until(stepsNumber / bucketSize).map {
            steps.add(
                parallelMultipleBranch(
                    (0 until 1).map { independent("INNER$it$STEP_NAME", it) },
                    bucketSize
                )
            )
        }
        if (stepsNumber % bucketSize != 0) {
            steps.add(
                parallelMultipleBranch(
                    (0 until 1).map { independent("INNER$it$STEP_NAME", it) },
                    bucketSize
                )
            )
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    fun withParallelIterationRange(stepNumber: Int): Workflow = Workflow(
        WORKFLOW_NAME,
        WORKFLOW_DESCRIPTION,
        WORKFLOW_INPUT,
        listOf(
            parallelIteration(
                StepContextGenerator.iteration(
                    (0 until stepNumber).map { independent("INNER$it$STEP_NAME", it) },
                    Range(1, 10)
                )
            )
        ),
        WORKFLOW_RESULT
    )

    fun withParallelIterationWithForEach(stepNumber: Int): Workflow = Workflow(
        WORKFLOW_NAME,
        WORKFLOW_DESCRIPTION,
        WORKFLOW_INPUT,
        listOf(
            parallelIteration(
                StepContextGenerator.iteration(
                    (0 until stepNumber).map { independent("INNER$it$STEP_NAME", it) },
                    Variable("number1"),
                )
            )
        ),
        WORKFLOW_RESULT
    )

    // ---------------------------------------------------------------------
    // Additions for the parameter-count (P2), internal-call resolution (P3)
    // and nesting-depth (P5) benchmarks. Pure in-memory model construction;
    // no I/O, no network, no cloud SDK.
    // ---------------------------------------------------------------------

    /**
     * P2 helper. Builds a workflow with a FIXED [stepsNumber] of CALL steps,
     * where every call carries exactly [parameterCount] query, header and
     * body parameters. This isolates the cost of rendering call payloads
     * from the cost of rendering more steps.
     *
     * @param stepsNumber fixed number of call steps
     * @param parameterCount number of query/header/body parameters per call
     */
    @JvmStatic
    fun withParameterizedCalls(stepsNumber: Int, parameterCount: Int): Workflow {
        val steps = (0 until stepsNumber).map { idx ->
            Step(
                STEP_NAME + idx,
                "Parameterized call step example",
                StepType.CALL,
                StepContextGenerator.callWithParameters(parameterCount)
            )
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * P3 helper. Builds a workflow whose calls are ALL internal (each call
     * references the same registry [functionName] via internalFunction()).
     * Resolution of these calls reads from the in-memory registry only.
     */
    @JvmStatic
    fun withInternalCalls(stepsNumber: Int, functionName: String): Workflow {
        val steps = (0 until stepsNumber).map { idx ->
            Step(
                STEP_NAME + idx,
                "Internal call step example",
                StepType.CALL,
                StepContextGenerator.internalCall(functionName)
            )
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * P8/P9 helper. Builds a workflow with [callCount] internal CALL steps whose
     * references are distributed round-robin over [distinctFunctionCount] distinct
     * functions: step idx -> "[baseName]${idx % distinctFunctionCount}". The registry
     * must contain exactly those functions ([baseName]0 .. [baseName]{distinctFunctionCount-1}).
     *
     * Unlike [withInternalCalls] (a single shared name), this lets the number of
     * DISTINCT internal functions vary INDEPENDENTLY of the number of calls, so the
     * two cost axes (N = calls, M = functions in the registry) can be swept separately.
     */
    @JvmStatic
    fun withDistinctInternalCalls(
        callCount: Int,
        distinctFunctionCount: Int,
        baseName: String = "benchFn"
    ): Workflow {
        val steps = (0 until callCount).map { idx ->
            Step(
                STEP_NAME + idx,
                "Distinct internal call step example",
                StepType.CALL,
                StepContextGenerator.internalCall("$baseName${idx % distinctFunctionCount}")
            )
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * P3 helper. Builds a workflow whose calls are ALL external (literal
     * host/path, no internalFunction()). The endpoint resolver leaves these
     * untouched, so this is the baseline (no registry access at all).
     */
    @JvmStatic
    fun withExternalCalls(stepsNumber: Int): Workflow {
        val steps = (0 until stepsNumber).map { idx ->
            Step(
                STEP_NAME + idx,
                "External call step example",
                StepType.CALL,
                StepContextGenerator.externalCall()
            )
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            steps,
            WORKFLOW_RESULT
        )
    }

    /**
     * P5 helper. Builds a workflow with a FIXED [totalSteps] count of leaf
     * CALL steps, wrapped in [depth] levels of nesting. Nesting alternates
     * between an iteration (range) wrapper and a parallel (single-branch)
     * wrapper, reusing the existing iteration/parallel step generators.
     *
     * depth == 0 -> flat list of [totalSteps] independent calls
     * depth >= 1 -> the leaf calls live [depth] containers deep
     *
     * The number of innermost leaf steps is held constant across depths so
     * that the benchmark isolates the effect of nesting from the effect of
     * step count.
     */
    @JvmStatic
    fun withNestedSteps(totalSteps: Int, depth: Int): Workflow {
        val leaves: List<Step> = (0 until totalSteps).map { independent("INNER$STEP_NAME", it) }
        var current: List<Step> = leaves
        for (level in 0 until depth) {
            val wrapped: Step = if (level % 2 == 0) {
                iterationWithRange("NEST$level", level, current, Range(1, 10))
            } else {
                parallelOneBranch(current)
            }
            current = listOf(wrapped)
        }
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            current,
            WORKFLOW_RESULT
        )
    }

    /**
     * P12 helper. Builds a workflow with a SINGLE Choice step carrying exactly [branchCount]
     * conditions, isolating the render cost of a Choice's branch width from step count/nesting.
     */
    @JvmStatic
    fun withChoiceBranches(branchCount: Int): Workflow {
        val step = Step(
            STEP_NAME,
            "Choice step example",
            StepType.CONDITIONAL,
            StepContextGenerator.choiceWithConditions(branchCount)
        )
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            listOf(step),
            WORKFLOW_RESULT
        )
    }

    /**
     * P12 helper. Builds a workflow with a SINGLE Parallel step carrying exactly [branchCount]
     * branches (each holding [leafStepsPerBranch] leaf calls), isolating the render cost of a
     * Parallel's branch width. Unlike [withParallelMultipleBranches] (which chunks a flat step
     * total into several separate Parallel blocks of fixed bucket size), this keeps a single
     * block so branch width is the only varying axis.
     */
    @JvmStatic
    fun withParallelBranchWidth(branchCount: Int, leafStepsPerBranch: Int = 1): Workflow {
        val leaves = (0 until leafStepsPerBranch).map { independent("INNER$STEP_NAME", it) }
        val step = parallelMultipleBranch(leaves, branchCount)
        return Workflow(
            WORKFLOW_NAME,
            WORKFLOW_DESCRIPTION,
            WORKFLOW_INPUT,
            listOf(step),
            WORKFLOW_RESULT
        )
    }
}
