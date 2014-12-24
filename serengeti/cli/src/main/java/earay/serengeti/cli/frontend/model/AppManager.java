package earay.serengeti.cli.frontend.model;

import lombok.Data;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ApiModel("application manager to be registered in serengeti server")
@Data
public class AppManager implements SerengetiResource {
	
	public static enum Type {
		ClouderaManager, Ambari
	}
	
	@ApiModelProperty(value = "name of 3rd party hadoop application manager to be recoginzed in serengeti server", required = true)
	@NotBlank
	private String name;
	
	@ApiModelProperty(value = "3rd party hadoop application manager url", required = true)
	@NotBlank
	@URL
	private String appManagerURL;
	
	@ApiModelProperty(value = "3rd party hadoop application manager type", required = true, allowableValues = "ClouderaManager, Ambari")
	private Type type;
	
	@ApiModelProperty(value = "description on 3rd party hadoop application manager", required = true)
	@NotBlank
	private String description;
	
	@ApiModelProperty(value = "account to manage 3rd party hadoop application manager", required = true)
	@NotBlank
	private String userName;
	
	@ApiModelProperty(value = "account password to manage 3rd party hadoop application manager", required = true)
	private String password;
	
	@ApiModelProperty(value = "file path of certificate if https protocol is used")
	private String certificate;

	@Override
	public String toCommandAdd() {
		StringBuffer command = new StringBuffer("appmanager add --name ");
		command.append(name).append(" --description ").append(description).append(" --type ").append(type).append(" --url ").append(appManagerURL);
		return command.toString();
	}

	@Override
	public String expectedAddResult() {
		return "appmanager " + name + " added";
	}
	
	@Override
	public String toCommandDelete() {
		return "appmanager delete --name " + name;
	}

	@Override
	public String expectedDeleteResult() {
		return "appmanager " + name + " deleted";
	}
	
}
