/**
 * 
 */
package multimodule.core;

import javax.annotation.Nonnegative;
import org.junit.jupiter.api.Test;

public class SampleCoreTest {
  private String field;

  @Test
  public int npe() {
    System.out.println("test".toString());
    return field.hashCode();
  }

  @Test
  public @Nonnegative int nonnegative() {
    if (field == null) {
      return -5;
    } else {
      return 42;
    }
  }
}
