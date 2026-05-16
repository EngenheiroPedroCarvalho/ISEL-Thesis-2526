import quickfaas.triggers.storage.BlobQf;
import quickfaas.triggers.storage.BucketEventQf;

public class MyFunctionClass {

    public void myFunction(BucketEventQf event, BlobQf blob) {
        System.out.println("Processing file: " + blob.getName());
    }

}