package earay.serengeti.cli.frontend.model;

import java.io.Serializable;

import earay.base.frontend.resource.JSchREST;
import net.sf.expectit.Expect;

public interface CliCommand extends Serializable {
	
	public static final String serengetiCLIPrompt = "serengeti>";
	
	public void operate(JSchREST jsch, Expect expect, int cliTimeout, int longOpsTimeout) throws Exception;
	
	public void clean(JSchREST jsch, Expect expect, int cliTimeout, int longOpsTimeout);

}
