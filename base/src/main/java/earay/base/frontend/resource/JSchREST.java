package earay.base.frontend.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.filter.Filters;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import earay.base.ClientProxyConfiguration;
import earay.base.EarayConfiguration;
import groovy.lang.Binding;

@Path("/ssh")
@Api(value = "/ssh", description = "Execute commands or transfer files via sshv2")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class JSchREST {
	
	private final EarayConfiguration config;
	
	@Path("/exec")
	@POST
	@ApiOperation(value = "Run one command and get the result", notes = "Tweak yml in case of handling ssh connection issues")
	public Response execute(
			@ApiParam(value = "remote host name", required = true) @QueryParam("host") String hostname,
			@ApiParam(value = "sshd port on remote host") @DefaultValue("22") @QueryParam("port") int port,
			@ApiParam(value = "user name for sshing to remote host", required = true) @QueryParam("user") String username,
			@ApiParam(value = "password for sshing to remote host", required = true) @QueryParam("password") String password,
			@ApiParam(value = "command to execute", required = true) @QueryParam("command") String command) {
		Session session = null;
		Channel channel = null;
		try {
			Preconditions.checkNotNull(StringUtils.trimToNull(command));
			session = openSSH(username, password, hostname, port);
			log.info("successfully ssh on remote host " + hostname);
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			((ChannelExec)channel).setErrStream(err);
			InputStream in = channel.getInputStream();
			channel.connect(session.getTimeout());
			StringBuffer sb = new StringBuffer();
			int es;
			byte[] tmp= new byte[1024];
			while (true) {
		        while (in.available() > 0) {
		          int i = in.read(tmp, 0, 1024);
		          if(i < 0) break;
		          sb.append(new String(tmp, 0, i));
		        }
		        if (channel.isClosed()) {
		          if(in.available() > 0) continue; 
		          es = channel.getExitStatus();
		          break;
		        }
		        try { Thread.sleep(100); } catch ( Exception ee ) { }
			}
			return Response.ok(new JSchResult(sb.toString(), err.toString(), es)).build();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new JSchResult("", ex.getMessage(), -1)).build();
		} finally {
			closeSSH(session, channel, null);
		}
	}
	
	@Path("/scp")
	@POST
	@ApiOperation(value = "Copy files from source to destination", notes = "Tweak yml in case of handling ssh connection issues")
	public Response scp(
			@ApiParam(value = "source host name", required = true) @QueryParam("sourceHost") String sourceHost,
			@ApiParam(value = "sshd port on source host") @DefaultValue("22") @QueryParam("sourcePort") int sourcePort,
			@ApiParam(value = "user name for source host", required = true) @QueryParam("sourceHostUser") String sourceUser,
			@ApiParam(value = "password for source host", required = true) @QueryParam("sourceHostPassword") String sourcePasswd,
			@ApiParam(value = "resource to by copied", required = true) @QueryParam("resource") String resource,
			@ApiParam(value = "destination host name", required = true) @QueryParam("destHost") String destHost,
			@ApiParam(value = "sshd port on destination host") @DefaultValue("22") @QueryParam("destPort") int destPort,
			@ApiParam(value = "user name for destination host", required = true) @QueryParam("destHostUser") String destUser,
			@ApiParam(value = "password for destination host", required = true) @QueryParam("destHostPassword") String destPasswd,
			@ApiParam(value = "destination to by copied", required = true) @QueryParam("destination") String destination,
			@ApiParam(value = "timeout for validating scp process") @DefaultValue("10") @QueryParam("timeoutInSecond") int timeout,
			@ApiParam(value = "groovy script to deal with scp process") @DefaultValue("scp.groovy") @QueryParam("groovyScript") String groovyScript
			) {
		Session session = null;
		Channel channel = null;
		Expect expect = null;
		try {
			Preconditions.checkNotNull(StringUtils.trimToNull(resource));
			Preconditions.checkNotNull(StringUtils.trimToNull(destination));
			Preconditions.checkNotNull(StringUtils.trimToNull(groovyScript));
			sourceHost = StringUtils.trimToNull(sourceHost);
			Preconditions.checkNotNull(sourceHost);
			sourceUser = StringUtils.trimToNull(sourceUser);
			Preconditions.checkNotNull(sourceUser);
			sourcePasswd = StringUtils.trimToEmpty(sourcePasswd);
			Preconditions.checkArgument(timeout > 0, "timeout should be larger than zero");
			Preconditions.checkArgument(sourcePort > 0 && sourcePort < 256, "ssh port should be in range of 1-255");
			session = openSSH(destUser, destPasswd, destHost, destPort);
			channel = session.openChannel("shell");
			String command = "scp -r -P " + sourcePort + " " + sourceUser + "@"
					+ sourceHost + ":" + resource + " " + destination + "; exit";
			expect = new ExpectBuilder()
					.withOutput(channel.getOutputStream())
					.withInputs(channel.getInputStream(), 
							channel.getExtInputStream()).withEchoInput(System.out)
					.withInputFilters(Filters.removeColors(), Filters.removeNonPrintable())
					.withExceptionOnFailure().build();
			channel.connect();
			Binding binding = new Binding();
			binding.setVariable("expect", expect);
			binding.setVariable("command", command);
			binding.setVariable("timeout", timeout);
			binding.setVariable("password", sourcePasswd);
			GroovyREST.gse.run("scp.groovy", binding);
			return Response.ok(new JSchResult("scp succeeded", null, 0)).build();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new JSchResult("", ex.getMessage(), -1)).build();
		} finally {
			closeSSH(session, channel, expect);
		}
	}
	
	public Session openSSH(String username, String password, String hostname, int port) throws JSchException {
		hostname = StringUtils.trimToNull(hostname);
		Preconditions.checkNotNull(hostname);
		username = StringUtils.trimToNull(username);
		Preconditions.checkNotNull(username);
		password = StringUtils.trimToEmpty(password);
		Preconditions.checkArgument(port > 0 && port < 256, "ssh port should be in range of 1-255");
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, hostname, port);
		session.setUserInfo(new JschUserInfo(password));
		JschConfiguration configuration = config.getJschConfiguration();
		ClientProxyConfiguration proxy = config.getProxyConfiguration();
		if (configuration != null) {
			session.setConfig(configuration.getConfig());
			session.setTimeout(Math.max(configuration.getTimeOut(), session.getTimeout()));
		}
		if (proxy != null) {
			String proxyType = StringUtils.trimToNull(proxy.getType());
			if ("http".equalsIgnoreCase(proxyType)) {
				ProxyHTTP httpProxy = new ProxyHTTP(proxy.getHost(), proxy.getPort());
				String proxyUser = StringUtils.trimToNull(proxy.getUser());
				if (proxyUser != null)
					httpProxy.setUserPasswd(proxyUser, StringUtils.trimToEmpty(proxy.getPassword()));
				session.setProxy(httpProxy);
			}
		}
		session.connect();
		return session;
	}
	
	public void closeSSH(Session session, Channel channel, Expect expect) {
		if (channel != null) channel.disconnect();
		if (session != null) session.disconnect();
		if (expect != null) try { expect.close(); } catch (IOException e) { }
	}
	
	@Getter
	@RequiredArgsConstructor
	public static class JschUserInfo implements UserInfo {

		private final String password;

		public boolean promptYesNo(String str) {
			return true;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public void showMessage(String message) {
		}
	}
	
	@Getter
	@RequiredArgsConstructor
	public static class JSchResult {
		
		private final String output;
		
		private final String error;
		
		private final int status;		
		
	}
	
}
