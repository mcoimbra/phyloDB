package pt.ist.meic.phylodb.utils.db;

import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public abstract class EntityRepository<E, K> extends Repository<E, K> {

	public static final long CURRENT_VERSION_VALUE = -1;
	public static final String CURRENT_VERSION = "" + CURRENT_VERSION_VALUE;

	protected EntityRepository(Session session) {
		super(session);
	}

	protected abstract Result get(K key, long version);

	public Optional<E> find(K key, long version) {
		if (key == null) return Optional.empty();
		Result result = get(key, version);
		if (result == null) return Optional.empty();
		Iterator<Map<String, Object>> it = result.iterator();
		return !it.hasNext() ? Optional.empty() : Optional.of(parse(it.next()));
	}

}
