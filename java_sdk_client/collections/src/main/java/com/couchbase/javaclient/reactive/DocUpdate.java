package com.couchbase.javaclient.reactive;

import static com.couchbase.client.java.kv.ReplaceOptions.replaceOptions;
import static com.couchbase.client.java.kv.InsertOptions.insertOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.ReactiveCollection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.javaclient.doc.DocSpec;
import com.couchbase.javaclient.doc.Person;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class DocUpdate implements Callable<String> {
	private DocSpec ds;
	private Bucket bucket;
	private Collection collection;
	private static int num_docs = 0;
	private boolean done = false;

	public DocUpdate(DocSpec _ds, Bucket _bucket) {
		ds = _ds;
		bucket = _bucket;
	}

	public DocUpdate(DocSpec _ds, Collection _collection) {
		ds = _ds;
		collection = _collection;
	}

	@Override
	public String call() throws Exception {
		if (collection != null) {
			updateCollection(ds, collection);
		} else {
			updateBucketCollections(ds, bucket);
		}
		done = true;
		return num_docs + " DOCS UPDATED!";
	}

	public static void updateBucketCollections(DocSpec ds, Bucket bucket) {
		List<Collection> bucketCollections = new ArrayList<>();
		List<ScopeSpec> bucketScopes = bucket.collections().getAllScopes();
		for (ScopeSpec scope : bucketScopes) {
			for (CollectionSpec scopeCollection : scope.collections()) {
				Collection collection = bucket.scope(scope.name()).collection(scopeCollection.name());
				if (collection != null) {
					bucketCollections.add(collection);
				}
			}
		}
		bucketCollections.parallelStream().forEach(c -> updateCollection(ds, c));
	}

	public static void updateCollection(DocSpec ds, Collection collection) {
		ReactiveCollection rcollection = collection.reactive();
		num_docs = (int) (ds.get_num_ops() * ((float) ds.get_percent_update() / 100));
		Flux<String> docsToUpdate = Flux.range(ds.get_startSeqNum(), num_docs)
				.map(id -> ds.get_prefix() + id + ds.get_suffix());

		docsToUpdate.buffer(100).subscribe(keys -> updateBatch(collection, keys));

	}

	public static void updateBatch(Collection collection, List<String> keys) {
//		Flux<String> batchKeys = Flux.fromIterable(keys);
//		List<MutateInSpec> addressSpec = new ArrayList<MutateInSpec>();
//		addressSpec.add("streetAdress");
//		
//		
//		batchKeys.publishOn(Schedulers.parallel()).flatMap(key -> 
//		rcollection.mutateIn(key, addressSpec))
//				// Num retries, first backoff, max backoff
//				.retryBackoff(10, Duration.ofMillis(10), Duration.ofMillis(100))
//				// Block until last value, complete or timeout expiry
//				.blockLast(Duration.ofMinutes(10));
		
		keys.forEach(key -> replace(collection, key));

	}

	public boolean isDone() {
		return done;
	}

	public static void replace(Collection collection, String key) {
		try {
			GetResult found = collection.get(key);
			JsonObject content = found.contentAsObject();
			content.put("modified", true).put("initial", false);
			for (int attempt = 0; attempt < 10; attempt++) {
				try {
					if (found != null) {
						collection.replace(key, content, replaceOptions().cas(found.cas()));
					}
					break; // if successful, break out of the retry loop
				} catch (CasMismatchException ex) {
					// don't do anything, we'll retry the loop
				}
			}
		} catch (Exception e) {

		}
	}
}