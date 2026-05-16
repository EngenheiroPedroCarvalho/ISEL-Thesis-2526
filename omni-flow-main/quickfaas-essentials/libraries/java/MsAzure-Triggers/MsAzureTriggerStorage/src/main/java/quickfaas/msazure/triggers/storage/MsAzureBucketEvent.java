/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.msazure.triggers.storage;

import quickfaas.triggers.storage.BucketEventQf;
import quickfaas.triggers.storage.EventTypeQf;

public class MsAzureBucketEvent implements BucketEventQf {

    private final String storageAccount;
    private final String container;
    public MsAzureBucketEvent(String storageAccount, String container) {
        this.storageAccount = storageAccount;
        this.container = container;
    }

    @Override
    public String getBucketName() {
        return storageAccount + '/' + container;
    }

    //TODO: To retrieve the type of event in MsAzure the usage of EventGrid service is required
    @Override
    public EventTypeQf getType() {
        return EventTypeQf.CREATE;
    }
}
