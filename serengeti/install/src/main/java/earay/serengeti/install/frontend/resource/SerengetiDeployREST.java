package earay.serengeti.install.frontend.resource;

import io.dropwizard.validation.ValidationMethod;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import earay.base.frontend.model.SSHServerFile;
import earay.base.frontend.model.SSHServerInfo;
import earay.base.frontend.resource.JSchREST;
import earay.base.frontend.resource.JSchREST.JSchResult;

@Path("/serengeti/install")
@Api(value = "/serengeti/install", description = "Service to deploy serengeti on vCenter")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class SerengetiDeployREST {
	
	public static final String postInstaller = "shell/postInstall.sh";
	
	private final JSchREST jsch;
	
	@Path("/postInstall")
	@POST
	@ApiOperation(value = "/postInstall", notes = "Post installation steps after serengeti is deployed on vCenter", response = JSchResult.class)
	public Response postInstall(@ApiParam(required = true) @Valid @NotNull RunScriptFileRequest req) {
		String scriptFile = req.getScriptFile();
		String resourceFolder = req.getResourceFolder();
		String fileName = scriptFile;
		if (scriptFile.lastIndexOf("/") > 0)
			fileName = scriptFile.substring(scriptFile.lastIndexOf("/") + 1);
		log.info("Deploying " + scriptFile + " to " + resourceFolder + fileName + " on host " + req.getSerengetiServer().getHostName()); 
		Response response = jsch.deploy(new JSchREST.DeployRequest(scriptFile, SSHServerFile.from(req.getSerengetiServer(), resourceFolder + fileName)));
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		String command = "cd " + resourceFolder + "; chmod 755 " + fileName + "; ./" + fileName;
		log.info("Executing '" + command + "' to do post setup steps on host " + req.getSerengetiServer().getHostName());
		return jsch.execute(new JSchREST.SSHExecRequest(req.getSerengetiServer(), command));
	}
	
	@SuppressWarnings("serial")
	@ApiModel("Map reduce job request")
	@Data
	public static class RunScriptFileRequest implements Serializable {
		
		@ApiModelProperty(value = "Serengeti server ssh access information", required = true)
		@NotNull
		@Valid
		private SSHServerInfo serengetiServer;
		
		@ApiModelProperty(value = "OS folder where the script file is deployed on serengeti server, default is /tmp/")
		private String resourceFolder;
		
		@ApiModelProperty(value = "Script file in resource class path or local file path, default is '" + postInstaller + "'")
		private String scriptFile;
		
		@JsonIgnore
		@ValidationMethod
		public boolean isValid() {
			scriptFile = JSchREST.trimToDefault(scriptFile, postInstaller);
			resourceFolder = JSchREST.trimToDefault(resourceFolder, "/tmp/");
			if (!resourceFolder.endsWith("/"))
				resourceFolder += "/";
			return true;
		}
		
	}

}
