package com.couchbase.javaclient.reactive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.ReactiveCollection;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.javaclient.doc.DocSpec;
import com.couchbase.javaclient.doc.Person;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DocCreate {
	private DocSpec ds;
	private Bucket bucket;
	private Collection collection;
	private static int num_docs = 0;

	public DocCreate(DocSpec _ds, Bucket _bucket) {
		ds = _ds;
		bucket = _bucket;
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
		bucketCollections.parallelStream().forEach(c -> batchDocs(ds, c));
	}

	public DocCreate(DocSpec _ds, Collection _collection) {
		ds = _ds;
		collection = _collection;
		batchDocs(ds, collection);
	}

	public void batchDocs(DocSpec ds, Collection collection) {
		ReactiveCollection rcollection = collection.reactive();
		num_docs = (int) (ds.get_num_ops() * ((float) ds.get_percent_create() / 100));
		Flux<String> docsToUpsert = Flux.range(ds.get_startSeqNum(), num_docs)
				.map(id -> ds.get_prefix() + id + ds.get_suffix());
		final int chunkSize = (int) Math.ceil(num_docs / Runtime.getRuntime().availableProcessors());
		final AtomicInteger counter = new AtomicInteger();
		Map<Integer, List<String>> batches = new HashMap<>();
		batches = docsToUpsert.toStream().parallel().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize));
		
		ExecutorService service = Executors.newFixedThreadPool(num_docs/chunkSize);
		List<UpsertCallable> futureList = new ArrayList<UpsertCallable>();
		batches.forEach((batch, keys) -> {
			UpsertCallable uCallable = this.new UpsertCallable(rcollection, batch, keys);
			futureList.add(uCallable);
		});
		System.out.println("Start");
		try {
			List<Future<Integer>> futures = service.invokeAll(futureList);
		} catch (Exception err) {
			err.printStackTrace();
		}
		System.out.println("Completed");
		service.shutdown();
	}

	public void upsertBatch(ReactiveCollection rcollection, List<String> keys) {
		long threadId = Thread.currentThread().getId();
		System.out.println("\nI am thread " + threadId + " processing batch starting with " + keys.get(0) + "\n");

		Flux.fromIterable(keys)
				.publishOn(Schedulers.elastic())
				//.delayElements(Duration.ofMillis(5))
				.flatMap(key -> rcollection.upsert(key, new Person().getJsonObject()))
				// Num retries, first backoff, max backoff
				.retryBackoff(10, Duration.ofMillis(100), Duration.ofMillis(1000))
				// Block until last value, complete or timeout expiry
				.blockLast(Duration.ofMinutes(10));
				
		//return this.batch;
	}
	
	class UpsertCallable implements Callable<Integer> {
		ReactiveCollection rcollection;
		Integer batch = 0;
		List<String> keys = new ArrayList<String>();

		public UpsertCallable(ReactiveCollection rcollection, Integer batch, List<String> keys) {
			this.rcollection = rcollection;
			this.batch = batch;
			this.keys = keys;
		}

		public Integer call() {
			long threadId = Thread.currentThread().getId();
			System.out.println("\nI am thread " + threadId + " processing batch starting with " + keys.get(0) + "\n");

			Flux.fromIterable(keys)
					.publishOn(Schedulers.elastic())
					//.delayElements(Duration.ofMillis(5))
					.flatMap(key -> this.rcollection.upsert(key, new Person().getJsonObject()))
					// Num retries, first backoff, max backoff
					.retryBackoff(10, Duration.ofMillis(100), Duration.ofMillis(1000))
					// Block until last value, complete or timeout expiry
					.blockLast(Duration.ofMinutes(10));
					
			return this.batch;
		}
	}
}