package earay.base.frontend.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSHServerInfo implements Serializable {
	
	@ApiModelProperty(value = "host name of remote ssh server", required = true)
	@NotBlank
	private String hostName;
	
	@ApiModelProperty(value = "port number of remote ssh server", hidden = true)
	@Range(min=1, max=255)
	private int port = 22;
	
	@ApiModelProperty(value = "account to sign on remote ssh server", required = true)
	@NotBlank
	private String userName;
	
	@ApiModelProperty(value = "account password to sign on remote ssh server", required = true)
	private String password;
	
	@ApiModelProperty(value = "timeout in milliseconds when communicating with remote ssh server")
	private int timeOut;
	
}
