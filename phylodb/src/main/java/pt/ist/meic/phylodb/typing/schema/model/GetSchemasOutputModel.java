package pt.ist.meic.phylodb.typing.schema.model;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pt.ist.meic.phylodb.mediatype.Json;
import pt.ist.meic.phylodb.mediatype.Output;

import java.util.List;
import java.util.stream.Collectors;

public class GetSchemasOutputModel implements Json, Output<Json> {

	private List<SimpleSchemaModel> schemas;

	public GetSchemasOutputModel(List<Schema> schemas) {
		this.schemas = schemas.stream()
				.map(SimpleSchemaModel::new)
				.collect(Collectors.toList());
	}

	public List<SimpleSchemaModel> getSchemas() {
		return schemas;
	}

	@Override
	public ResponseEntity<Json> toResponse() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(this);
	}

	private static class SimpleSchemaModel {

		private String id;

		public SimpleSchemaModel(Schema schema) {
			this.id = schema.getId();
		}

		public String getId() {
			return id;
		}

	}

}
