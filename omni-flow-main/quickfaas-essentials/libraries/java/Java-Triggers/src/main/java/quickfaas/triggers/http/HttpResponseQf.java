/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.triggers.http;

public interface HttpResponseQf {

    void appendHeader(String header, String value);

    void send(int status, String body);

    void setContentType(String contentType);

}
