package earay.bigdata.bigtop;

import earay.base.EarayApplication;

public class BigtopApplication extends EarayApplication {
	
	@Override
	public String getName() {
		return "Hadoop";
	}
	
	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0)
			args = new String[] {"server", "earay.yml"};
		new BigtopApplication().run(args);
	}

}
