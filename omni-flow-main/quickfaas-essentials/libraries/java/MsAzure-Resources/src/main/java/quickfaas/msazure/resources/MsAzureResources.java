/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.msazure.resources;

import quickfaas.msazure.resources.storage.MsAzureStorage;
import quickfaas.resources.CloudResourcesQf;
import quickfaas.resources.compute.ComputeQf;
import quickfaas.resources.database.DatabaseQf;
import quickfaas.resources.storage.StorageQf;

public class MsAzureResources implements CloudResourcesQf {
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
        return new MsAzureStorage();
    }
}
