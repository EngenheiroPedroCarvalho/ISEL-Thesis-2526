import quickfaas.triggers.http.HttpRequestQf;
import quickfaas.triggers.http.HttpResponseQf;

public class MyFunctionClass {

    public void myFunction(HttpRequestQf req, HttpResponseQf res) {
	res.send(200, "Hello world!");
    }

}
