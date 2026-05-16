/*
 * Copyright (c) 4/30/2022, Pexers (https://github.com/Pexers)
 */

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import quickfaas.configurations.ConfigurationsManagerQf;
import quickfaas.msazure.resources.MsAzureResources;
import quickfaas.msazure.triggers.http.MsAzureHttpRequest;
import quickfaas.msazure.triggers.http.MsAzureHttpResponse;
import quickfaas.resources.CloudResourcesManagerQf;
import quickfaas.triggers.http.HttpRequestQf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class MsAzureHttpTemplate {

    private static final String functionName = "<name>";
    private static final String configsFile = "<configs_file>";
    private static String configsJson;

    @FunctionName(functionName)
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<String> request, final ExecutionContext context) throws Exception {
        if (configsJson == null) {
            InputStream inStream = MsAzureHttpTemplate.class.getClassLoader().getResourceAsStream(configsFile);
            if (inStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                configsJson = reader.lines().collect(Collectors.joining());
                ConfigurationsManagerQf.setConfigurations(configsJson);
            }
            CloudResourcesManagerQf.setCloudResources(new MsAzureResources());
        }
        HttpRequestQf reqQf = new MsAzureHttpRequest(request);
        MsAzureHttpResponse msAzureHttpRes = new MsAzureHttpResponse(request);

        new MyFunctionClass().myFunction(reqQf, msAzureHttpRes);

        return msAzureHttpRes.response;
    }
}
