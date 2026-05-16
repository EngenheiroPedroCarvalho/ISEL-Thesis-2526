/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources;

import static quickfaas.configurations.ConfigurationsManagerQf.getConfiguration;

public class CloudResourcesManagerQf {

    private static CloudResourcesQf cloudRes;

    /** Invoked by template functions **/
    public static void setCloudResources(CloudResourcesQf cloudResources) {
        if (cloudRes == null) cloudRes = cloudResources;
    }

    protected static CloudResourcesQf getCloudResources() {
        return cloudRes;
    }

    protected static String getResourceProperties(String resourceId, String resourceType) {
        return getConfiguration(resourceKey(resourceId, resourceType));
    }

    public static String resourceKey(String resourceId, String resourceType) {
        return resourceId + '/' + resourceType + "/resource";
    }

}
