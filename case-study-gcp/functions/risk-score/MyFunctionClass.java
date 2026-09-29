import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

// Scores a transaction from 0 to 100, growing with its amount.
public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        double amount = Double.parseDouble(req.getQueryParameter("amount"));
        long score = Math.min(100, Math.round(amount / 50));
        res.setContentType("application/json");
        res.send(200, "{\"score\": " + score + "}");
    }
}
