import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String name = req.getQueryParameter("name");
        if (name == null || name.isEmpty()) {
            name = "World";
        }
        res.send(200, "{\"message\": \"Hello, " + name + "! Deployed via QuickFaaS from OmniFlow.\"}");
    }
}
