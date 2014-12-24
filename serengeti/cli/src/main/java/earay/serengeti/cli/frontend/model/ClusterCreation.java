package earay.serengeti.cli.frontend.model;

import lombok.Data;
import net.sf.expectit.Expect;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import com.jcraft.jsch.JSchException;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import earay.base.frontend.resource.JSchREST;

@SuppressWarnings("serial")
@ApiModel("cluster add operation in serengeti cli")
@Data
public class ClusterCreation implements CliCommand {
	
	public static enum Type {
		Hadoop, HBase
	}
	
	public static enum Topology {
		HVE, RACK_AS_RACK, HOST_AS_RACK
	}
	
	@ApiModelProperty(required = true)
	@NotBlank
	private String name;
	
	@ApiModelProperty(value = "Cluster type is either Hadoop or HBase, no default value", allowableValues = "Hadoop, HBase")
	private Type type;
	
	@ApiModelProperty(value = "OS path to cluster specification file on serengeti server")
	private String specFile;
	
	@ApiModelProperty(value = "Distro for the cluster, which can be found by command 'distro list' or 'appmanager list --distros'")
	private String distro;
	
	@ApiModelProperty(value = "Name of 3rd party application manager registered in serengeti")
	private String appManager;
	
	@ApiModelProperty(value = "Local yum server URL for the application manager")
	private String localRepoURL;
	
	@ApiModelProperty(value = "Names of data stores registered in serengeti, connected by ','")
	private String dataStoreNames;
	
	@ApiModelProperty(value = "Names of resource pools registered in serengeti, connected by ','")
	private String resourcePoolNames;
	
	@ApiModelProperty(value = "Registered network name for serengeti management server")
	private String networkName;
	
	@ApiModelProperty(value = "Registered network name for HDFS traffic")
	private String hdfsNetworkName;
	
	@ApiModelProperty(value = "Registered network name for MapReduce traffic")
	private String mapredNetworkName;
	
	@ApiModelProperty(value = "Topology type for the cluster : HVE or RACK_AS_RACK or HOST_AS_RACK", 
			allowableValues = "HVE, RACK_AS_RACK, HOST_AS_RACK")
	private Topology topology;
	
	@ApiModelProperty(value = "Set password for all VMs in this cluster if this property is set")
	private String password;
	
	@ApiModelProperty(value = "Flag to resume cluster creation")
	private boolean resume;
	
	@ApiModelProperty(value = "Flag to kkip cluster configuration validation before cluster creation")
	private boolean skipConfigValidation;
	
	@ApiModelProperty(value = "Answer 'yes' to all Y/N questions if it's true")
	private boolean confirmYes;
	
	@ApiModelProperty(value = "time out in seconds for checking expectation of cluster creation")
	private int timeout;
	
	@Override
	public void operate(JSchREST jsch, Expect expect, int cliTimeout, int longOpsTimeout) throws Exception {
		if (timeout <= 0)
			timeout = longOpsTimeout;
		StringBuffer command = new StringBuffer("cluster create --name ").append(name);
		if (type != null)
			command.append(" --type ").append(type);
		if (StringUtils.trimToNull(specFile) != null)
			command.append(" --specFile ").append(specFile);
		if (StringUtils.trimToNull(distro) != null)
			command.append(" --distro ").append(distro);
		if (StringUtils.trimToNull(appManager) != null)
			command.append(" --appManager ").append(appManager);
		if (StringUtils.trimToNull(localRepoURL) != null)
			command.append(" --localRepoURL ").append(localRepoURL);
		if (StringUtils.trimToNull(dataStoreNames) != null)
			command.append(" --dsNames ").append(dataStoreNames);
		if (StringUtils.trimToNull(resourcePoolNames) != null)
			command.append(" --rpNames ").append(resourcePoolNames);
		if (StringUtils.trimToNull(networkName) != null)
			command.append(" --networkName ").append(networkName);
		if (StringUtils.trimToNull(hdfsNetworkName) != null)
			command.append(" --hdfsNetworkName ").append(hdfsNetworkName);
		if (StringUtils.trimToNull(mapredNetworkName) != null)
			command.append(" --mapredNetworkName ").append(mapredNetworkName);
		if (topology != null)
			command.append(" --topology ").append(topology);
		if (resume)
			command.append(" --resume");
		if (skipConfigValidation)
			command.append(" --skipConfigValidation");
		if (confirmYes)
			command.append(" --yes");
		password = StringUtils.trimToNull(password);
		String input;
		if (password != null) {
			command.append(" --password");
			jsch.expectSuccessOrFailuare(expect, command.toString(), "Enter the password:", serengetiCLIPrompt, true, cliTimeout);
			jsch.expectSuccessOrFailuare(expect, password, "Confirm the password:", serengetiCLIPrompt, true, cliTimeout);
			input = password;
		} else
			input = command.toString();
		try {
			jsch.expectSuccessOrFailuare(expect, input, "cluster " + name + " created\r\n"
					+ serengetiCLIPrompt, serengetiCLIPrompt, true, timeout);
		} catch (JSchException e) {
			String message = e.getMessage();
			String failure = "cluster " + name + " create failed:";
			int index = message.indexOf(failure);
			if (index >= 0)
				throw new JSchException(message.substring(index + failure.length(), message.lastIndexOf("\r\n" + serengetiCLIPrompt)));
		}
	}

	@Override
	public void clean(JSchREST jsch, Expect expect, int cliTimeout, int longOpsTimeout) {
		
	}

}
