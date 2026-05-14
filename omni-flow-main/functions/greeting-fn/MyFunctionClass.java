import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String lang;
        try {
            lang = req.getQueryParameter("lang");
        } catch (Exception e) {
            lang = "en";
        }
        if (lang == null || lang.isEmpty()) {
            lang = "en";
        }

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

        res.send(200, "{\"greeting\": \"" + greeting + "\", \"language\": \"" + lang + "\"}");
    }
}
