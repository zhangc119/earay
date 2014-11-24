package earay.bigdata.bigtop;

import earay.base.EarayApplication;

public class BigtopApplication extends EarayApplication {
	
	@Override
	public String getName() {
		return "Hadoop";
	}
	
	public static void main(String[] args) throws Exception {
		new BigtopApplication().run(args);
	}

}
