package earay.serengeti.cli.frontend.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.expectit.Expect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jcraft.jsch.JSchException;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import earay.base.frontend.model.FilePathOrContent;
import earay.base.frontend.resource.GroovyREST;
import earay.base.frontend.resource.JSchREST;
import groovy.lang.Binding;

@ApiModel("Resources to be registered in serengeti server")
@Data
@Slf4j
@SuppressWarnings("serial")
public class ResourceAdd implements CliCommand {
	
	@ApiModelProperty(value = "resource pools to be added in serengeti server", required = true)
	@Valid
	private Set<ResourcePool> resourcePools;
	
	@ApiModelProperty(value = "data stores to be added in serengeti server", required = true)
	@Valid
	private Set<DataStore> dataStores;
	
	@ApiModelProperty(value = "networks to be added in serengeti server", required = true)
	@Valid
	private Set<Network> networks;
	
	@ApiModelProperty(value = "all 3rd party hadoop application managers to be registered in serengeti server")
	@Valid
	private Set<AppManager> appManagers;
	
	@ApiModelProperty(value = "groovy script to be triggered after resources are handled")
	@Valid
	private FilePathOrContent groovyScript;
	
	@ApiModelProperty(value = "remove all newly added resource pools, data stores, networks and app managers in this request")
	private boolean cleanResourcesFinally;
	
	@JsonIgnore
	private Set<SerengetiResource> addedResources = new HashSet<SerengetiResource>();

	@Override
	public void operate(JSchREST jsch, Expect expect, int cliTimeout, int longOpsTimeout) throws Exception {
		addResources(jsch, expect, resourcePools, cliTimeout);
		addResources(jsch, expect, dataStores, cliTimeout);
		addResources(jsch, expect, networks, cliTimeout);
		addResources(jsch, expect, appManagers, cliTimeout);
		if (groovyScript != null) {
			Binding binding = new Binding();
			binding.setVariable("expect", expect);
			binding.setVariable("cliTimeout", cliTimeout);
			binding.setVariable("longOpsTimeout", longOpsTimeout);
			if (groovyScript.getContent() != null)
				GroovyREST.runShell(groovyScript.getContent(), binding);
			else
				GroovyREST.gse.run(groovyScript.getFile(), binding);
		}
	}

	@Override
	public void clean(JSchREST jsch, Expect expect, int cliTimeout, int longOpsTimeout) {
		if (cleanResourcesFinally) {
			for (SerengetiResource resource : addedResources) {
				try {
					jsch.expectSuccessOrFailuare(expect, resource.toCommandDelete(), resource.expectedDeleteResult() + "\r\n" + serengetiCLIPrompt, serengetiCLIPrompt, true, cliTimeout);
				} catch (Exception e) {
					log.warn("Failed to delete " + resource.getClass().getName() + " '" + resource.getName() + "'", e);
				}
			}
		}
	}
	
	private void addResources(JSchREST jsch, Expect expect, Set<? extends SerengetiResource> resources, int cliTimeout) throws Exception {
		if (resources == null)
			return;
		for (SerengetiResource resource : resources) {
			if (resource instanceof AppManager) {
				AppManager app = (AppManager)resource;
				jsch.expectSuccessOrFailuare(expect, app.toCommandAdd(), "Enter the username:", serengetiCLIPrompt, true, cliTimeout);
				jsch.expectSuccessOrFailuare(expect, app.getUserName(), "Enter the password:", serengetiCLIPrompt, true, cliTimeout);
				if (app.getAppManagerURL().toLowerCase().startsWith("https://")) {
					jsch.expectSuccessOrFailuare(expect, app.getPassword(),
							"Enter the file path of the ssl certificate:", serengetiCLIPrompt, true, cliTimeout);
					String message = jsch.expectSuccessOrFailuare(expect, app.getCertificate(), serengetiCLIPrompt,
							"Enter the file path of the ssl certificate:", false, cliTimeout);
					if (message.indexOf(app.expectedAddResult()) < 0)
						throw new JSchException(message); 
				} else {
					jsch.expectSuccessOrFailuare(expect, app.getPassword(), app.expectedAddResult() + "\r\n" + serengetiCLIPrompt, serengetiCLIPrompt, true, cliTimeout);
				}
			} else {
				jsch.expectSuccessOrFailuare(expect, resource.toCommandAdd(), resource.expectedAddResult() + "\r\n" + serengetiCLIPrompt, serengetiCLIPrompt, true, cliTimeout);
			}
			addedResources.add(resource);
		}
	}

}
