package earay.serengeti.cli.frontend.resource;

import io.dropwizard.validation.ValidationMethod;

import java.io.IOException;
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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.expectit.Expect;
import net.sf.expectit.matcher.Matchers;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import earay.base.EarayConfiguration;
import earay.base.EarayProject;
import earay.base.frontend.model.SSHServerInfo;
import earay.base.frontend.resource.JSchREST;
import earay.serengeti.cli.SerengetiCLIProject;
import earay.serengeti.cli.frontend.model.CliCommand;
import earay.serengeti.cli.frontend.model.ClusterCreation;
import earay.serengeti.cli.frontend.model.ResourceAdd;

@Path("/serengeti/cli")
@Api(value = "/serengeti/cli", description = "Service to trigger serengeti command line")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class SerengetiCLIREST {
	
	private final JSchREST jsch;
	
	private final EarayConfiguration config;
	
	private int cliTimeout;
	
	private int longOpsTimeout;
	
	@Inject
	public SerengetiCLIREST(JSchREST jsch, EarayConfiguration config) {
		this.jsch = jsch;
		this.config = config;
		for (EarayProject project : this.config.getProjects()) {
			if (project instanceof SerengetiCLIProject) {
				cliTimeout = ((SerengetiCLIProject)project).getCliTimeout();
				longOpsTimeout = ((SerengetiCLIProject)project).getLongOpsTimeout();
			}
		}
	}
	
	@Path("/resoures")
	@POST
	@ApiOperation(value = "/resoures", notes = "Configurate serengeti resources and execute certain commands")
	public Response resoures (
			@ApiParam(value = "description of serengeti resources to be registered", required = true) @Valid @NotNull SerengetiResourceRequest req) {
		return cliTempate(req.getSeregentiServer(), req.getCliConnection(), req.getResources());
	}
	
	@Path("/createCluster")
	@POST
	@ApiOperation(value = "/createCluster", notes = "Create one cluster")
	public Response createCluster (
			@ApiParam(value = "description of cluster to be created", required = true) @Valid @NotNull ClusterCreationRequest req) {
		return cliTempate(req.getSeregentiServer(), req.getCliConnection(), req.getCluster());
	}
	
	private Response cliTempate(SSHServerInfo seregentiServer, SerengetiCLIConnection cliConnection, CliCommand command) {
		Session session = null;
		Channel channel = null;
		Expect expect = null;
		try {
			session = jsch.openSSH(seregentiServer);
			channel = session.openChannel("shell");
			expect = jsch.setupExpect(channel);
			channel.connect(session.getTimeout());
			String promptKeyWord = seregentiServer.getUserName() + "@";
			expect.send("").expect(Matchers.contains(promptKeyWord));
			jsch.expectSuccessOrFailuare(expect, cliConnection.getCliCommand(), CliCommand.serengetiCLIPrompt, promptKeyWord, true, cliTimeout);
			jsch.expectSuccessOrFailuare(expect, "connect --host " + cliConnection.getHost(), "Enter the username:", CliCommand.serengetiCLIPrompt, true, cliTimeout);
			jsch.expectSuccessOrFailuare(expect, cliConnection.getUserName(), "Enter the password:", CliCommand.serengetiCLIPrompt, true, cliTimeout);
			jsch.expectSuccessOrFailuare(expect, cliConnection.getPassword(), CliCommand.serengetiCLIPrompt, "Enter the password:", false, cliTimeout);
			command.operate(jsch, expect, cliTimeout, longOpsTimeout);
			return Response.ok().build();
		} catch (Exception e) {
			return Response.serverError().entity(e).build();
		} finally {
			command.clean(jsch, expect, cliTimeout, longOpsTimeout);
			try {
				expect.sendLine("exit").expect(Matchers.contains(seregentiServer.getUserName() + "@"));
				expect.sendLine("exit").expect(Matchers.eof());
			} catch (IOException e) {
				log.warn("Error when exiting serengeti server", e);
			}
			jsch.closeSSH(session, channel, expect);
		}
	}
	
	@SuppressWarnings("serial")
	@ApiModel("Serengeti CLI request")
	@Data
	public static class SerengetiResourceRequest implements Serializable {
		
		@ApiModelProperty(value = "ssh access information of serengeti server", required = true)
		@NotNull
		@Valid
		private SSHServerInfo seregentiServer;
		
		@ApiModelProperty(value = "credentials to connect serengeti server in command line", required = true)
		@NotNull
		@Valid
		private SerengetiCLIConnection cliConnection;
		
		@ApiModelProperty(value = "resource to be registered in serengeti server", required = true)
		@Valid
		private ResourceAdd resources;
		
	}
	
	@SuppressWarnings("serial")
	@ApiModel("Serengeti CLI request")
	@Data
	public static class ClusterCreationRequest implements Serializable {
		
		@ApiModelProperty(value = "ssh access information of serengeti server", required = true)
		@NotNull
		@Valid
		private SSHServerInfo seregentiServer;
		
		@ApiModelProperty(value = "credentials to connect serengeti server in command line", required = true)
		@NotNull
		@Valid
		private SerengetiCLIConnection cliConnection;
		
		@ApiModelProperty(value = "resource to be registered in serengeti server", required = true)
		@Valid
		private ClusterCreation cluster;
		
	}
	
	@SuppressWarnings("serial")
	@ApiModel("credentials to connect serengeti server in command line")
	@Data
	public static class SerengetiCLIConnection implements Serializable {
		
		@ApiModelProperty(value = "command to start serengeti cli, default 'serengeti'")
		private String cliCommand;
		
		@ApiModelProperty(value = "host information in serengeti connect command, default 'localhost:8443'")
		private String host;
		
		@ApiModelProperty(value = "account to sign on serengeti server", required = true)
		@NotBlank
		private String userName;
		
		@ApiModelProperty(value = "account password to sign on serengeti server", required = true)
		private String password;
		
		@JsonIgnore
		@ValidationMethod
		public boolean isValid() {
			cliCommand = JSchREST.trimToDefault(cliCommand, "serengeti");
			host = JSchREST.trimToDefault(host, "localhost:8443");
			return true;
		}
		
	}

}
