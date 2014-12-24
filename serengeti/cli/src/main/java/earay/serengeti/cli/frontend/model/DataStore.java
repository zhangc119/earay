package earay.serengeti.cli.frontend.model;

import lombok.Data;

import org.hibernate.validator.constraints.NotBlank;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ApiModel("data store to be added in serengeti server")
@Data
public class DataStore implements SerengetiResource {
	
	public static enum Type {
		Local, Shared
	}
	
	@ApiModelProperty(value = "type of network", required = true, allowableValues = "Local, Shared")
	private Type type;
	
	@ApiModelProperty(value = "datastore name to be recoginzed in seregenti server", required = true)
	@NotBlank
	private String name;
	
	@ApiModelProperty(value = "--spec parameter used in 'datastore add' command in serengeti cli", required = true)
	@NotBlank
	private String spec;
	
	@ApiModelProperty(value = "represents if spec is a regular expression")
	private boolean regex = false;

	@Override
	public String toCommandAdd() {
		StringBuffer command = new StringBuffer("datastore add --name ");
		command.append(name).append(" --spec ").append(spec).append(" --type ").append(type.toString().toUpperCase());
		if (regex)
			command.append(" --regex");
		return command.toString();
	}

	@Override
	public String expectedAddResult() {
		return "datastore " + name + " added";
	}

	@Override
	public String toCommandDelete() {
		return "datastore delete --name " + name;
	}

	@Override
	public String expectedDeleteResult() {
		return "datastore " + name + " deleted";
	}

}
