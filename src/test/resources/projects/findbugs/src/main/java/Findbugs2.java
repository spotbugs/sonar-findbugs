import java.util.Date;

public final class Findbugs2 {

  public Date getDate(int seconds) {
    // violation of the rule "int value converted to long and used as absolute time" that
    // is introduced in Findbugs 2.0
    return new Date(seconds * 1000);
  }
}
