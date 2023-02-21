import com.phuong.proxy.Internet;
import com.phuong.proxy.ProxyInternet;

public class Main {
    public static void main(String[] args) {
        Internet internet = new ProxyInternet();
        try {
            internet.connectTo("www.google.com");
            System.out.println("Connection established to www.google.com");
        } catch (Exception e) {
            System.out.println("Unable to connect to www.google.com: " + e.getMessage());
        }

        try {
            internet.connectTo("def.com");
            System.out.println("Connection established to abc.com");
        } catch (Exception e) {
            System.out.println("Unable to connect to abc.com: " + e.getMessage());
        }
    }
}