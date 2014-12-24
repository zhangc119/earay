package earay.base.frontend.model;

import io.dropwizard.validation.ValidationMethod;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@Data
@AllArgsConstructor
public class FilePathOrContent implements Serializable{
	
	@ApiModelProperty(value = "File path on server side if content is empty, trigger /file/upload first for client file")
	private String file;
	
	@ApiModelProperty(value = "Content to be passed")
	private String content;
	
	@JsonIgnore
	@ValidationMethod(message= "Either content or file should not be empty")
	public boolean isValid() {
		content = StringUtils.trimToNull(content);
		if (content == null) {
			file = StringUtils.trimToNull(file);
			if (file == null)
				return false;
		}
		return true;
	}

}
