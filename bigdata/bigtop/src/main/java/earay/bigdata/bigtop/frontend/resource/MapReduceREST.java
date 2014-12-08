package earay.bigdata.bigtop.frontend.resource;

import java.io.Serializable;

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

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import earay.base.frontend.resource.JSchREST;
import earay.base.frontend.resource.JSchREST.JSchResult;
import earay.base.frontend.resource.RESTUtil;

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
	public Response runWordCount(
			@ApiParam(value = "remote hadoop client host name", required = true) @QueryParam("hadoopClientHost") String hadoopClientHost,
			@ApiParam(value = "sshd port on remote hadoop client host") @DefaultValue("22") @QueryParam("hadoopClientPort") int hadoopClientPort,
			@ApiParam(value = "user name for hadoop client host", required = true) @QueryParam("hadoopClientUser") String username,
			@ApiParam(value = "password for hadoop client host", required = true) @QueryParam("hadoopClientPassword") String password,
			@ApiParam(value = "hdfs folder for word count job") @DefaultValue("wordcount-{" + timeStampToken + "}") @QueryParam("wordCountFolder") String wordCountFolder,
			@ApiParam(value = "os folder for word count job on client host") @DefaultValue("/tmp/") @QueryParam("resourceFolder") String resourceFolder,
			@ApiParam(value = "remove hdfs folder for word count after job done") @DefaultValue("false") @QueryParam("removeWordCountFolder") boolean purgeWordCountFolder,
			@ApiParam(value = "shell script file to trigger word count job, either file path or class path") @DefaultValue(defaultWordCountTrigger) @QueryParam("scriptFile") String scriptFile,
			@ApiParam(value = "input file for word count job, either file path or class path") @DefaultValue(defaultWordCountInput) @QueryParam("wordCountInput") String wordCountInput,
			@ApiParam(value = "file to verify output of word count job, either file path or class path. if it's set, the funciton will return any difference") 
				@DefaultValue(defaultWordCountValidator) @QueryParam("wordCountValidator") String wordCountValidator) {
		wordCountFolder = RESTUtil.trimToDefault(wordCountFolder, "wordcount-{" + timeStampToken + "}");
		if (wordCountFolder.indexOf(timeStampToken) >= 0)
			wordCountFolder = wordCountFolder.replaceAll("\\{" + timeStampToken + "\\}", String.valueOf(System.currentTimeMillis()));
		resourceFolder = RESTUtil.trimToDefault(resourceFolder, "/tmp/");
		if (!resourceFolder.endsWith("/"))
			resourceFolder += "/";
		scriptFile = RESTUtil.trimToDefault(scriptFile, defaultWordCountTrigger);
		String fileName = scriptFile;
		if (scriptFile.lastIndexOf("/") > 0)
			fileName = scriptFile.substring(scriptFile.lastIndexOf("/") + 1);
		log.info("Deploying " + scriptFile + " to " + resourceFolder + fileName + " on host " + hadoopClientHost);
		Response response = jsch.deploy(hadoopClientHost, hadoopClientPort, username, password, scriptFile, resourceFolder + fileName);
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		String command = "cd " + resourceFolder + "; chmod 755 " + fileName + "; ./" + fileName;
		wordCountInput = RESTUtil.trimToDefault(wordCountInput, defaultWordCountInput);
		fileName = wordCountInput;
		if (wordCountInput.lastIndexOf("/") > 0)
			fileName = wordCountInput.substring(wordCountInput.lastIndexOf("/") + 1);
		log.info("Deploying " + wordCountInput + " to " + resourceFolder + fileName + " on host " + hadoopClientHost);
		response = jsch.deploy(hadoopClientHost, hadoopClientPort, username, password, wordCountInput, resourceFolder + fileName);
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		command += " " + wordCountFolder + " " + fileName + " " + String.valueOf(purgeWordCountFolder);
		wordCountValidator = StringUtils.trimToNull(wordCountValidator);
		if (wordCountValidator != null) {
			if (wordCountValidator.lastIndexOf("/") > 0)
				fileName = wordCountValidator.substring(wordCountValidator.lastIndexOf("/") + 1);
			log.info("Deploying " + wordCountValidator + " to " + resourceFolder + fileName + " on host " + hadoopClientHost);
			response = jsch.deploy(hadoopClientHost, hadoopClientPort, username, password, wordCountValidator, resourceFolder + fileName);
			if (Status.OK.getStatusCode() != response.getStatus()) return response;
		}
		log.info("Executing '" + command + "' to trigger map reduce job on host " + hadoopClientHost);
		response = jsch.execute(hadoopClientHost, hadoopClientPort, username, password, command);
		JSchResult jobResult = (JSchResult)response.getEntity();
		log.info("MapReduce job output stream : \n" + jobResult.getOutput());
		log.info("MapReduce job error stream : \n" + jobResult.getError());
		if (Status.OK.getStatusCode() != response.getStatus()) return response;
		String remoteResult = resourceFolder + wordCountFolder + ".out";
		if (wordCountValidator == null) {
			command = "cat " + remoteResult;
			log.info("Executing '" + command + "' to get map reduce result on host " + hadoopClientHost);
			response = jsch.execute(hadoopClientHost, hadoopClientPort, username, password, command);
			if (Status.OK.getStatusCode() != response.getStatus())
				return Response.ok(MapReduceResult.fromJSchResult(jobResult, "please manually check map reduce result in remote file " + remoteResult)).build();
			else
				return Response.ok(MapReduceResult.fromJSchResult(jobResult, ((JSchResult)response.getEntity()).getOutput())).build();
		} else {
			command = "diff " + remoteResult + " " + resourceFolder + fileName + "; exit 0";
			log.info("Executing '" + command + "' to compare map reduce result on host " + hadoopClientHost);
			response = jsch.execute(hadoopClientHost, hadoopClientPort, username, password, command);
			if (Status.OK.getStatusCode() != response.getStatus())
				return response;
			else
				return Response.ok(MapReduceResult.fromJSchResult(jobResult, ((JSchResult)response.getEntity()).getOutput())).build();
		}
	}
	
	@Path("/wordcount2")
	@POST
	@ApiOperation(value = "/wordcount2", notes = "Word count job", response = MapReduceResult.class)
	@ApiResponses(value = { @ApiResponse(code = 400, response = JSchResult.class, message = "Bad request to trigger wordcount job via ssh"),
			@ApiResponse(code = 500, response = JSchResult.class, message = "Internal server error when kicking off wordcount job via ssh") })
	public Response runWordCount2(@ApiParam(value = "request", required = true) MapReduceRequest req) {
		return null;
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
		
		public static MapReduceResult fromJSchResult(JSchResult sch, String result) {
			return new MapReduceResult(sch.getOutput(), sch.getError(), sch.getStatus(), result);
		}
		
	}
	
	@SuppressWarnings("serial")
	@ApiModel("Map reduce job request")
	@Data
	public static class MapReduceRequest implements Serializable {
		
		@ApiModelProperty(value = "remote hadoop client host name", required = true)
		private String hadoopClientHost;
		
		@ApiModelProperty(value = "sshd port on remote hadoop client host")
		public int hadoopClientPort = 22;
		
		@ApiModelProperty(value = "user name for hadoop client host", required = true)
		private String username;
		
		@ApiModelProperty(value = "user name for hadoop client host", required = true)
		private String password;
		
		@ApiModelProperty(value = "hdfs folder for word count job")
		private String wordCountFolder;
		
		@ApiModelProperty(value = "os folder for word count job on client host")
		private String resourceFolder;
		
		@ApiModelProperty(value = "remove hdfs folder for word count after job done")
		private boolean purgeWordCountFolder;
		
		@ApiModelProperty(value = "shell script file to trigger word count job, either file path or class path")
		private String scriptFile;
		
		@ApiModelProperty(value = "input file for word count job, either file path or class path")
		private String wordCountInput;
		
		@ApiModelProperty(value = "file to verify output of word count job, either file path or class path. if it's set, the funciton will return any difference")
		private String wordCountValidator;
		
	}

}
