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

    // Must match the shade plugin's <finalName> in buildLambdaPom(). Since the POM sets no
    // top-level <build><finalName>, Maven's default jar plugin also produces an unshaded
    // "<artifactId>-<version>.jar" alongside the shaded one; picking the fat jar by exact name
    // (rather than "any non-'original-' jar") avoids depending on filesystem listing order.
    private const val SHADED_JAR_FINAL_NAME = "function"

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
        val jar = File(targetDir, "$SHADED_JAR_FINAL_NAME.jar")
        if (!jar.exists()) {
            throw IllegalStateException(
                "Lambda build failed: expected shaded JAR at '${jar.absolutePath}' was not found. " +
                        "Check that the maven-shade-plugin produced a fat JAR."
            )
        }
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
                            <finalName>$SHADED_JAR_FINAL_NAME</finalName>
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
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.Map;

public class AwsHttpTemplate implements RequestHandler<Map<String, Object>, Object> {

    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        Map<String, String> queryParams = new HashMap<>();
        Object qsp = event.get("queryStringParameters");
        if (qsp instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) qsp).entrySet()) {
                if (entry.getValue() != null) {
                    queryParams.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return new MyFunctionClass().handleRequest(queryParams, context);
    }
}
    """.trimIndent()
}
