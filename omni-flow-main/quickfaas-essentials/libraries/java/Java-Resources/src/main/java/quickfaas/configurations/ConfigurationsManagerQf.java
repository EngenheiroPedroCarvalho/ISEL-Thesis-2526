/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.configurations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import quickfaas.configurations.exceptions.NoSuchConfigurationException;

import java.util.HashMap;
import java.util.Map;

import static quickfaas.resources.CloudResourcesManagerQf.resourceKey;

public final class ConfigurationsManagerQf {

    private static final Map<String, String> configsMap = new HashMap<>();
    private static final Gson gson = new Gson();
    private static String configsJson;
    private static JsonObject configsJsonObj;

    /** Invoked by template functions **/
    public static void setConfigurations(String configurationsJson) {
        if (configsMap.isEmpty()) {
            configsJson = configurationsJson;
            deserializeConfigurations();
        }
    }

    private static void deserializeConfigurations() {
        ConfigurationsDataQf configurations = gson.fromJson(configsJson, ConfigurationsDataQf.class);
        if (configurations == null) return;
        if (configurations.resources != null) {
            for (ConfigurationsDataQf.ResourceDataQf resource : configurations.resources) {
                configsMap.put(resourceKey(resource.id, resource.type), resource.properties.toString());
            }
        }
    }

    public static String getConfiguration(String key) {
        return configsMap.get(key);
    }

    static JsonElement getConfigurationAsJson(String property) {
        if (configsJsonObj == null) configsJsonObj = gson.fromJson(configsJson, JsonObject.class);
        if (!configsJsonObj.has(property)) throw new NoSuchConfigurationException(property);
        return configsJsonObj.get(property);
    }

}
