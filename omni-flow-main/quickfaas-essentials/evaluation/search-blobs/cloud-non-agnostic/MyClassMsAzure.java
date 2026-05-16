/*
 * Copyright © 5/29/2022, Pexers (https://github.com/Pexers)
 */

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.util.LinkedList;
import java.util.List;

public class MyClassMsAzure {

    private static final String accessKey = "bucketAccessKey";

    @FunctionName("search-blobs")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET}) HttpRequestMessage<String> request, final ExecutionContext context) throws Exception {
        String[] parts = getFromJsonBody("bucketName", request).getAsString().split("/", 2);
        String search = getFromJsonBody("blobSearch", request).getAsString();
        String storageAccountName = parts[0];
        String container = parts[1];
        CloudStorageAccount storageAccount = CloudStorageAccount.parse("DefaultEndpointsProtocol=https;AccountName=" + storageAccountName + ";AccountKey=" + accessKey);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer containerClient = blobClient.getContainerReference(container);
        List<String> blobs = listBlobs(containerClient);
        List<String> results = new LinkedList<>();
        for (String blobName : blobs) if (blobName.contains(search)) results.add(blobName);
        return request.createResponseBuilder(HttpStatus.OK).header("content-type", "application/json")
                .body("{\"results\":\"" + results + "\"}").build();
    }

    public JsonElement getFromJsonBody(String property, HttpRequestMessage<String> request) {
        Gson gson = new Gson();
        JsonObject jsonObj = gson.fromJson(getBody(request), JsonObject.class);
        return jsonObj.get(property);
    }

    public List<String> listBlobs(CloudBlobContainer containerClient) {
        List<String> list = new LinkedList<>();
        for (ListBlobItem blob : containerClient.listBlobs()) {
            String uriPath = blob.getUri().getPath();
            list.add(uriPath.substring(uriPath.lastIndexOf('/') + 1));
        }
        return list;
    }

    public String getBody(HttpRequestMessage<String> request) {
        String body = "";
        if (body.isEmpty()) body = request.getBody();
        return body;
    }

}
