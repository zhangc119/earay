package earay.serengeti.cli.frontend.model;

import lombok.Data;

import org.hibernate.validator.constraints.NotBlank;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ApiModel("resource pool to be added in serengeti server")
@Data
public class ResourcePool implements SerengetiResource  {
	
	@ApiModelProperty(value = "resource pool name to be recoginzed in seregenti server", required = true)
	@NotBlank
	private String name;
	
	@ApiModelProperty(value = "vCenter cluster, used for --vccluster in serengeti cli", required = true)
	@NotBlank
	private String vcCluster;
	
	@ApiModelProperty(value = "vCenter resource pool, used for --vcrp in serengeti cli", required = true)
	@NotBlank
	private String vcResourcePool;
	
	@Override
	public String toCommandAdd() {
		StringBuffer command = new StringBuffer("resourcepool add --name ");
		command.append(name).append(" --vccluster ").append(vcCluster).append(" --vcrp ").append(vcResourcePool);
		return command.toString();
	}
	
	@Override
	public String expectedAddResult() {
		return "resourcepool " + name + " added";
	}
	
	@Override
	public String toCommandDelete() {
		return "resourcepool delete --name " + name;
	}

	@Override
	public String expectedDeleteResult() {
		return "resourcepool " + name + " deleted";
	}

}
