import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String word;
        try {
            word = req.getQueryParameter("word");
        } catch (Exception e) {
            res.send(200, "{\"length\": 0, \"error\": \"missing word parameter\"}");
            return;
        }
        if (word == null || word.isEmpty()) {
            res.send(200, "{\"length\": 0, \"error\": \"empty word\"}");
            return;
        }

        res.send(200, "{\"word\": \"" + word.replace("\"", "\\\"") + "\", \"length\": " + word.length() + "}");
    }
}
