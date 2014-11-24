package earay.bigdata.bigtop;

import lombok.Getter;
import earay.base.EarayProject;
import earay.bigdata.bigtop.frontend.resource.MapReduceREST;

@Getter
public class BigtopProject implements EarayProject {
	
	private String resourcePackage = MapReduceREST.class.getPackage().getName();
	
	private String smokeMountPoint;

}
