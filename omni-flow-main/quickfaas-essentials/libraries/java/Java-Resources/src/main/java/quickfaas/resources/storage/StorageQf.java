/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources.storage;

import static quickfaas.resources.storage.StorageManagerQf.getStorage;
import static quickfaas.resources.storage.StorageManagerQf.getStorageProperties;

public interface StorageQf {

    static BucketQf newBucket(String bucketName) {
        return getStorage().newBucketImpl(bucketName, getStorageProperties(bucketName));
    }

    BucketQf newBucketImpl(String bucketName, StoragePropertiesQf properties);

}
