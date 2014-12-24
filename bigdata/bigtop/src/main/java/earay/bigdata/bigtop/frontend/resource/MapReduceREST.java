package earay.bigdata.bigtop.frontend.resource;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import earay.base.frontend.model.SSHServerFile;
import earay.base.frontend.model.SSHServerInfo;
import earay.base.frontend.resource.JSchREST;
import earay.base.frontend.resource.JSchREST.JSchResult;

@Path("/mapreduce")
@Api(value = "/mapreduce", description = "Operations to trigger mapreduce jobs")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class MapReduceREST {
	
	public static final String timeStampToken = "currentTimeStamp";
	
	public static final String defaultWordCountTrigger = "shell/runWordCount.sh";
	
	public static final String defaultWordCountInput = "hadoop/wcinput.txt";

	public static final String defaultWordCountValidator = "hadoop/wcoutput.txt";

	private final JSchREST jsch;
	
	@Path("/wordcount")
	@POST
	@ApiOperation(value = "/wordcount", notes = "Word count job", response = MapReduceResult.class)
	@ApiResponses(value = { @ApiResponse(code = 400, response = JSchResult.class, message = "Bad request to trigger wordcount job via ssh"),
			@ApiResponse(code = 500, response = JSchResult.class, message = "Internal server error when kicking off wordcount job via ssh") })
	public Response runWordCount(@ApiParam(required = true) @Valid @NotNull MapReduceRequest req) {
		String scriptFile = req.getScriptFile();
		String resourceFolder = req.getResourceFolder();
		String hadoopClientHost = req.getHadoopClient().getHostName();
		String fileName = scriptFile;
		if (scriptFile.lastIndexOf("/") > 0)
			fileName = scriptFile.substring(scriptFile.lastIndexOf("/") + 1);
		log.info("Deploying " + scriptFile + " to " + resourceFolder + fileName + " on host " + hadoopClientHost); 
		Response response = jsch.deploy(new JSchREST.DeployRequest(scriptFile, SSHServerFile.from(req.getHadoopClient(), resourceFolder + fileName)));
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		String command = "cd " + resourceFolder + "; chmod 755 " + fileName + "; ./" + fileName;
		String wordCountInput = req.getWordCountInput();
		fileName = wordCountInput;
		if (wordCountInput.lastIndexOf("/") > 0)
			fileName = wordCountInput.substring(wordCountInput.lastIndexOf("/") + 1);
		log.info("Deploying " + wordCountInput + " to " + resourceFolder + fileName + " on host " + hadoopClientHost);
		response = jsch.deploy(new JSchREST.DeployRequest(wordCountInput, SSHServerFile.from(req.getHadoopClient(), resourceFolder + fileName)));
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		String wordCountFolder = req.getWordCountFolder();
		command += " " + wordCountFolder + " " + fileName + " " + String.valueOf(req.isPurgeWordCountFolder());
		String wordCountValidator = req.getWordCountValidator();	
		if (wordCountValidator != null) {
			if (wordCountValidator.lastIndexOf("/") > 0)
				fileName = wordCountValidator.substring(wordCountValidator.lastIndexOf("/") + 1);
			log.info("Deploying " + wordCountValidator + " to " + resourceFolder + fileName + " on host " + hadoopClientHost);
			response = jsch.deploy(new JSchREST.DeployRequest(wordCountValidator, SSHServerFile.from(req.getHadoopClient(), resourceFolder + fileName)));
			if (Status.OK.getStatusCode() != response.getStatus()) return response;
		}
		log.info("Executing '" + command + "' to trigger map reduce job on host " + hadoopClientHost);
		response = jsch.execute(new JSchREST.SSHExecRequest(req.getHadoopClient(), command));
		JSchResult jobResult = (JSchResult)response.getEntity();
		log.info("MapReduce job output stream : \n" + jobResult.getOutput());
		log.info("MapReduce job error stream : \n" + jobResult.getError());
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		String remoteResult = resourceFolder + wordCountFolder + ".out";
		if (wordCountValidator == null) {
			command = "cat " + remoteResult;
			log.info("Executing '" + command + "' to get map reduce result on host " + hadoopClientHost);
			response = jsch.execute(new JSchREST.SSHExecRequest(req.getHadoopClient(), command));
			if (Status.OK.getStatusCode() != response.getStatus())
				return Response.ok(MapReduceResult.from(jobResult, "please manually check map reduce result in remote file " + remoteResult)).build();
			else
				return Response.ok(MapReduceResult.from(jobResult, ((JSchResult)response.getEntity()).getOutput())).build();
		} else {
			command = "diff " + remoteResult + " " + resourceFolder + fileName + "; exit 0";
			log.info("Executing '" + command + "' to compare map reduce result on host " + hadoopClientHost);
			response = jsch.execute(new JSchREST.SSHExecRequest(req.getHadoopClient(), command));
			if (Status.OK.getStatusCode() != response.getStatus())
				return response;
			else
				return Response.ok(MapReduceResult.from(jobResult, ((JSchResult)response.getEntity()).getOutput())).build();
		}
	}
	
	@Getter
	@RequiredArgsConstructor
	public static class MapReduceResult {
		
		@ApiModelProperty(value = "message from output stream of mapreduce job")
		private final String output;
		
		@ApiModelProperty(value = "message from error stream of mapreduce job")
		private final String error;
		
		@ApiModelProperty(value = "status for mapreduce operation, 0 for expected behavior")
		private final int status;	
		
		@ApiModelProperty(value = "result of mapreduce job")
		private final String result;
		
		public static MapReduceResult from(JSchResult sch, String result) {
			return new MapReduceResult(sch.getOutput(), sch.getError(), sch.getStatus(), result);
		}
		
	}
	
	@SuppressWarnings("serial")
	@ApiModel("Map reduce job request")
	@Data
	public static class MapReduceRequest implements Serializable {
		
		@ApiModelProperty(value = "remote ssh server access information", required = true)
		@NotNull
		@Valid
		private SSHServerInfo hadoopClient;
		
		@ApiModelProperty(value = "Hdfs folder for word count job, default is wordcount-{" + timeStampToken + "}")
		private String wordCountFolder;
		
		@ApiModelProperty(value = "OS folder for word count job on hadoop client host, default is /tmp/")
		private String resourceFolder;
		
		@ApiModelProperty(value = "Remove hdfs folder for word count after job done")
		private boolean purgeWordCountFolder;
		
		@ApiModelProperty(value = "Shell script file to trigger word count job, "
				+ "either local file or resource file in class path where earay application is, "
				+ "default is class path resource file '" + defaultWordCountTrigger + "'")
		private String scriptFile;
		
		@ApiModelProperty(value = "Input file for word count job, "
				+ "either local file or resource file in class path where earay application is, "
				+ "default is class path resource file '" + defaultWordCountInput + "'")
		private String wordCountInput;
		
		@ApiModelProperty(value = "File to verify output of word count job, "
				+ "either local file or resource file in class path where earay application is. "
				+ "If it's set, the funciton will response with validation result, otherwise, word count job output returns."
				+ "Use '" + defaultWordCountValidator + "' if you want to compare outcome of input '" + defaultWordCountInput + "'")
		private String wordCountValidator;
		
		@JsonIgnore
		@ValidationMethod
		public boolean isValid() {
			wordCountFolder = JSchREST.trimToDefault(wordCountFolder, "wordcount-{" + timeStampToken + "}");
			if (wordCountFolder.indexOf(timeStampToken) >= 0)
				wordCountFolder = wordCountFolder.replaceAll("\\{" + timeStampToken + "\\}", String.valueOf(System.currentTimeMillis()));
			resourceFolder = JSchREST.trimToDefault(resourceFolder, "/tmp/");
			if (!resourceFolder.endsWith("/"))
				resourceFolder += "/";
			scriptFile = JSchREST.trimToDefault(scriptFile, defaultWordCountTrigger);
			wordCountInput = JSchREST.trimToDefault(wordCountInput, defaultWordCountInput);
			wordCountValidator = StringUtils.trimToNull(wordCountValidator);
			return true;
		}
		
	}

}
