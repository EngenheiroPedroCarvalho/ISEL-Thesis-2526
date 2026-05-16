/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources.storage;

import com.google.gson.Gson;
import quickfaas.resources.CloudResourcesManagerQf;

final class StorageManagerQf extends CloudResourcesManagerQf {

    private static final String type = "storage";
    private static StorageQf storage;

    static StorageQf getStorage() {
        if (storage == null) storage = getCloudResources().storage();
        return storage;
    }

    static StoragePropertiesQf getStorageProperties(String bucketName) {
        return new Gson().fromJson(getResourceProperties(bucketName, type), StoragePropertiesQf.class);
    }

}
