package com.captain.protocol;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.captain.AtomixCreater;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.election.LeaderElection;
import io.atomix.core.election.Leadership;
import io.atomix.core.value.AtomicValue;
import io.atomix.protocols.backup.MultiPrimaryProtocol;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;

public class TestRaft {

	private static Collection<Node> bootstrapLocations = Arrays.asList(
			Node.builder().withId("node1").withAddress(Address.from("localhost:5001")).build(),
			Node.builder().withId("node2").withAddress(Address.from("localhost:5002")).build(),
			Node.builder().withId("node3").withAddress(Address.from("localhost:5003")).build());

	
	public static Atomix atomix(int id) throws InterruptedException, ExecutionException, TimeoutException{
		Atomix atomix = Atomix.builder()
				.withClusterId("raft")
				.withMemberId(MemberId.from("node" + id))
				.withHost("localhost")
				.withPort((5000 + id))
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
//				.withManagementGroup(RaftPartitionGroup.builder("raft")
//						.withMembers(Arrays.asList("node1", "node2", "node3"))
//						.withNumPartitions(1)
//						.withPartitionSize(1024 * 1024)
//						.withStorageLevel(StorageLevel.DISK)
//						.withDataDirectory(new File("d:/atomix/node" + id)).build())
				.withManagementGroup(PrimaryBackupPartitionGroup.builder("system").build())
				.withPartitionGroups(PrimaryBackupPartitionGroup.builder("data").build())
				.build();
		atomix.start().join();
		return atomix;
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
		int id = 2;
		Atomix atomix = Atomix.builder().withMemberId(MemberId.from("node" + id)).withHost("localhost")
				.withPort(5000 + id).withClusterId("elect")
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.withManagementGroup(PrimaryBackupPartitionGroup.builder("mg-data").build())
				.withPartitionGroups(PrimaryBackupPartitionGroup.builder("data").build()).build();
		atomix.start().join();
		
		System.out.println("-------------" + id);
//		atomix.getMembershipService().addListener(event -> {
//			System.out.println(node + "listener " + event);
//		});
//		AtomicValue v = atomix.<String>atomicValueBuilder("my-value").withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
//		
//		v.set("node" + node);
//		System.out.println("node" + node + v.get());
		atomix.getMembershipService().addListener(event -> {
			System.out.println("node" + id + " " + event.type() + " -> " + event.subject().id());
		});
		LeaderElection<String> election1 = atomix.<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result = election1.run("node" + id);
		
		election1.addListener(event -> {
			System.out.println("node" + id + " leaderId = " + result.leader().id());
		});
		while(true) {
			System.out.println(result.leader().id());
			Thread.sleep(2000);
		}
	}
}
