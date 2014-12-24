package earay.serengeti.install;

import lombok.Getter;
import earay.base.EarayProject;
import earay.serengeti.install.frontend.resource.SerengetiDeployREST;

@Getter
public class SerengetiInstallProject implements EarayProject {
	
	private String resourcePackage = SerengetiDeployREST.class.getPackage().getName();

}
