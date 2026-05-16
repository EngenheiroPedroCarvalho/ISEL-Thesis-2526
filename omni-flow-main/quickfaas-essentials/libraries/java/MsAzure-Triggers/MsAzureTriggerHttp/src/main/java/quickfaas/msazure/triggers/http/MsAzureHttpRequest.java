/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.msazure.triggers.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.HttpRequestMessage;
import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.exceptions.NoSuchHeaderException;
import quickfaas.triggers.http.exceptions.NoSuchPropertyException;
import quickfaas.triggers.http.exceptions.UnexpectedContentTypeException;

public class MsAzureHttpRequest implements HttpRequestQf {

    private static Gson gson;
    private final HttpRequestMessage request;
    private String body = "";

    public MsAzureHttpRequest(HttpRequestMessage request) {
        this.request = request;
    }

    @Override
    public String getBody() {
        if (body.isEmpty()) body = request.getBody().toString();
        return body;
    }

    @Override
    public String getContentType() {
        String contentType = request.getHeaders().getOrDefault("content-type", "").toString();
        if (contentType.isEmpty()) {
            throw new NoSuchHeaderException("Content-Type");
        }
        return contentType;
    }

    @Override
    public JsonElement getFromJsonBody(String property) {
        if (gson == null) gson = new Gson();
        String contentType = getContentType();
        if (!contentType.equals("application/json")) {
            throw new UnexpectedContentTypeException("application/json", contentType);
        }
        JsonObject jsonObj = gson.fromJson(getBody(), JsonObject.class);
        if (!jsonObj.has(property)) throw new NoSuchPropertyException(property);
        return jsonObj.get(property);
    }

    @Override
    public String getQueryParameter(String parameter) {
        Object obj = request.getQueryParameters().get(parameter);
        if (obj == null) {
            throw new NoSuchPropertyException(parameter);
        }
        return obj.toString();
    }

    @Override
    public String getHttpMethod() {
        return request.getHttpMethod().name();
    }
}
