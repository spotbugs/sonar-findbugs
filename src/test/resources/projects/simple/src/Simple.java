import java.util.Properties;
import java.util.Random;

public class Simple {
  public void method() {
    Random r = new Random();
    String token = Long.toHexString(r.nextLong()); // fb-contrib violation (find security bugs)
    new Properties().put("key", (Object) null); // fb-contrib violation
    "".toString(); // findbugs violation
  }
}
