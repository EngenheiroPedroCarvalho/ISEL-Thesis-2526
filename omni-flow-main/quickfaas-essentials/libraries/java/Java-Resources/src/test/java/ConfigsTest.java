/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

import com.google.gson.JsonElement;
import quickfaas.configurations.ConfigurationsManagerQf;
import quickfaas.configurations.ConfigurationsQf;

import static quickfaas.resources.CloudResourcesManagerQf.resourceKey;

public class ConfigsTest {
    public static void main(String[] args) {
        ConfigurationsManagerQf.setConfigurations("{\n" +
                "  \"resources\": [\n" +
                "    {\n" +
                "      \"id\": \"bucket1sources/quick-container\",\n" +
                "      \"type\": \"storage\",\n" +
                "      \"properties\": {\n" +
                "        \"accessKey\": \"someKey\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        String config1 = ConfigurationsManagerQf.getConfiguration(resourceKey("bucket1sources/quick-container", "storage"));
        String config2 = ConfigurationsQf.getConfiguration("customKey").getAsString();
        System.out.println(config1);
        System.out.println(config2);
    }
}
