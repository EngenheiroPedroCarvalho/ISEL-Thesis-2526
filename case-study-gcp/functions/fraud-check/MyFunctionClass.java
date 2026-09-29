import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

// Flags a transaction as suspicious when it is a high-value payment made abroad.
public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        double amount = Double.parseDouble(req.getQueryParameter("amount"));
        String country = req.getQueryParameter("country");
        boolean flagged = amount > 1000 && !"PT".equals(country);
        res.setContentType("application/json");
        res.send(200, "{\"flagged\": " + flagged + "}");
    }
}
