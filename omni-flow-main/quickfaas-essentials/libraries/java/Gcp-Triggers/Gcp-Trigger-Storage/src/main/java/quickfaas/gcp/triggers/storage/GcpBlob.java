/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.triggers.storage;

import com.google.cloud.functions.Context;
import quickfaas.triggers.storage.BlobQf;

public class GcpBlob implements BlobQf {

    private final String blobName;
    private final Context context;
    public GcpBlob(String blobName, Context context) {
        this.blobName = blobName;
        this.context = context;
    }

    @Override
    public String getName() {
        return blobName;
    }
}
