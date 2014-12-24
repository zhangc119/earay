package earay.serengeti.cli;

import lombok.Getter;
import earay.base.EarayProject;
import earay.serengeti.cli.frontend.resource.SerengetiCLIREST;

@Getter
public class SerengetiCLIProject implements EarayProject {
	
	private String resourcePackage = SerengetiCLIREST.class.getPackage().getName();
	
	private int cliTimeout = 10;
	
	private int longOpsTimeout = 1200;

}
