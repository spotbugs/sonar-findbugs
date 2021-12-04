import java.util.Date;

public final class File2 {
  public Date getDate(int seconds) {
    return new Date(seconds * 1000); // FindBugs: "int value converted to long and used as absolute time"
  }
}
