package pt.ist.meic.phylodb.typing.dataset.model;

import java.util.UUID;

public class Dataset {

	private String id;
	private String description;
	private String taxonId;
	private String schemaId;

	public Dataset() {
	}

	public Dataset(UUID id, String description, String taxonId, String schemaId) {
		this.id = id.toString();
		this.description = description;
		this.taxonId = taxonId;
		this.schemaId = schemaId;
	}


	public UUID getId() {
		return UUID.fromString(id);
	}

	public String getDescription() {
		return description;
	}

	public String getTaxonId() {
		return taxonId;
	}

	public String getSchemaId() {
		return schemaId;
	}

}
