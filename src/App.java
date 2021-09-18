import utilities.Config;
import utilities.NetworkInfo;

public class App {

    public static void main(String[] args) throws Exception {
        NetworkInfo networkInfo = NetworkInfo.getInstance();

        Config config = new Config("/local-test/config");
        config.readFile();
        System.out.println("Host name: " + networkInfo.getHostName());
    }
}
