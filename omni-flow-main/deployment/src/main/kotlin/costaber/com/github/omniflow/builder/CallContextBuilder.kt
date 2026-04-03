package costaber.com.github.omniflow.builder

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import costaber.com.github.omniflow.jackson.OmniflowObjectMapper
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.*


class  CallContextBuilder : ContextBuilder {

    private val objectMapper: ObjectMapper
    private val typeFactory: TypeFactory = TypeFactory.defaultInstance()
    private val mapType = typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java)

    constructor(objectMapper: ObjectMapper = OmniflowObjectMapper.default) : super() {
        this.objectMapper = objectMapper
    }

    // mandatory
    private lateinit var method: HttpMethod
    private lateinit var result: String

    //external fields (optional if internalFunction is used)
    private  var host: String? = null
    private  var path: String? = null

    //internal field
    private var internalFunction: InternalFunction? = null

    // optional
    private var resultType: ResultType = ResultType.BODY
    private val header: MutableMap<String, Term<*>> = mutableMapOf()
    private val query: MutableMap<String, Term<*>> = mutableMapOf()
    private val body: MutableMap<String, Any> = mutableMapOf()
    private var bodyRaw: String? = null
    private var authenticationBuilder: AuthenticationBuilder? = null
    private var timeout: Long? = null

    /**
     * External call only.
     */
    fun host(value: String) = apply {
        check(internalFunction == null){
            "Invalid call(): host() cannot be used when internalFunction(...) is set."
        }
        this.host = value
    }
    /**
     * External call only.
     */
    fun path(value: String) = apply {
        check(internalFunction == null){
            "Invalid call(): path() cannot be used when internalFunction(...) is set."
        }
        this.path = value
    }

    /**
     * Internal Call:
     * - host/path must not be set
     * - host/path will be resolved from Function Registry before render/deploy
     */
    fun internalFunction(name: String, deploymentDescriptorPath: String? = null) = apply {
        check(host == null && path == null){
            "Invalid call(): internalFunction(...) cannot be used if host/path were already set."
        }
        this.internalFunction = InternalFunction(name = name, deploymentDescriptorPath = deploymentDescriptorPath)
    }
    /*fun functionRef(value: String) = apply {
        check(host == null  && path == null){
            "Invalid CallContext: functionRef('$value') cannot be used when host/path are already set"
        }
        this.functionRef = value.trim()
    }*/

    fun method(value: HttpMethod) = apply { this.method = value }

    fun header(vararg value: Pair<String, Any>) = apply {
        value.forEach {
            header[it.first] = when (it.second) {
                is Term<*> -> it.second as Term<*>
                else -> Value(it.second)
            }
        }
    }

    fun header(value: Map<String, Any>) = apply {
        value.forEach {
            header[it.key] = when (it.value) {
                is Term<*> -> it.value as Term<*>
                else -> Value(it.value)
            }
        }
    }

    fun query(vararg value: Pair<String, Any>) = apply {
        value.forEach {
            query[it.first] = when (it.second) {
                is Term<*> -> it.second as Term<*>
                else -> Value(it.second)
            }
        }
    }

    fun query(value: Map<String, Any>) = apply {
        value.forEach {
            query[it.key] = when (it.value) {
                is Term<*> -> it.value as Term<*>
                else -> Value(it.value)
            }
        }
    }


    fun body(value: String) = apply { this.bodyRaw = value }
    fun body(vararg value: Pair<String, Any>) = apply {
        value.forEach { body[it.first] = it.second }
    }

    fun body(value: Map<String, Any>) = apply {
        value.forEach { body[it.key] = it.value }
    }

    fun body(any: Any) = apply {
        val maybeJsonString: String? = try {
            objectMapper.writeValueAsString(any)
        } catch (_: JsonProcessingException) {
            null
        }

        val maybeJson: Map<String, Any>? =
            maybeJsonString?.let {
                try {
                    objectMapper.readValue(maybeJsonString, mapType)
                } catch (_: JsonProcessingException) {
                    null
                } catch (_: JsonMappingException) {
                    null
                }
            }

        if (maybeJson != null) {
            body(maybeJson)
        } else {
            bodyRaw = maybeJsonString ?: any.toString()
        }
    }

    fun authentication(value: AuthenticationBuilder) = apply { this.authenticationBuilder = value }

    fun timeout(value: Long) = apply { this.timeout = value }

    fun result(value: String) = apply { this.result = value }

    fun resultType(resultType: ResultType) = apply { this.resultType = resultType }

    override fun stepType() = StepType.CALL

    override fun build(): CallContext {
        //INTERNAL call
        internalFunction?.let { internal ->
            return CallContext(
                host = "",
                path = "",
                method = method,
                header = header,
                query = query,
                body = body,
                bodyRaw = bodyRaw ?: "",
                authentication = authenticationBuilder?.build(),
                timeoutInSeconds = timeout,
                result = result,
                resultType = resultType,
                internalFunction = internal
            )
        }

        //EXTERNAL call
        val finalHost = host ?: throw IllegalStateException(
            "Invalid call(): host() is mandatory for external calls (when internalFunction is not set)"
        )

        val finalPath = path ?: ""

        return CallContext(
            host = finalHost,
            path = finalPath,
            method = method,
            header = header,
            query = query,
            body = body,
            bodyRaw = bodyRaw ?: "",
            authentication = authenticationBuilder?.build(),
            timeoutInSeconds = timeout,
            result = result,
            resultType = resultType,
            internalFunction = null
        )
    }
}