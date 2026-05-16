/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.configurations;

import com.google.gson.JsonObject;

import java.util.List;

class ConfigurationsDataQf {
    List<ResourceDataQf> resources;

    static class ResourceDataQf {
        String id;
        String type;
        JsonObject properties;
    }

}
