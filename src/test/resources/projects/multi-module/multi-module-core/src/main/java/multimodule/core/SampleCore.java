/**
 * 
 */
package multimodule.core;

import javax.annotation.Nonnegative;

public class SampleCore {
	private String field;

	public int npe() {
    System.out.println("test".toString());
		return field.hashCode();
	}
	
	public @Nonnegative int nonnegative() {
		if (field == null) {
			return -5;
		} else {
			return 42;
		}
	}
}
