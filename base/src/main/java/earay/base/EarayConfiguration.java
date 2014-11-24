package earay.base;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.authenticator.LdapConfiguration;

import earay.base.frontend.resource.JschConfiguration;

@Getter
public class EarayConfiguration extends Configuration {
	
	public static enum AuthType {
		None, Basic, Ldap
	}

	public EarayConfiguration() {
	}
	
	@JsonProperty("database")
	private DataSourceFactory database = new DataSourceFactory();
	
	@JsonProperty("ldap")
	private LdapConfiguration ldapConfiguration = new LdapConfiguration();
	
	@JsonProperty("ssh")
	private JschConfiguration jschConfiguration;
	
	@JsonProperty("proxy")
	private ClientProxyConfiguration proxyConfiguration;
	
	private List<EarayProject> projects;

	@Valid
	@NotNull
	@JsonProperty("earay")
	private ApplicationSettings applicationSettings;
	
	@Getter
	public static class ApplicationSettings {
		
		private String contextPath = "/";
		
		private AuthType authType = AuthType.None;
		
	}

}
