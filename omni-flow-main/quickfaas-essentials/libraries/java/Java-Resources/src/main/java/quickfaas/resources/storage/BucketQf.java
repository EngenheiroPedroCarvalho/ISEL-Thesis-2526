/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources.storage;

import java.util.List;

public interface BucketQf {

    // To create a directory for the blob use <directory>/<blobName>
    void createBlob(String blobName, byte[] content, String contentType);

    byte[] readBlob(String blobName);

    List<String> listBlobs();

}
