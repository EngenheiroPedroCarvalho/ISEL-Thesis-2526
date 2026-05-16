/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.resources.storage;

import quickfaas.resources.storage.BucketQf;
import quickfaas.resources.storage.StoragePropertiesQf;
import quickfaas.resources.storage.StorageQf;

public class GcpStorage implements StorageQf {
    @Override
    public BucketQf newBucketImpl(String bucketName, StoragePropertiesQf storageProperties) {
        return new GcpBucket(bucketName);  // Storage properties not needed
    }
}
