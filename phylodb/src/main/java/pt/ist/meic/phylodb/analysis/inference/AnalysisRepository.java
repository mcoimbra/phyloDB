package pt.ist.meic.phylodb.analysis.inference;

import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.springframework.stereotype.Repository;
import pt.ist.meic.phylodb.analysis.inference.model.Analysis;
import pt.ist.meic.phylodb.analysis.inference.model.Edge;
import pt.ist.meic.phylodb.analysis.inference.model.InferenceAlgorithm;
import pt.ist.meic.phylodb.typing.profile.model.Profile;
import pt.ist.meic.phylodb.utils.db.AlgorithmsRepository;
import pt.ist.meic.phylodb.utils.db.Query;
import pt.ist.meic.phylodb.utils.service.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class AnalysisRepository extends AlgorithmsRepository<Analysis, Analysis.PrimaryKey> {

	protected AnalysisRepository(Session session) {
		super(session);
	}

	@Override
	protected Result getAll(int page, int limit, Object... filters) {
		if (filters == null || filters.length == 0)
			return null;
		String statement = "MATCH (pj:Project {id: $})-[:CONTAINS]->(ds:Dataset {id: $})\n" +
				"MATCH (ds)-[:CONTAINS]->(p1:Profile)-[d:DISTANCES]->(p2:Profile)\n" +
				"WHERE d.deprecated = false\n" +
				"RETURN pj.id as projectId, ds.id as datasetId, d.id as id, d.deprecated as deprecated, d.algorithm,\n" +
				"collect(DISTINCT {from: p1.id, fromVersion: d.fromVersion, fromDeprecated: p1.deprecated, distance: d.distance,\n" +
				"to: p2.id, toVersion: d.toVersion, toDeprecated: p2.deprecated}) as edges\n" +
				"ORDER BY pj.id, d.id, size(d.id), d.id SKIP $ LIMIT $";
		return query(new Query(statement, filters[0], filters[1], page, limit));
	}

	@Override
	protected Result get(Analysis.PrimaryKey key) {
		String statement = "MATCH (pj:Project {id: $})-[:CONTAINS]->(ds:Dataset {id: $})\n" +
				"MATCH (ds)-[:CONTAINS]->(p1:Profile)-[d:DISTANCES {id: $}]->(p2:Profile)\n" +
				"RETURN pj.id as projectId, ds.id as datasetId, d.id as id, d.deprecated as deprecated, d.algorithm\n" +
				"collect(DISTINCT {from: p1.id, fromVersion: d.fromVersion, fromDeprecated: p1.deprecated, distance: d.distance,\n" +
				"to: p2.id, toVersion: d.toVersion, toDeprecated: p2.deprecated}) as edges\n";
		return query(new Query(statement, key.getProjectId(), key.getDatasetId(), key.getId()));
	}

	@Override
	protected Analysis parse(Map<String, Object> row) {
		List<Edge> list = new ArrayList<>();
		UUID projectId = UUID.fromString(row.get("projectId").toString());
		UUID datasetId = UUID.fromString(row.get("datasetId").toString());
		for (Map<String, Object> edge: (Map<String, Object>[]) row.get("edges")) {
			Entity<Profile.PrimaryKey> from = new Entity<>(new Profile.PrimaryKey(projectId, datasetId, (String) edge.get("from")), (long) edge.get("fromVersion"), (boolean) edge.get("fromDeprecated"));
			Entity<Profile.PrimaryKey> to = new Entity<>(new Profile.PrimaryKey(projectId, datasetId, (String) edge.get("to")), (long) edge.get("toVersion"), (boolean) edge.get("toDeprecated"));
			list.add(new Edge(from, to, (int) edge.get("distance")));
		}
		return new Analysis(projectId,
				datasetId,
				UUID.fromString(row.get("id").toString()),
				InferenceAlgorithm.valueOf(row.get("algorithm").toString()),
				(boolean) row.get("deprecated"),
				list
		);
	}

	@Override
	protected boolean isPresent(Analysis.PrimaryKey key) {
		String statement = "OPTIONAL MATCH (pj:Project {id: $})-[:CONTAINS]->(ds:Dataset {id: $})\n" +
				"OPTIONAL MATCH (ds)-[:CONTAINS]->(p1:Profile)-[d:DISTANCES {id: $}]->(p2:Profile)\n" +
				"WITH d.deprecated as deprecated, collect(d) as ignored\n" +
				"RETURN COALESCE(deprecated = false, false)";
		return query(Boolean.class, new Query(statement, key.getProjectId(), key.getDatasetId(), key.getId()));
	}

	@Override
	protected void store(Analysis analysis) {
		String statement = "MATCH (pj:Project {id: $})-[:CONTAINS]->(d:Dataset {id: $})\n" +
				"WHERE d.deprecated = false\n" +
				"OPTIONAL MATCH (d)-[:CONTAINS]->(p1:Profile)-[d:DISTANCES {id: $}]->(p2:Profile)\n" +
				"WITH d, $ as treeId, COALESCE(d.version, 0) + 1 as v\n" +
				"UNWIND $ as edge\n" +
				"MATCH (d)-[:CONTAINS]->(p1:Profile {id: edge.from})-[r1:CONTAINS_DETAILS]->(:ProfileDetails)\n" +
				"WHERE NOT EXISTS(r1.to)\n" +
				"MATCH (d)-[:CONTAINS]->(p2:Profile {id: edge.to})-[r2:CONTAINS_DETAILS]->(:ProfileDetails)\n" +
				"WHERE NOT EXISTS(r2.to)\n" +
				"CREATE (p1)-[:DISTANCES {id: treeId, version: v, deprecated: false, fromVersion: r1.version, toVersion: r2.version, distance: edge.distance}]";
		Analysis.PrimaryKey key = analysis.getPrimaryKey();
		Query query = new Query(statement, key.getProjectId(), key.getDatasetId(), key.getId(), key.getId(),
				analysis.getEdges().stream()
						.map(e -> new Object() {
							public final String from = e.getFrom().getPrimaryKey().getId();
							public final String to = e.getTo().getPrimaryKey().getId();
							public final int distance = e.getWeight();
						})
		);
		execute(query);
	}

	@Override
	protected void delete(Analysis.PrimaryKey key) {
		String statement = "MATCH (MATCH (pj:Project {id: $})-[:CONTAINS]->(ds:Dataset {id: $})\n" +
				"MATCH (ds)-[:CONTAINS]->(p1:Profile)-[d:DISTANCES {id: $}]->(p2:Profile)\n" +
				"SET d.deprecated = true";
		execute(new Query(statement, key.getProjectId(), key.getDatasetId(), key.getId()));
	}

}
