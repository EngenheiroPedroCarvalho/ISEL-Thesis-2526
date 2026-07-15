#!/usr/bin/env bash
# S1 - Lambda bundle/ZIP size: AWS "agnostic" (QuickFaaS-built) vs native baseline.
#
# Mirrors the QuickFaaS "ZIP size (KB)" evaluation, extended to the AWS provider.
# Builds the SAME function two ways and compares the shaded fat-jar size:
#   - agnostic: MyFunctionClass (business logic) + generated AwsHttpTemplate adapter
#               + aws-lambda-java-core, shaded  (exactly what AwsBuildScripts produces)
#   - native:   a single RequestHandler class doing the same work directly, shaded
# The delta is the packaging overhead added by the QuickFaaS AWS layer.
# Purely LOCAL (a Maven build); no cloud, no deploy.
#
# Usage: benchmark/results/measure_bundle_size.sh
set -euo pipefail

MVN="${MVN:-mvn}"
WORK="$(mktemp -d)"
REPO_FUNC="$(cd "$(dirname "$0")/../../functions/hello-lambda-fn" && pwd)"
trap 'rm -rf "$WORK"' EXIT

# ---- shared POM (verbatim from AwsBuildScripts.buildLambdaPom, no user deps) ----
pom() {
  local finalName="$1" handler="$2"
  cat <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>quickfaas</groupId>
    <artifactId>${finalName}</artifactId>
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
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <finalName>${finalName}</finalName>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <filters><filter><artifact>*:*</artifact><excludes>
                                <exclude>module-info.class</exclude>
                                <exclude>META-INF/versions/*/module-info.class</exclude>
                            </excludes></filter></filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
POM
}

# ---------------- AGNOSTIC (QuickFaaS AWS) ----------------
AGN="$WORK/agnostic/src/main/java"
mkdir -p "$AGN"
cp "$REPO_FUNC/MyFunctionClass.java" "$AGN/MyFunctionClass.java"
# AwsHttpTemplate wrapper, verbatim from AwsBuildScripts.LAMBDA_HTTP_WRAPPER
cat > "$AGN/AwsHttpTemplate.java" <<'JAVA'
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
JAVA
pom "function" "AwsHttpTemplate" > "$WORK/agnostic/pom.xml"

# ---------------- NATIVE (hand-written baseline) ----------------
NAT="$WORK/native/src/main/java"
mkdir -p "$NAT"
cat > "$NAT/GreetingLambda.java" <<'JAVA'
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.Map;

// Same greeting logic, written directly as the Lambda entry point (no adapter).
public class GreetingLambda implements RequestHandler<Map<String, Object>, Object> {
    @Override
    public Object handleRequest(Map<String, Object> event, Context context) {
        String lang = "en";
        Object qsp = event.get("queryStringParameters");
        if (qsp instanceof Map) {
            Object l = ((Map<?, ?>) qsp).get("lang");
            if (l != null) lang = String.valueOf(l);
        }
        String greeting;
        switch (lang.toLowerCase()) {
            case "pt": greeting = "Ola, Mundo!"; break;
            case "es": greeting = "Hola, Mundo!"; break;
            case "fr": greeting = "Bonjour, le Monde!"; break;
            case "de": greeting = "Hallo, Welt!"; break;
            default:   greeting = "Hello, World!"; break;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("greeting", greeting);
        result.put("language", lang);
        return result;
    }
}
JAVA
pom "function-native" "GreetingLambda" > "$WORK/native/pom.xml"

# ---------------- build both & measure ----------------
echo "Building agnostic (QuickFaaS AWS) bundle..."
( cd "$WORK/agnostic" && "$MVN" -q -o package 2>/dev/null || "$MVN" -q package ) >/dev/null
echo "Building native baseline bundle..."
( cd "$WORK/native" && "$MVN" -q -o package 2>/dev/null || "$MVN" -q package ) >/dev/null

AGN_JAR="$WORK/agnostic/target/function.jar"
NAT_JAR="$WORK/native/target/function-native.jar"
agn=$(stat -c %s "$AGN_JAR")
nat=$(stat -c %s "$NAT_JAR")
delta=$((agn - nat))

printf '\n%-28s %12s %10s\n' "Bundle" "bytes" "KB"
printf '%-28s %12d %10.1f\n' "AWS agnostic (QuickFaaS)" "$agn" "$(echo "scale=1;$agn/1024"|bc)"
printf '%-28s %12d %10.1f\n' "AWS native (baseline)" "$nat" "$(echo "scale=1;$nat/1024"|bc)"
printf '%-28s %12d %10.2f\n' "delta (agnostic overhead)" "$delta" "$(echo "scale=2;$delta/1024"|bc)"
pct=$(echo "scale=3; 100*$delta/$nat" | bc)
printf 'overhead: %s%% of the native bundle\n' "$pct"
