import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String text;
        try {
            text = req.getQueryParameter("text");
        } catch (Exception e) {
            res.send(200, "{\"totalLength\": 0, \"wordCount\": 0, \"averageLength\": 0}");
            return;
        }
        if (text == null || text.isEmpty()) {
            res.send(200, "{\"totalLength\": 0, \"wordCount\": 0, \"averageLength\": 0}");
            return;
        }

        String[] words = text.trim().split("\\s+");
        int totalLength = 0;
        for (String word : words) {
            totalLength += word.length();
        }
        int wordCount = words.length;
        double avg = wordCount > 0 ? (double) totalLength / wordCount : 0;

        res.send(200, "{\"totalLength\": " + totalLength
                + ", \"wordCount\": " + wordCount
                + ", \"averageLength\": " + String.format("%.1f", avg) + "}");
    }
}
