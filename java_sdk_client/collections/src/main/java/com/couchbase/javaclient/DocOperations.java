package com.couchbase.javaclient;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.javaclient.doc.DocSpec;
import com.couchbase.javaclient.doc.DocSpecBuilder;
import com.couchbase.javaclient.reactive.DocCreate;
import com.couchbase.javaclient.reactive.DocDelete;
import com.couchbase.javaclient.reactive.DocRetrieve;
import com.couchbase.javaclient.reactive.DocUpdate;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class DocOperations {
	public static void main(String[] args) {
		ArgumentParser parser = ArgumentParsers.newFor("Couchbase Java SDK Client For Collections").build()
				.defaultHelp(true).description("Standalone SDK Client");
		// Connection params
		parser.addArgument("-i", "--cluster").required(true).help("Couchbase cluster address");
		parser.addArgument("-u", "--username").setDefault("Administrator").help("Username of Couchbase user");
		parser.addArgument("-p", "--password").setDefault("password").help("Password of Couchbase user");
		parser.addArgument("-b", "--bucket").setDefault("default").help("Name of existing Couchbase bucket");
		parser.addArgument("-s", "--scope").setDefault("_default").help("Name of existing scope");
		parser.addArgument("-c", "--collection").setDefault("default").help("Name of existing collection");

		// Operation params
		parser.addArgument("-n", "--num_ops").type(Integer.class).setDefault(1000).help("Number of operations");
		parser.addArgument("-pc", "--percent_create").type(Integer.class).setDefault(100)
		.help("Percentage of creates out of num_ops");
		parser.addArgument("-pu", "--percent_update").type(Integer.class).setDefault(10)
		.help("Percentage of updates out of num_ops");
		parser.addArgument("-pd", "--percent_delete").type(Integer.class).setDefault(10)
		.help("Percentage of deletes out of num_ops");
		parser.addArgument("-l", "--load_pattern").choices("uniform", "sparse", "dense").setDefault("uniform")
		.help("uniform: load all collections with same number of docs, "
				+ "sparse: load all collections with random number of docs closer to lower bound, "
				+ "dense: load all collections with random number of docs closer to upper bound");

		// Doc params
		parser.addArgument("-dsn", "--start_seq_num").type(Integer.class).setDefault(1)
		.help("Doc id start sequence number");
		parser.addArgument("-dpx", "--prefix").setDefault("doc_").help("Doc id prefix");
		parser.addArgument("-dsx", "--suffix").setDefault("").help("Doc id suffix");
		parser.addArgument("-dt", "--template").setDefault("Person").help("JSON document template");

		// Output params
		parser.addArgument("-o", "--output").choices("info", "verbose").setDefault("summary")
		.help("Output detail level");

		try {
			Namespace ns = parser.parseArgs(args);
			run(ns);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
	}

	private static void run(Namespace ns) {
		String clusterName = ns.getString("cluster");
		String username = ns.getString("username");
		String password = ns.getString("password");
		String bucketName = ns.getString("bucket");
		String scopeName = ns.getString("scope");
		String collectionName = ns.getString("collection");

		ConnectionFactory connection = new ConnectionFactory(clusterName, username, password, bucketName, scopeName,
				collectionName);
		Cluster cluster = connection.getCluster();
		Bucket bucket = connection.getBucket();
		Collection collection = null;
		collection = connection.getCollection();
		
		DocSpec dSpec = new DocSpecBuilder().numOps(ns.getInt("num_ops")).percentCreate(ns.getInt("percent_create"))
				.percentUpdate(ns.getInt("percent_update")).percentDelete(ns.getInt("percent_delete"))
				.loadPattern(ns.getString("load_pattern")).startSeqNum(ns.getInt("start_seq_num"))
				.prefix(ns.getString("prefix")).suffix(ns.getString("suffix")).template(ns.getString("template"))
				.buildDocSpec();

		DocCreate create = new DocCreate(dSpec, collection);
//		try {
//			create.call();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//		DocCreate create = new DocCreate(dSpec, collection);
//		executor.scheduleWithFixedDelay(create, 0, 1, TimeUnit.SECONDS);
		
//		ForkJoinPool pool = new ForkJoinPool();
//		ForkJoinTask<String> create;
//		ForkJoinTask<String> update;
//		ForkJoinTask<String> delete;
//		ForkJoinTask<String> retrieve;
//		if(!collectionName.equals("default")) {
//			create = ForkJoinTask.adapt(new DocCreate(dSpec, collection));
//			update = ForkJoinTask.adapt(new DocUpdate(dSpec, collection));
//			delete = ForkJoinTask.adapt(new DocDelete(dSpec, collection));
//			retrieve = ForkJoinTask.adapt(new DocRetrieve(dSpec, collection));
//		} else {
//			create = ForkJoinTask.adapt(new DocCreate(dSpec, bucket));
//			update = ForkJoinTask.adapt(new DocUpdate(dSpec, bucket));
//			delete = ForkJoinTask.adapt(new DocDelete(dSpec, bucket));
//			retrieve = ForkJoinTask.adapt(new DocRetrieve(dSpec, bucket));
//		}
//		try {
//			pool.invoke(create);
//			//wait_for_op_complete(pool, create);
//			pool.invoke(update);
//			wait_for_op_complete(pool, update);
//			pool.invoke(delete);
//			wait_for_op_complete(pool, delete);
//			pool.invoke(retrieve);
//			wait_for_op_complete(pool, retrieve);
//			pool.shutdown();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		connection.close();
	}

	private static void wait_for_op_complete(ForkJoinPool pool, ForkJoinTask<String> op) {
		do {
			System.out.printf("******************************************\n");
			System.out.printf("Main: Parallelism: %d\n", pool.getParallelism());
			System.out.printf("Main: Active Threads: %d\n", pool.getActiveThreadCount());
			System.out.printf("Main: Task Count: %d\n", pool.getQueuedTaskCount());
			System.out.printf("Main: Steal Count: %d\n", pool.getStealCount());
			System.out.printf("******************************************\n");
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} while ((!op.isDone()));
		
	}

}