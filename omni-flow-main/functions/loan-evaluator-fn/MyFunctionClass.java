import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String incomeStr, loanAmountStr;
        try {
            incomeStr = req.getQueryParameter("income");
            loanAmountStr = req.getQueryParameter("loanAmount");
        } catch (Exception e) {
            res.send(200, "{\"approved\": false, \"reason\": \"missing parameters\", \"maxLoanAmount\": 0}");
            return;
        }
        if (incomeStr == null || incomeStr.isEmpty() || loanAmountStr == null || loanAmountStr.isEmpty()) {
            res.send(200, "{\"approved\": false, \"reason\": \"missing parameters\", \"maxLoanAmount\": 0}");
            return;
        }

        double income, loanAmount;
        try {
            income = Double.parseDouble(incomeStr);
            loanAmount = Double.parseDouble(loanAmountStr);
        } catch (NumberFormatException e) {
            res.send(200, "{\"approved\": false, \"reason\": \"invalid number format\", \"maxLoanAmount\": 0}");
            return;
        }

        if (income <= 0) {
            res.send(200, "{\"approved\": false, \"reason\": \"income must be positive\", \"maxLoanAmount\": 0}");
            return;
        }

        double maxLoan = income * 5;
        boolean approved = loanAmount > 0 && loanAmount <= maxLoan;
        String reason = approved
                ? "Loan within acceptable debt-to-income ratio"
                : "Requested amount exceeds maximum allowed (" + String.format("%.0f", maxLoan) + ")";

        res.send(200, "{\"approved\": " + approved
                + ", \"reason\": \"" + reason + "\""
                + ", \"maxLoanAmount\": " + String.format("%.0f", maxLoan) + "}");
    }
}
