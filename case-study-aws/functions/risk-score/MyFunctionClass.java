import com.amazonaws.services.lambda.runtime.Context;
import java.util.Map;

// Scores a transaction from 0 to 100, growing with its amount.
public class MyFunctionClass {
    public Map<String, Object> handleRequest(Map<String, String> query, Context context) {
        double amount = Double.parseDouble(query.get("amount"));
        return Map.of("score", Math.min(100, Math.round(amount / 50)));
    }
}
