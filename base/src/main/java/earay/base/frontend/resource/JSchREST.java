package earay.base.frontend.resource;

import io.dropwizard.validation.ValidationMethod;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.TimeUnit;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import net.sf.expectit.filter.Filters;
import net.sf.expectit.matcher.Matchers;

import org.apache.commons.lang.StringUtils;
import org.h2.util.IOUtils;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import earay.base.ClientProxyConfiguration;
import earay.base.EarayConfiguration;
import earay.base.frontend.model.SSHServerFile;
import earay.base.frontend.model.SSHServerInfo;
import groovy.lang.Binding;

@Path("/file")
@Api(value = "/file", description = "Transfer files via sshv2 or http")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class JSchREST {
	
	private final EarayConfiguration config;
	
	@Path("/exec")
	@POST
	@ApiOperation(value = "Run one command and get the result via ssh", response = JSchResult.class)
	public Response execute(@ApiParam(value = "remote ssh server access information", required = true) @Valid @NotNull SSHExecRequest req) {
		Session session = null;
		Channel channel = null;
		try {
			session = openSSH(req.getServer());
			log.info("successfully ssh on remote host " + req.getServer().getHostName());
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(req.getCommand());
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
			//TODO : need to bind streams as log appenders automatically, and for expectit as well
			log.info(sb.toString());
			log.error(err.toString());
			JSchResult result = new JSchResult(sb.toString(), err.toString(), es);
			if (es == 0)
				return Response.ok(result).build();
			else
				return Response.serverError().entity(result).build();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return Response.serverError().entity(new JSchResult("", ex.getMessage(), -1)).build();
		} finally {
			closeSSH(session, channel, null);
		}
	}
	
	@Path("/scp")
	@POST
	@ApiOperation(value = "Copy files from source to destination via ssh", 
			notes = "Both file and recursive folder copy are supported", 
			response = JSchResult.class)
	public Response scp(@ApiParam(required = true) @Valid @NotNull ScpRequest req) {
		Session session = null;
		Channel channel = null;
		Expect expect = null;
		try {
			session = openSSH(req.getDestination());
			channel = session.openChannel("shell");
			SSHServerFile source = req.getSource();
			String command = "scp -r -P " + source.getPort() + " " + source.getUserName() + "@"
					+ source.getHostName() + ":" + source.getFile() + " " + req.getDestination().getFile() + "; exit";
			expect = setupExpect(channel);
			channel.connect(session.getTimeout());
			Binding binding = new Binding();
			binding.setVariable("expect", expect);
			binding.setVariable("command", command);
			binding.setVariable("timeout", req.getTimeout());
			binding.setVariable("password", source.getPassword());
			GroovyREST.gse.run(req.getGroovyScript(), binding);
			return Response.ok(new JSchResult("scp succeeded", "", 0)).build();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return Response.serverError().entity(new JSchResult("", ex.getMessage(), -1)).build();
		} finally {
			closeSSH(session, channel, expect);
		}
	}
	
	@Path("/deploy")
	@POST
	@ApiOperation(value = "Deploy one resourse file within earay application jar or one local file to remote host via ssh", 
			notes = "<p/>For resource file within application jar, please enter the file location relative to resource class path, e.g. 'groovy/scp.groovy' for parameter 'resource'", 
			response = JSchResult.class)
	public Response deploy(@ApiParam(required = true) @Valid @NotNull DeployRequest req) {
		Session session = null;
		Channel channel = null;
		InputStream fis = null;
		try {
			session = openSSH(req.getDestination());
			String command = "scp -t " + req.getDestination().getFile();
			channel = session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			channel.connect(session.getTimeout());
			JSchResult result = checkAck(in);
			if (result.getStatus() != 0) {
				return Response.serverError().entity(result).build();
			}
			String resource = req.getResource();
			URL fileURL = JSchREST.class.getClassLoader().getResource(resource);
			File localFile = null;
			String fileName = resource;
			if (fileURL == null) {
				localFile = new File(resource);
				if (!localFile.exists())
					return Response.status(Status.BAD_REQUEST).entity(new JSchResult("", resource + " doen not exist", -1)).build();	
			} else {
				fileName = fileURL.getFile();
				localFile = new File(fileName);
			}
			if (localFile.isDirectory())
				return Response.status(Status.BAD_REQUEST).entity(new JSchResult("", resource + " is a folder which is supported here", -1)).build();
			command = "C0644 " + localFile.length() + " ";
			if (fileName.lastIndexOf("/") > 0)
				command += fileName.substring(fileName.lastIndexOf('/') + 1);
			else
				command += fileName;
			command += "\n";
			out.write(command.getBytes()); 
			out.flush();
			result = checkAck(in);
			if (result.getStatus() != 0) {
				return Response.serverError().entity(result).build();
			}
			if (fileURL != null)
				fis = fileURL.openStream();
			else
				fis = new FileInputStream(fileName);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len);
			}
			fis.close();
			fis = null;
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			out.close();
			result = checkAck(in);
			if (result.getStatus() != 0) {
				return Response.serverError().entity(result).build();
			}
			return Response.ok(new JSchResult("deployment done", "", 0)).build();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return Response.serverError().entity(new JSchResult("", ex.getMessage(), -1)).build();
		} finally {
			closeSSH(session, channel, fis);
		}
	}
	
	@Path("/upload")
	@POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)	@ApiImplicitParams(@ApiImplicitParam(dataType = "file", name = "file", paramType = "body"))
	@ApiOperation(value = "Upload a client file to the host where this earay applicaiton exists via http", response = JSchResult.class)
	public Response upload(@FormDataParam("file") InputStream stream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		String filename = fileDetail.getFileName();
		String uploadDir = config.getJschConfiguration() != null ? config.getJschConfiguration().getUploadDir() : JschConfiguration.defaultUploadDir;
		String outputPath = uploadDir + File.separator + filename;
		OutputStream outputStream = null;
		try {
            outputStream = new FileOutputStream(outputPath);
            IOUtils.copy(stream, outputStream);
            return Response.ok(new JSchResult(outputPath, "", 0)).build();
        } catch (IOException ex) {
        		log.error("Failed to upload " + filename, ex);
            return Response.serverError().entity(new JSchResult("", ex.getMessage(), -1)).build();
        } finally {
			if (outputStream != null)
				try {
					outputStream.close();
				} catch (IOException e) {
				}
        }
	}
	
	public Session openSSH(SSHServerInfo server) throws JSchException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(server.getUserName(), server.getHostName(), server.getPort());
		session.setUserInfo(new JschUserInfo(server.getPassword()));
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
		session.setTimeout(Math.max(server.getTimeOut(), session.getTimeout()));
		session.connect(session.getTimeout());
		return session;
	}
	
	public void closeSSH(Session session, Channel channel, Closeable closeable) {
		if (channel != null) channel.disconnect();
		if (session != null) session.disconnect();
		if (closeable != null) try { closeable.close(); } catch (IOException e) { }
	}
	
	public Expect setupExpect(Channel channel) throws IOException {
		if (channel == null)
			return null;
		return new ExpectBuilder()
				.withOutput(channel.getOutputStream())
				.withInputs(channel.getInputStream(), channel.getExtInputStream())
				.withEchoInput(System.out)
				.withInputFilters(Filters.removeColors(), Filters.removeNonPrintable())
				.withExceptionOnFailure().build();
	}
	
	public String expectSuccessOrFailuare(Expect expect, String command,
			String success, String failure, boolean matchSuccessFirst, int timeout) throws Exception {
		if (expect == null || StringUtils.trimToNull(command) == null)
			return "";
		Result result = expect.sendLine(command).withTimeout(timeout, TimeUnit.SECONDS).expect(
				Matchers.anyOf(Matchers.contains(success), Matchers.contains(failure)));
		String message = result.getInput();
		if ((matchSuccessFirst && message.indexOf(success) < 0)
				|| (!matchSuccessFirst && message.indexOf(failure) >= 0))
			throw new JSchException(message);
		return message;
	}
	
	private JSchResult checkAck(InputStream in) throws IOException {
		int b = in.read();
		if (b == 0 || b == -1)
			return new JSchResult("", "", b);
		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1 || b == 2)
				return new JSchResult("", sb.toString(), b);
		}
		return new JSchResult("", "", b);
	}
	
	public static String trimToDefault(String text, String defaultValue) {
		text = StringUtils.trimToNull(text);
		if (text == null)
			text = defaultValue;
		return text;
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
		
		@ApiModelProperty(value = "returned message")
		private final String output;
		
		@ApiModelProperty(value = "any error on the operation via ssh")
		private final String error;
		
		@ApiModelProperty(value = "status for ssh operation, 0 for expected behavior")
		private final int status;		
		
	}
	
	@SuppressWarnings("serial")
	@Data
	@AllArgsConstructor
	public static class SSHExecRequest implements Serializable {
		
		@ApiModelProperty(value = "remote ssh server access information", required = true)
		@NotNull
		@Valid
		private SSHServerInfo server;
		
		@ApiModelProperty(value = "command to run on remote ssh server", required = true)
		@NotBlank
		@Valid
		private String command;
		
	}
	
	@SuppressWarnings("serial")
	@Data
	@AllArgsConstructor
	public static class ScpRequest implements Serializable {
		
		@ApiModelProperty(value = "Source to be copied", required = true)
		@NotNull
		@Valid
		private SSHServerFile source;
		
		@ApiModelProperty(value = "Destination to be copied", required = true)
		@NotNull
		@Valid
		private SSHServerFile destination;
		
		@ApiModelProperty(value = "Timeout in seconds for validating scp process, default is 10")
		private int timeout;
		
		@ApiModelProperty(value = "Groovy script to deal with scp process, default is 'scp.groovy'")
		private String groovyScript;
		
		@JsonIgnore
		@ValidationMethod
		public boolean isValid() {
			groovyScript = JSchREST.trimToDefault(groovyScript, "scp.groovy");
			if (timeout <= 0)
				timeout = 10;
			return true;
		}
		
	}
	
	@SuppressWarnings("serial")
	@Data
	@AllArgsConstructor
	public static class DeployRequest implements Serializable {
		
		@ApiModelProperty(value = "Resource class path within appliation jar, or local file location. Folder is not supported", required = true)
		@NotBlank
		private String resource;
		
		@ApiModelProperty(value = "Source to be copied", required = true)
		@NotNull
		@Valid
		private SSHServerFile destination;
		
	}
	
}
