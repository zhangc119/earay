package earay.serengeti.install;

import earay.base.EarayApplication;

public class SerengetiInstaller extends EarayApplication {
	
	@Override
	public String getName() {
		return "Serengeti Installer";
	}
	
	public static void main(String[] args) throws Exception {
		new SerengetiInstaller().run(args);
	}

}
