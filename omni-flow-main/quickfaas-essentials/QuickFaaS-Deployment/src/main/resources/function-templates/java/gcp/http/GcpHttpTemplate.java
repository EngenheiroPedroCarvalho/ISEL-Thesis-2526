/*
 * Copyright (c) 4/30/2022, Pexers (https://github.com/Pexers)
 */

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import quickfaas.configurations.ConfigurationsManagerQf;
import quickfaas.gcp.resources.GcpResources;
import quickfaas.gcp.triggers.http.GcpHttpRequest;
import quickfaas.gcp.triggers.http.GcpHttpResponse;
import quickfaas.resources.CloudResourcesManagerQf;
import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class GcpHttpTemplate implements HttpFunction {

    private static final String configsFile = "<configs_file>";
    private static String configsJson;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        if (configsJson == null) {
            InputStream inStream = GcpHttpTemplate.class.getClassLoader().getResourceAsStream(configsFile);
            if (inStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                configsJson = reader.lines().collect(Collectors.joining());
                ConfigurationsManagerQf.setConfigurations(configsJson);
            }
            CloudResourcesManagerQf.setCloudResources(new GcpResources());
        }
        HttpRequestQf reqQf = new GcpHttpRequest(request);
        HttpResponseQf resQf = new GcpHttpResponse(response);

        new MyFunctionClass().myFunction(reqQf, resQf);
    }

}
