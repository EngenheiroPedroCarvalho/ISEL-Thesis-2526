/*
 * Copyright © 5/29/2022, Pexers (https://github.com/Pexers)
 */

import quickfaas.resources.storage.BucketQf;
import quickfaas.resources.storage.StorageQf;
import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

import java.util.LinkedList;
import java.util.List;

public class MyFunctionClass {  // search-blobs

    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        BucketQf bucket = StorageQf.newBucket(req.getFromJsonBody("bucketName").getAsString());
        String search = req.getFromJsonBody("blobSearch").getAsString();
        List<String> blobs = bucket.listBlobs();
        List<String> results = new LinkedList<>();
        for (String blobName : blobs) if (blobName.contains(search)) results.add(blobName);
        res.setContentType("application/json");
        res.send(200, "{\"results\":\"" + results + "\"}");
    }

}
