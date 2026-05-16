/*
 * Copyright (c) 4/30/2022, Pexers (https://github.com/Pexers)
 */

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import quickfaas.configurations.ConfigurationsManagerQf;
import quickfaas.gcp.resources.GcpResources;
import quickfaas.gcp.triggers.storage.EventData;
import quickfaas.gcp.triggers.storage.GcpBlob;
import quickfaas.gcp.triggers.storage.GcpBucketEvent;
import quickfaas.resources.CloudResourcesManagerQf;
import quickfaas.triggers.storage.BlobQf;
import quickfaas.triggers.storage.BucketEventQf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class GcpStorageTemplate implements BackgroundFunction<EventData> {

    private static final String configsFile = "<configs_file>";
    private static String configsJson;

    @Override
    public void accept(EventData event, Context context) throws Exception {
        if (configsJson == null) {
            InputStream inStream = GcpStorageTemplate.class.getClassLoader().getResourceAsStream(configsFile);
            if (inStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                configsJson = reader.lines().collect(Collectors.joining());
                ConfigurationsManagerQf.setConfigurations(configsJson);
            }
            CloudResourcesManagerQf.setCloudResources(new GcpResources());
        }
        BucketEventQf bucketEventQf = new GcpBucketEvent(context.eventType(), event);
        BlobQf blobQf = new GcpBlob(event.name, context);

        new MyFunctionClass().myFunction(bucketEventQf, blobQf);
    }

}
