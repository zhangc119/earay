package earay.base.frontend.resource;

import earay.base.frontend.model.FilePathOrContent;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;

import java.net.URL;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.inject.Injector;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path("/groovy")
@Api(value = "/groovy", description = "Execute dynamic groovy scripts")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroovyREST {
	
	public static GroovyScriptEngine gse = new GroovyScriptEngine(
			new URL[] { GroovyREST.class.getClassLoader().getResource("groovy/") });

	private Binding defaultBinding;
	
	private GroovyShell shell;
	
	public GroovyREST(Injector injector) {
		defaultBinding = new Binding();
		defaultBinding.setVariable("injector", injector);
		shell = new GroovyShell(defaultBinding);
	}
	
	@Path("/run")
	@POST
	@ApiOperation(value = "Run input groovy shell script", notes = "<p/>For debugging, e.g. 'return injector.getInstance(earay.base.EarayConfiguration.class);'.<p/>"
			+ "If returned object is too large (e.g. 'return injector.getAllBindings();'), swagger UI might not tackle it well, please try /api/groovy/shell for this case.",
			response = GroovyResult.class)
	public Response run(@ApiParam(required = true) @Valid @NotNull FilePathOrContent script) {
		try {
			String content = script.getContent();
			if (content != null)
				return Response.ok(new GroovyResult(shell.evaluate(content), null)).build();
			else {
				return Response.ok(new GroovyResult(gse.run(script.getFile(), defaultBinding), null)).build();
			}
		} catch (Throwable e) {
			return Response.status(Status.BAD_REQUEST).entity(handleException(e)).build();
		}
	}
	
	public static Object runShell(String script, Binding binding) {
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(script);
	}
	
	private GroovyResult handleException(Throwable ex) {
		log.error("Failed to execute groovy script", ex);
		return new GroovyResult("Failed to execute groovy script", ex);
	}
	
	@Getter
	@RequiredArgsConstructor
	public static class GroovyResult {
		
		@ApiModelProperty(value = "returned object from groovy script")
		private final Object output;
		
		@ApiModelProperty(value = "any exception in evaluating groovy script")
		private final Throwable exception;
		
	}

}
