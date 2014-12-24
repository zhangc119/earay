package earay.serengeti.cli.frontend.model;

import io.dropwizard.validation.ValidationMethod;
import lombok.Data;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ApiModel("network to be added in serengeti server")
@Data
public class Network implements SerengetiResource {
	
	public static enum Type {
		Static, Dhcp
	}
	
	@ApiModelProperty(value = "type of network", required = true, allowableValues = "Static, Dhcp")
	private Type type;
	
	@ApiModelProperty(value = "network name to be recoginzed in seregenti server", required = true)
	@NotBlank
	private String name;
	
	@ApiModelProperty(value = "network port group in vCenter", required = true)
	@NotBlank
	private String portGroup;
	
	@ApiModelProperty(value = "ip range for static network, formatted as 'xx.xx.xx.xx-xx[,xx]*', e.g. 'xx.xx.xx.xx-xx, xx.xx.xx.xx-xx, single_ip, single_ip'")
	private String ip;
	
	@ApiModelProperty(value = "dns ip address for static network")
	private String dns;
	
	@ApiModelProperty(value = "secondary dns ip address for static network")
	private String secondDns;
	
	@ApiModelProperty(value = "gateway ip address for static network")
	private String gateway;
	
	@ApiModelProperty(value = "network mask for static network")
	private String mask;
	
	@JsonIgnore
	@ValidationMethod(message="must set ip, dns, gateway and mask for static network")
	public boolean isValidForStatic() {
		if (type == Type.Static)
			return StringUtils.trimToNull(ip) != null
					&& StringUtils.trimToNull(dns) != null
					&& StringUtils.trimToNull(gateway) != null
					&& StringUtils.trimToNull(mask) != null;
		return true;
	}

	@Override
	public String toCommandAdd() {
		StringBuffer command = new StringBuffer("network add --name ");
		command.append(name).append(" --portGroup ").append(portGroup);
		if (type == Type.Dhcp)
			command.append(" --dhcp");
		else {
			command.append(" --ip ").append(ip).append(" --dns ").append(dns)
					.append(" --gateway ").append(gateway).append(" --mask ").append(mask);
			if (StringUtils.trimToNull(secondDns) != null)
				command.append(" --secondDNS ").append(secondDns);
		}
		return command.toString();
	}

	@Override
	public String expectedAddResult() {
		return "network " + name + " added";
	}

	@Override
	public String toCommandDelete() {
		return "network delete --name " + name;
	}

	@Override
	public String expectedDeleteResult() {
		return "network " + name + " deleted";
	}

}
