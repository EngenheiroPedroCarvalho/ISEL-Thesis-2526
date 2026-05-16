/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.triggers.http;

import com.google.cloud.functions.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.exceptions.NoSuchHeaderException;
import quickfaas.triggers.http.exceptions.NoSuchPropertyException;
import quickfaas.triggers.http.exceptions.UnexpectedContentTypeException;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class GcpHttpRequest implements HttpRequestQf {

    private static Gson gson;
    private final HttpRequest request;
    private String body = "";

    public GcpHttpRequest(HttpRequest request) {
        this.request = request;
    }

    @Override
    public String getBody() {  // Can only be read once, the second reading would bring empty string
        if (body.isEmpty()) {
            try {
                body = request.getReader().lines().collect(Collectors.joining());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    @Override
    public String getContentType() {
        Optional<String> optional = request.getContentType();
        if (optional.isEmpty()) {
            throw new NoSuchHeaderException("Content-Type");
        }
        return optional.get();
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
        Optional<String> optional = request.getFirstQueryParameter(parameter);
        if (optional.isEmpty()) {
            throw new NoSuchPropertyException(parameter);
        }
        return optional.get();
    }

    @Override
    public String getHttpMethod() {
        return request.getMethod();
    }
}
