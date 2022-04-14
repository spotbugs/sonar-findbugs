/**
 * 
 */
package multimodule.app;

import multimodule.core.SampleCore;
import multimodule.fx.SampleFx;

public class SampleApp {
	private SampleCore core;
	private SampleFx fx;

	public int npe() {
	  System.out.println("test".toString());
		return core.npe() + fx.npe();
	}
}
