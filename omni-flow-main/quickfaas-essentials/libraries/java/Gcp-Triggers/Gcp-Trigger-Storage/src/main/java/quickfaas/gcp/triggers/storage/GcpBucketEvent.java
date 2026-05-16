/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.triggers.storage;

import quickfaas.triggers.storage.BucketEventQf;
import quickfaas.triggers.storage.EventTypeQf;

public class GcpBucketEvent implements BucketEventQf {

    private final String eventType;
    private final EventData eventData;
    public GcpBucketEvent(String eventType, EventData eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    @Override
    public String getBucketName() {
        return eventData.bucket;
    }

    @Override
    public EventTypeQf getType() {
        EventTypeQf type;
        switch (eventType) {
            case "google.storage.object.finalize":
                type = EventTypeQf.CREATE;
                break;
            case "google.storage.object.delete":
                type = EventTypeQf.DELETE;
                break;
            case "google.storage.object.metadataUpdate":
                type = EventTypeQf.UPDATE;
                break;
            default:
                type = EventTypeQf.CUSTOM;
        }
        return type;
    }
}
