/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.gcp.resources;

import quickfaas.gcp.resources.storage.GcpStorage;
import quickfaas.resources.CloudResourcesQf;
import quickfaas.resources.compute.ComputeQf;
import quickfaas.resources.database.DatabaseQf;
import quickfaas.resources.storage.StorageQf;

public class GcpResources implements CloudResourcesQf {
    @Override
    public ComputeQf compute() {
        return null;
    }

    @Override
    public DatabaseQf database() {
        return null;
    }

    @Override
    public StorageQf storage() {
        return new GcpStorage();
    }
}
