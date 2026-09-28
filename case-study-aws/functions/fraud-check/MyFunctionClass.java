import com.amazonaws.services.lambda.runtime.Context;
import java.util.Map;

// Flags a transaction as suspicious when it is a high-value payment made abroad.
public class MyFunctionClass {
    public Map<String, Object> handleRequest(Map<String, String> query, Context context) {
        double amount = Double.parseDouble(query.get("amount"));
        String country = query.get("country");
        return Map.of("flagged", amount > 1000 && !"PT".equals(country));
    }
}
