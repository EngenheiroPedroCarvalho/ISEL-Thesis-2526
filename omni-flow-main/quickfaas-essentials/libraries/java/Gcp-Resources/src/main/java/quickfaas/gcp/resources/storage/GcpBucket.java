/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.resources.storage;

import com.google.api.gax.paging.Page;
import com.google.cloud.ServiceOptions;
import com.google.cloud.storage.*;
import quickfaas.resources.storage.BucketQf;

import java.util.LinkedList;
import java.util.List;

public class GcpBucket implements BucketQf {

    private final String bucketName;
    private final String projectId = ServiceOptions.getDefaultProjectId();
    private final Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

    public GcpBucket(String bucketName) {this.bucketName = bucketName;}

    @Override
    public void createBlob(String blobName, byte[] bytes, String contentType) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        storage.create(blobInfo, bytes);
    }

    @Override
    public byte[] readBlob(String blobName) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        return storage.readAllBytes(blobId);
    }

    @Override
    public List<String> listBlobs() {
        List<String> list = new LinkedList<>();
        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            list.add(blob.getName());
        }
        return list;
    }

}
