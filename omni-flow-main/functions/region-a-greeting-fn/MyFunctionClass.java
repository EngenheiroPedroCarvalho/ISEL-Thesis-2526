import com.amazonaws.services.lambda.runtime.Context;
import java.util.HashMap;
import java.util.Map;

public class MyFunctionClass {

    public Map<String, Object> handleRequest(Map<String, String> queryParams, Context context) {
        String lang = queryParams.getOrDefault("lang", "en");

        String greeting;
        switch (lang.toLowerCase()) {
            case "pt":
                greeting = "Ola, Mundo!";
                break;
            case "es":
                greeting = "Hola, Mundo!";
                break;
            case "fr":
                greeting = "Bonjour, le Monde!";
                break;
            case "de":
                greeting = "Hallo, Welt!";
                break;
            default:
                greeting = "Hello, World!";
                break;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("greeting", greeting);
        result.put("language", lang);
        return result;
    }
}
