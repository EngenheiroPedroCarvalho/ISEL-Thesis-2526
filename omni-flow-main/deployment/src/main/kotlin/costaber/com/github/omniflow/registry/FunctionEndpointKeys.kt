package costaber.com.github.omniflow.registry

object FunctionEndpointKeys {

    fun hostKey(functionRef: String): String = "${functionRef}.host"
    fun pathKey(functionRef: String): String = "${functionRef}.path"

    fun keys(functionRef: String): Pair<String, String> = hostKey(functionRef) to pathKey(functionRef)
}