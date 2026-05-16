/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.triggers.http;

import com.google.gson.JsonElement;

public interface HttpRequestQf {

    String getBody();

    String getContentType();

    JsonElement getFromJsonBody(String property);

    String getHttpMethod();

    String getQueryParameter(String parameter);

}
