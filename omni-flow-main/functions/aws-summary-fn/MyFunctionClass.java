import com.amazonaws.services.lambda.runtime.Context;
import java.util.HashMap;
import java.util.Map;

public class MyFunctionClass {

    public Map<String, Object> handleRequest(Map<String, String> queryParams, Context context) {
        String rawText = queryParams.getOrDefault("text", "");
        if (rawText.startsWith("[") && rawText.endsWith("]") && rawText.length() > 2)
            rawText = rawText.substring(1, rawText.length() - 1);

        String rawLang = queryParams.getOrDefault("lang", "en");
        if (rawLang.startsWith("[") && rawLang.endsWith("]") && rawLang.length() > 2)
            rawLang = rawLang.substring(1, rawLang.length() - 1);

        String text = rawText.trim();
        String lang = rawLang.trim().toLowerCase();
        String[] words = text.isEmpty() ? new String[0] : text.split("\\s+");
        int wordCount = words.length;

        int totalChars = 0;
        for (String w : words) totalChars += w.length();
        double avg = wordCount > 0 ? (double) totalChars / wordCount : 0.0;
        double avgRounded = Math.round(avg * 10.0) / 10.0;

        String summary;
        switch (lang) {
            case "pt":
                summary = String.format("O texto tem %d palavras e %.1f caracteres em média.", wordCount, avgRounded);
                break;
            case "es":
                summary = String.format("El texto tiene %d palabras y %.1f caracteres en promedio.", wordCount, avgRounded);
                break;
            case "fr":
                summary = String.format("Le texte a %d mots et %.1f caractères en moyenne.", wordCount, avgRounded);
                break;
            case "de":
                summary = String.format("Der Text hat %d Wörter und %.1f Zeichen im Durchschnitt.", wordCount, avgRounded);
                break;
            default:
                summary = String.format("The text has %d words and %.1f characters on average.", wordCount, avgRounded);
                break;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("language", lang);
        result.put("wordCount", wordCount);
        result.put("avgWordLength", avgRounded);
        return result;
    }
}
