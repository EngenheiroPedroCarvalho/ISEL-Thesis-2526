import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String text;
        try {
            text = req.getQueryParameter("text");
        } catch (Exception e) {
            res.send(200, "{\"words\": [], \"error\": \"missing text parameter\"}");
            return;
        }
        if (text == null || text.isEmpty()) {
            res.send(200, "{\"words\": [], \"error\": \"empty text\"}");
            return;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(words[i].replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");

        res.send(200, "{\"words\": " + sb.toString() + "}");
    }
}
