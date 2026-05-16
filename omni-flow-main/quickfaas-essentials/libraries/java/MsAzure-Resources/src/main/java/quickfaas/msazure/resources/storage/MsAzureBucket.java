/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.msazure.resources.storage;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import quickfaas.resources.exceptions.InvalidAccessKeyException;
import quickfaas.resources.exceptions.ResourceNotFoundException;
import quickfaas.resources.storage.BucketQf;
import quickfaas.resources.storage.StoragePropertiesQf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.LinkedList;
import java.util.List;

public class MsAzureBucket implements BucketQf {

    private final String bucketName;
    private final StoragePropertiesQf storageProperties;
    private CloudBlobContainer containerClient;

    public MsAzureBucket(String bucketName, StoragePropertiesQf storageProperties) {
        this.bucketName = bucketName;
        this.storageProperties = storageProperties;
    }

    @Override
    public void createBlob(String blobName, byte[] content, String contentType) {
        if(containerClient == null) buildContainerClient();
        try {
            CloudBlockBlob blob = containerClient.getBlockBlobReference(blobName);
            blob.getProperties().setContentType(contentType);
            blob.uploadFromByteArray(content, 0, content.length);
        } catch (URISyntaxException | StorageException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public byte[] readBlob(String blobName) {
        if(containerClient == null) buildContainerClient();
        try {
            CloudBlockBlob blob = containerClient.getBlockBlobReference(blobName);
            blob.downloadAttributes();
            byte[] content = new byte[(int) blob.getProperties().getLength()];
            blob.downloadToByteArray(content, 0);
            return content;
        } catch (StorageException | URISyntaxException e) {
            throw new ResourceNotFoundException(blobName);
        }
    }

    @Override
    public List<String> listBlobs() {
        if(containerClient == null) buildContainerClient();
        List<String> list = new LinkedList<>();
        for (ListBlobItem blob : containerClient.listBlobs()) {
            String uriPath = blob.getUri().getPath();
            list.add(uriPath.substring(uriPath.lastIndexOf('/') + 1));
        }
        return list;
    }

    private void buildContainerClient() {
        String[] parts = bucketName.split("/", 2);
        String storageAccountName = parts[0];
        String container = parts[1];
        String accessKey = storageProperties.accessKey;
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse("DefaultEndpointsProtocol=https;AccountName=" + storageAccountName + ";AccountKey=" + accessKey);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            containerClient = blobClient.getContainerReference(container);
        } catch (StorageException | URISyntaxException e) {
            throw new ResourceNotFoundException(bucketName, "Make sure that bucketName follows the format <bucket>/<container>.");
        } catch (InvalidKeyException e) {
            throw new InvalidAccessKeyException();
        }
    }

}
