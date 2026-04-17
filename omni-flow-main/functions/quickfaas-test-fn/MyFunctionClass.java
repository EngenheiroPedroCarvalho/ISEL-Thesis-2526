import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.BufferedWriter;

public class MyFunctionClass implements HttpFunction {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String name = request.getFirstQueryParameter("name").orElse("World");

        response.setContentType("application/json");
        BufferedWriter writer = response.getWriter();
        writer.write("{\"message\": \"Hello, " + name + "! Deployed via QuickFaaS from OmniFlow.\"}");
    }
}
