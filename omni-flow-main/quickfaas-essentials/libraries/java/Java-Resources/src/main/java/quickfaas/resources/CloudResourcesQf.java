/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources;

import quickfaas.resources.compute.ComputeQf;
import quickfaas.resources.database.DatabaseQf;
import quickfaas.resources.storage.StorageQf;

public interface CloudResourcesQf {

    ComputeQf compute();

    DatabaseQf database();

    StorageQf storage();

}
