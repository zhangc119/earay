package earay.base;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Getter;

@Getter
public class ClientProxyConfiguration {
	
	private String type = "http";
	
	@Valid
	@NotNull
	private String host;
	
	private int port;
	
	private String user;
	
	private String password;
	
}
