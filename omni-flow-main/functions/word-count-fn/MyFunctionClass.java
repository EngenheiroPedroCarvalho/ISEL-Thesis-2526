import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String valuesStr;
        try {
            valuesStr = req.getQueryParameter("values");
        } catch (Exception e) {
            res.send(200, "{\"totalLength\": 0, \"wordCount\": 0, \"error\": \"missing values parameter\"}");
            return;
        }
        if (valuesStr == null || valuesStr.isEmpty()) {
            res.send(200, "{\"totalLength\": 0, \"wordCount\": 0, \"error\": \"empty values\"}");
            return;
        }

        String[] parts = valuesStr.split(",");
        int total = 0;
        int count = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    total += Integer.parseInt(trimmed);
                    count++;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        res.send(200, "{\"totalLength\": " + total + ", \"wordCount\": " + count
                + ", \"averageLength\": " + (count > 0 ? String.format("%.1f", (double) total / count) : "0")
                + "}");
    }
}
