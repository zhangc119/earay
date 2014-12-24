package earay.serengeti.cli.frontend.model;

import java.io.Serializable;

import lombok.Data;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ApiModel("vCenter information used in serengeti server")
@Data
public class VCenterInfo implements Serializable {
	
	@ApiModelProperty(value = "name of vCenter server where serengeti is deployed", required = true)
	private String name;
	
	@ApiModelProperty(value = "host name of vCenter server", required = true)
	private String hostName;
	
	@ApiModelProperty(value = "account to sign on vCenter server", required = true)
	private String userName;
	
	@ApiModelProperty(value = "account password to sign on vCenter server", required = true)
	private String password;
	
	@ApiModelProperty(value = "vCenter product edition", allowableValues = "Enterprise, Standard, Essential")
	private String edition = "Enterprise";
	
	@ApiModelProperty(value = "vCenter release version", allowableValues = "5.1, 5.5, 6.0")
	private String version = "5.5";
	
	@ApiModelProperty(value = "vCenter folder for hadoop cluster provision")
	private String provisionFolder;
	
	@ApiModelProperty(value = "DataCenter where serengeti is deployed", required = true)
	private String dataCenter;

}
