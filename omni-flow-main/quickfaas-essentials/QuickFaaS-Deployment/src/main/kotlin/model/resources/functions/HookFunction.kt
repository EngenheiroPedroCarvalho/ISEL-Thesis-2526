/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions

class HookFunction {

    var definition = ""
    var dependencies = ""
    val defaultConfigs =
        "{\n  \"resources\": [\n    {\n      \"id\": \"\",\n      \"type\": \"\",\n      \"properties\": {}\n    }\n  ]\n}"
    var configurations = defaultConfigs
    lateinit var templateFile: String

}
