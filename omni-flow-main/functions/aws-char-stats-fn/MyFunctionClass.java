import com.amazonaws.services.lambda.runtime.Context;
import java.util.HashMap;
import java.util.Map;

public class MyFunctionClass {

    public Map<String, Object> handleRequest(Map<String, String> queryParams, Context context) {
        String raw = queryParams.getOrDefault("text", "");
        if (raw.startsWith("[") && raw.endsWith("]") && raw.length() > 2)
            raw = raw.substring(1, raw.length() - 1);

        String text = raw.trim();
        String[] words = text.isEmpty() ? new String[0] : text.split("\\s+");

        int totalChars = 0;
        String longestWord = "";
        String shortestWord = words.length > 0 ? words[0] : "";
        for (String word : words) {
            totalChars += word.length();
            if (word.length() > longestWord.length()) longestWord = word;
            if (word.length() < shortestWord.length()) shortestWord = word;
        }

        int wordCount = words.length;
        double avg = wordCount > 0 ? (double) totalChars / wordCount : 0.0;
        double rounded = Math.round(avg * 10.0) / 10.0;

        Map<String, Object> result = new HashMap<>();
        result.put("wordCount", wordCount);
        result.put("totalChars", totalChars);
        result.put("avgWordLength", rounded);
        result.put("longestWord", longestWord);
        result.put("shortestWord", shortestWord);
        return result;
    }
}
