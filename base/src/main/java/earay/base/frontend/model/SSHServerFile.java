package earay.base.frontend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.hibernate.validator.constraints.NotBlank;

import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@EqualsAndHashCode(callSuper=false)
@Data
public class SSHServerFile extends SSHServerInfo {
	
	@ApiModelProperty(value = "file path on remote ssh server", required = true)
	@NotBlank
	private String file;
	
	public static SSHServerFile from(SSHServerInfo server, String file) {
		SSHServerFile serverFile = new SSHServerFile();
		serverFile.setHostName(server.getHostName());
		serverFile.setPort(server.getPort());
		serverFile.setPassword(server.getPassword());
		serverFile.setUserName(server.getUserName());
		serverFile.setTimeOut(server.getTimeOut());
		serverFile.setFile(file);
		return serverFile;
	}

}
