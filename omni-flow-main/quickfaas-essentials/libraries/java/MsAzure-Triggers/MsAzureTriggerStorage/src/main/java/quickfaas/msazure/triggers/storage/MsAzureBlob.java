/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.msazure.triggers.storage;

import quickfaas.triggers.storage.BlobQf;

public class MsAzureBlob implements BlobQf {

    private final String blobName;

    public MsAzureBlob(String blobName) {
        this.blobName = blobName;
    }

    @Override
    public String getName() {
        return blobName;
    }
}
