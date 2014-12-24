package earay.serengeti.cli;

import earay.base.EarayApplication;

public class SerengetiCLI extends EarayApplication {
	
	@Override
	public String getName() {
		return "Serengeti CLI";
	}
	
	public static void main(String[] args) throws Exception {
		new SerengetiCLI().run(args);
	}

}
