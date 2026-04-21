import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String amount;
        try {
            amount = req.getQueryParameter("amount");
        } catch (Exception e) {
            res.send(200, "{\"valid\": false, \"reason\": \"missing amount\"}");
            return;
        }
        if (amount == null || amount.isEmpty()) {
            res.send(200, "{\"valid\": false, \"reason\": \"missing amount\"}");
            return;
        }

        double value;
        try {
            value = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            res.send(200, "{\"valid\": false, \"reason\": \"invalid number\"}");
            return;
        }

        boolean valid = value > 0 && value < 10000;
        String reason = valid ? "ok" : "amount out of range";
        res.send(200, "{\"valid\": " + valid + ", \"reason\": \"" + reason + "\"}");
    }
}
