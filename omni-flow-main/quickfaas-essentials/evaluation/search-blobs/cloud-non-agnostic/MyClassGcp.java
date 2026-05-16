/*
 * Copyright © 5/29/2022, Pexers (https://github.com/Pexers)
 */

import com.google.api.gax.paging.Page;
import com.google.cloud.ServiceOptions;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MyClassGcp implements HttpFunction {

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String projectId = ServiceOptions.getDefaultProjectId();
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        String body = getBody(request);  // Can only be read once
        String bucketName = getFromJsonBody("bucketName", request, body).getAsString();
        String search = getFromJsonBody("blobSearch", request, body).getAsString();
        List<String> blobs = listBlobs(bucketName, storage);
        List<String> results = new LinkedList<>();
        for (String blobName : blobs) if (blobName.contains(search)) results.add(blobName);
        response.setContentType("application/json");
        send(200, "{\"results\":\"" + results + "\"}", response);
    }

    public JsonElement getFromJsonBody(String property, HttpRequest request, String body) {
        Gson gson = new Gson();
        String contentType = getContentType(request);
        if (!contentType.equals("application/json")) {
            throw new RuntimeException("Content type it's not application/json");
        }
        JsonObject jsonObj = gson.fromJson(body, JsonObject.class);
        if (!jsonObj.has(property)) throw new RuntimeException(property);
        return jsonObj.get(property);
    }

    public List<String> listBlobs(String bucketName, Storage storage) {
        List<String> list = new LinkedList<>();
        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            list.add(blob.getName());
        }
        return list;
    }

    public String getBody(HttpRequest request) {  // Can only be read once
        String body = "";
        if (body.isEmpty()) {
            try {
                body = request.getReader().lines().collect(Collectors.joining());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return body;
    }

    public String getContentType(HttpRequest request) {
        Optional<String> optional = request.getContentType();
        if (optional.isEmpty()) {
            throw new RuntimeException("No Content-Type header");
        }
        return optional.get();
    }

    public void send(int status, String body, HttpResponse response) {
        response.setStatusCode(status);
        try {
            BufferedWriter writer = response.getWriter();
            writer.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
