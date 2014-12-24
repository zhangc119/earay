package earay.base.frontend.resource;

import java.util.Hashtable;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
public class JschConfiguration {
	
	public static final String defaultUploadDir = "/tmp";
	
	@JsonProperty("timeout")
	private int timeOut;
	
	private String uploadDir = defaultUploadDir;
	
	private Hashtable<String, String> config;

}
