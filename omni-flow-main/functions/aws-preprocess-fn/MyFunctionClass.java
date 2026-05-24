import com.amazonaws.services.lambda.runtime.Context;
import java.util.HashMap;
import java.util.Map;

public class MyFunctionClass {

    public Map<String, Object> handleRequest(Map<String, String> queryParams, Context context) {
        String raw = queryParams.getOrDefault("text", "");
        // Step Functions States.Array wraps values in brackets: "[value]" → strip them
        if (raw.startsWith("[") && raw.endsWith("]") && raw.length() > 2)
            raw = raw.substring(1, raw.length() - 1);

        String normalized = raw.trim().toLowerCase();
        String[] words = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");

        Map<String, Object> result = new HashMap<>();
        result.put("original", raw.trim());
        result.put("normalized", normalized);
        result.put("wordCount", words.length);
        result.put("isEmpty", normalized.isEmpty());
        return result;
    }
}
