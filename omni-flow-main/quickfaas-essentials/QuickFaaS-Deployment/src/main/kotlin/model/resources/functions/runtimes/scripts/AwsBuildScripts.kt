/*
 * Copyright © 2024, QuickFaaS
 */

package model.resources.functions.runtimes.scripts

import model.Utils
import model.resources.functions.AwsLambdaFunction
import model.resources.functions.CloudFunction
import model.resources.functions.runtimes.utils.JavaUtils
import java.io.File

object AwsBuildScripts : CloudBuildScripts {

    override fun javaBuildScript(func: CloudFunction, templatesDir: String, tmpDir: String) {
        func as AwsLambdaFunction
        JavaUtils.let {
            val pomContent = buildLambdaPom(func.hookFunction.dependencies)
            it.createPom(pomContent, tmpDir)
            it.createJavaSourceFile("AwsHttpTemplate.java", LAMBDA_HTTP_WRAPPER, tmpDir)
            it.createJavaSourceFile("MyFunctionClass.java", func.hookFunction.definition, tmpDir)
            it.mavenBuild(tmpDir)
            copyFatJarAsZip(func, tmpDir)
        }
    }

    override fun nodeJsBuildScript() = TODO("Not yet implemented")

    private fun copyFatJarAsZip(func: AwsLambdaFunction, tmpDir: String) {
        val runtime = func.runtimeVersion!!.runtime
        val targetDir = File("${runtime.tmpDirsRoot}/$tmpDir/target")
        val jar = targetDir.listFiles { f ->
            f.extension == "jar" && !f.name.startsWith("original-")
        }?.firstOrNull()
            ?: throw IllegalStateException(
                "Lambda build failed: no JAR found in '${targetDir.absolutePath}'. " +
                        "Check that the maven-shade-plugin produced a fat JAR."
            )
        val dest = File("${runtime.tmpDirsRoot}/$tmpDir/${Utils.ZIP_FILE}")
        jar.copyTo(dest, overwrite = true)
    }

    private fun buildLambdaPom(userDependencies: String): String {
        val deps = if (userDependencies.isBlank()) "" else
            userDependencies
                .replace("<dependencies>", "")
                .replace("</dependencies>", "")
                .trimIndent()

        return """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>quickfaas</groupId>
    <artifactId>aws-lambda-faas</artifactId>
    <version>1.0</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.2.3</version>
        </dependency>
        $deps
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.4.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <finalName>function</finalName>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>module-info.class</exclude>
                                        <exclude>META-INF/versions/*/module-info.class</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
        """.trimIndent()
    }

    private val LAMBDA_HTTP_WRAPPER = """
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.*;
import java.util.*;

public class AwsHttpTemplate implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String body = new String(input.readAllBytes(), "UTF-8");
        Map<String, String> params = parseQueryParams(body);
        Object result = new MyFunctionClass().handleRequest(params, context);
        output.write(toJsonBytes(result));
    }

    private Map<String, String> parseQueryParams(String json) {
        Map<String, String> params = new HashMap<>();
        int idx = json.indexOf("\"queryStringParameters\"");
        if (idx < 0) return params;
        int open = json.indexOf('{', idx + 23);
        if (open < 0) return params;
        int close = json.indexOf('}', open);
        if (close < 0) return params;
        String content = json.substring(open + 1, close).trim();
        if (content.isEmpty()) return params;
        for (String pair : content.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2)
                params.put(unquote(kv[0].trim()), unquote(kv[1].trim()));
        }
        return params;
    }

    private String unquote(String s) {
        return (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
            ? s.substring(1, s.length() - 1) : s;
    }

    private byte[] toJsonBytes(Object obj) {
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
                first = false;
            }
            return sb.append("}").toString().getBytes();
        }
        return ("\"" + obj + "\"").getBytes();
    }
}
    """.trimIndent()
}
