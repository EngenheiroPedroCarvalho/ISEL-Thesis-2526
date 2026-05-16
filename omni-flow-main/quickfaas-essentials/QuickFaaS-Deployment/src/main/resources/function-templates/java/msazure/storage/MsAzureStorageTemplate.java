/*
 * Copyright (c) 4/30/2022, Pexers (https://github.com/Pexers)
 */

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import quickfaas.configurations.ConfigurationsManagerQf;
import quickfaas.msazure.resources.MsAzureResources;
import quickfaas.msazure.triggers.storage.MsAzureBlob;
import quickfaas.msazure.triggers.storage.MsAzureBucketEvent;
import quickfaas.resources.CloudResourcesManagerQf;
import quickfaas.triggers.storage.BlobQf;
import quickfaas.triggers.storage.BucketEventQf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class MsAzureStorageTemplate {

    private static final String functionName = "<name>";
    private static final String storageAccount = "<storage_account>";
    private static final String container = "<container>";
    private static final String configsFile = "<configs_file>";
    private static String configsJson;

    // Function app setting: AzureWebJobsStorageTrigger-{storage_account}
    @StorageAccount("StorageTrigger-" + storageAccount)
    @FunctionName(functionName)
    public void run(
            @BlobTrigger(name = "file",
                    dataType = "binary",
                    path = container + "/{fileName}") byte[] content,
            @BindingName("fileName") String filename,
            final ExecutionContext context
    ) throws Exception {
        if (configsJson == null) {
            InputStream inStream = MsAzureStorageTemplate.class.getClassLoader().getResourceAsStream(configsFile);
            if (inStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                configsJson = reader.lines().collect(Collectors.joining());
                ConfigurationsManagerQf.setConfigurations(configsJson);
            }
            CloudResourcesManagerQf.setCloudResources(new MsAzureResources());
        }
        BucketEventQf bucketEventQf = new MsAzureBucketEvent(storageAccount, container);
        BlobQf bucketObjectQf = new MsAzureBlob(filename);

        new MyFunctionClass().myFunction(bucketEventQf, bucketObjectQf);
    }
}
