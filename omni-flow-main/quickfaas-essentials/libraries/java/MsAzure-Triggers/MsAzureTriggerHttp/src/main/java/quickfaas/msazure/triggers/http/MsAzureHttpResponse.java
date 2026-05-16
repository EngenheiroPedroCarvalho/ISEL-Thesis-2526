/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.msazure.triggers.http;

import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.HttpStatus;
import quickfaas.triggers.http.HttpResponseQf;

public class MsAzureHttpResponse implements HttpResponseQf {

    private final HttpRequestMessage<String> request;
    public HttpResponseMessage response;  // Accessed in template function
    private Builder builder;
    public MsAzureHttpResponse(HttpRequestMessage<String> request) {
        this.request = request;
    }

    @Override
    public void send(int status, String body) {
        createBuilderIfNeeded();
        response = builder.status(HttpStatus.valueOf(status)).body(body).build();
    }

    @Override
    public void setContentType(String contentType) {
        appendHeader("content-type", contentType);
    }

    @Override
    public void appendHeader(String header, String value) {
        createBuilderIfNeeded();
        builder.header(header, value);
    }

    private void createBuilderIfNeeded() {
        if (builder == null) {
            builder = request.createResponseBuilder(HttpStatus.OK);
        }
    }
}
