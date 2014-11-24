package earay.bigdata.bigtop.frontend.resource;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/mapreduce")
@Api(value = "/mapreduce", description = "Operations to trigger mapreduce jobs")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class MapReduceREST {
	
	@Path("/wordcount")
	@GET
	@ApiOperation(value = "/wordcount", notes = "Word count job")
	public Response getCategoryEntries() throws Exception {
		log.info("Run word count job.");
		return null;
	}

}
