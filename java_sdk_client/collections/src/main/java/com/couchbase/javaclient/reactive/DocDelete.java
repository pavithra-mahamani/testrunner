package com.couchbase.javaclient.reactive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.ReactiveCollection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.javaclient.doc.DocSpec;
import com.couchbase.javaclient.doc.Person;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class DocDelete implements Callable<String> {
	private DocSpec ds;
	private Bucket bucket;
	private Collection collection;
	private static int num_docs=0;
	private boolean done=false;

	public DocDelete(DocSpec _ds, Bucket _bucket) {
		ds = _ds;
		bucket = _bucket;
	}

	public DocDelete(DocSpec _ds, Collection _collection) {
		ds = _ds;
		collection = _collection;
	}
	
	@Override
	public String call() throws Exception {
		if(collection != null) {
			deleteCollection(ds,collection);
		}else {
			deleteBucketCollections(ds, bucket);
		}
		done = true;
		return num_docs + " DOCS DELETED!";
	}

	public static void deleteBucketCollections(DocSpec ds, Bucket bucket) {
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
		bucketCollections.parallelStream().forEach(c -> deleteCollection(ds, c));
	}

	public static void deleteCollection(DocSpec ds, Collection collection) {
		ReactiveCollection rcollection = collection.reactive();
		num_docs = (int) (ds.get_num_ops()*((float) ds.get_percent_delete()/100));
		List<String> docsToDeleteList = new ArrayList<>();
		String key = null;
		try {
			for(int id=ds.get_startSeqNum(); id < ds.get_startSeqNum() + num_docs; id++) {
				key = ds.get_prefix() + id + ds.get_suffix();
				GetResult found = collection.get(key);
				docsToDeleteList.add(key);
			}
		}catch(Exception e) {
			System.out.println(key + " not found. Skipping delete");
		}
		Flux<String> docsToDelete = Flux.fromIterable(docsToDeleteList);

		docsToDelete.buffer(1000).subscribe(keys -> deleteBatch(rcollection, keys));

	}

	public static void deleteBatch(ReactiveCollection rcollection, List<String> keys) {
		Flux<String> batchKeys = Flux.fromIterable(keys);
		try {
			batchKeys.publishOn(Schedulers.parallel()).flatMap(key -> rcollection.remove(key))
			// Num retries, first backoff, max backoff
			.retryBackoff(10, Duration.ofMillis(100), Duration.ofMillis(1000))
			// Block until last value, complete or timeout expiry
			.blockLast(Duration.ofMinutes(10));
		}catch(DocumentNotFoundException ex) {
			
		}
	}

	public boolean isDone() {
		return done;
	}
}