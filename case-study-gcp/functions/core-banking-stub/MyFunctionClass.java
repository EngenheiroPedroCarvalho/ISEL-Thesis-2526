import com.google.gson.JsonParser;
import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

// Stand-in for the core banking system: acknowledges the decision it receives.
// Parses the body itself: getFromJsonBody rejects Cloud Workflows' 'application/json; charset=utf-8'.
public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String decision = req.getQueryParameter("decision");
        String id = JsonParser.parseString(req.getBody()).getAsJsonObject().get("id").getAsString();
        res.setContentType("application/json");
        res.send(200, "{\"recorded\": true, \"id\": \"" + id + "\", \"decision\": \"" + decision + "\"}");
    }
}
