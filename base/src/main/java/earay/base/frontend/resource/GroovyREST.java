package earay.base.frontend.resource;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;

import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path("/groovy")
@Api(value = "/groovy", description = "Execute dynamic groovy scripts")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroovyREST {
	
	private Binding defaultBinding;
	
	private GroovyScriptEngine gse;
	
	private GroovyShell shell;
	
	public GroovyREST(Injector injector) {
		gse = new GroovyScriptEngine(new URL[]{getClass().getClassLoader().getResource("groovy/")});
		defaultBinding = new Binding();
		defaultBinding.setVariable("injector", injector);
		shell = new GroovyShell(defaultBinding);
	}
	
	public Object run(String script, Binding binding) throws Exception {
		return gse.run(script, binding != null ? binding : defaultBinding);
	}
	
	@Path("/shell")
	@POST
	@ApiOperation(value = "Run input groovy shell script", notes = "<p/>For debugging, e.g. 'return injector.getInstance(earay.base.EarayConfiguration.class);'.<p/>"
			+ "If returned object is too large (e.g. 'return injector.getAllBindings();'), swagger UI might not tackle it well, please try underneath /api/groovy/shell call for this case.")
	public Response runShell(
			@ApiParam(value = "groovy script shell", required = true) @QueryParam("script") String script) {
		Preconditions.checkNotNull(script);
		try {
			return Response.ok(new GroovyResult(shell.evaluate(script), null)).build();
		} catch (Throwable e) {
			return Response.status(Status.BAD_REQUEST).entity(handleException(e)).build();
		}
	}
	
	@Path("/file")
	@POST
	@ApiOperation(value = "Run one groovy script file", notes = "Check notes on /groovy/shell")
	public Response runFile(
			@ApiParam(value = "groovy script file path", required = true) @QueryParam("file") String file) {
		file = StringUtils.trimToNull(file);
		Preconditions.checkNotNull(file);
		try {
			return Response.ok(new GroovyResult(run(file, null), null)).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(handleException(e)).build();
		}
	}
	
	private GroovyResult handleException(Throwable ex) {
		log.error("Failed to execute groovy script", ex);
		return new GroovyResult("Failed to execute groovy script", ex);
	}
	
	@Getter
	@RequiredArgsConstructor
	public static class GroovyResult {
		
		private final Object output;
		
		private final Throwable exception;
		
	}

}
