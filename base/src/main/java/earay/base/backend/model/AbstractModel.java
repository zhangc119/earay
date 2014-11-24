package earay.base.backend.model;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.TableGenerator;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractModel implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "id_generator")
	@TableGenerator(
			name = "id_generator",
			table = "hibernate_sequences",
			pkColumnName = "sequence_name",
			valueColumnName = "sequence_next_value",
			allocationSize = 1000)
	private Long id;
	
}
