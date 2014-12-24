package earay.serengeti.cli.frontend.model;

import java.io.Serializable;

public interface SerengetiResource extends Serializable {
	
	public String getName();
	
	public String toCommandAdd();
	
	public String expectedAddResult();
	
	public String toCommandDelete();
	
	public String expectedDeleteResult();

}
