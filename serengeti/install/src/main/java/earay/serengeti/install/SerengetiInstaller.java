package earay.serengeti.install;

import earay.base.EarayApplication;

public class SerengetiInstaller extends EarayApplication {
	
	@Override
	public String getName() {
		return "Serengeti Installer";
	}
	
	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0)
			args = new String[] {"server", "earay.yml"};
		new SerengetiInstaller().run(args);
	}

}
