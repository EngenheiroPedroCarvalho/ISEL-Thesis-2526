/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.triggers.http;

import com.google.cloud.functions.HttpResponse;
import quickfaas.triggers.http.HttpResponseQf;

import java.io.BufferedWriter;
import java.io.IOException;

public class GcpHttpResponse implements HttpResponseQf {

    private final HttpResponse response;

    public GcpHttpResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public void send(int status, String body) {
        response.setStatusCode(status);
        try {
            BufferedWriter writer = response.getWriter();
            writer.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    @Override
    public void appendHeader(String header, String value) {
        response.appendHeader(header, value);
    }
}
