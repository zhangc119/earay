package earay.serengeti.cli;

import earay.base.EarayApplication;

public class SerengetiCLI extends EarayApplication {
	
	@Override
	public String getName() {
		return "Serengeti CLI";
	}
	
	public static void main(String[] args) throws Exception {
		if (args == null || args.length == 0)
			args = new String[] {"server", "earay.yml"};
		new SerengetiCLI().run(args);
	}

}
