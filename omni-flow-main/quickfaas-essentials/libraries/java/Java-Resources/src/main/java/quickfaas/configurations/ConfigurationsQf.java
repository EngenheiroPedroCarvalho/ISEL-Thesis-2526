/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.configurations;

import com.google.gson.JsonElement;

import static quickfaas.configurations.ConfigurationsManagerQf.getConfigurationAsJson;

public interface ConfigurationsQf {
    static JsonElement getConfiguration(String property) {
        return getConfigurationAsJson(property);
    }
}
