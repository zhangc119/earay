package earay.base.frontend.resource;

import org.apache.commons.lang.StringUtils;

public class RESTUtil {
	
	public static String trimToDefault(String text, String defaultValue) {
		text = StringUtils.trimToNull(text);
		if (text == null)
			text = defaultValue;
		return text;
	}

}
