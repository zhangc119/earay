package earay.base.frontend.resource;

import java.util.Hashtable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class JschConfiguration {
	
	@JsonProperty("timeout")
	private int timeOut = 10000;
	
	private Hashtable<String, String> config;

}
