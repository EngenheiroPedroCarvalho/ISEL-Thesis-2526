public class MyFunctionClass {
    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
        String name = req.getParameter("name");
        if (name == null || name.isEmpty()) {
            name = "World";
        }
        res.write("{\"message\": \"Hello, " + name + "! Deployed via QuickFaaS from OmniFlow.\"}");
    }
}
