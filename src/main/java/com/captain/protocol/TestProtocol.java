package com.captain.protocol;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.idgenerator.AtomicIdGenerator;
import io.atomix.core.profile.Profile;
import io.atomix.protocols.backup.MultiPrimaryProtocol;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;

public class TestProtocol {
	
	private static Logger log = LoggerFactory.getLogger(TestProtocol.class);
	
	private static Collection<Node> bootstrapLocations = Arrays.asList(
			Node.builder().withId("node1").withAddress(Address.from("localhost:5001")).build(),
			Node.builder().withId("node2").withAddress(Address.from("localhost:5002")).build(),
			Node.builder().withId("node3").withAddress(Address.from("localhost:5003")).build());

	private AtomicInteger id = new AtomicInteger();
	
	public Atomix atomix(int id) throws InterruptedException, ExecutionException, TimeoutException{
		Atomix atomix = Atomix.builder().withMemberId(MemberId.from("node" + id))
				.withHost("localhost")
				.withPort((5000 + id))
				.withClusterId("raft")
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.withManagementGroup(RaftPartitionGroup.builder("raft")
						.withMembers(Arrays.asList("node1", "node2", "node3"))
						.withNumPartitions(1)
						.withPartitionSize(1024 * 1024)
						.withStorageLevel(StorageLevel.DISK)
						.withDataDirectory(new File("d:/atomix/node" + id)).build())
				.withPartitionGroups(PrimaryBackupPartitionGroup.builder("data")
						.withNumPartitions(2)
						.build())
				.build();
		atomix.start().join();
		return atomix;
	}
	
	@Test
	public void testManagerByRaftNode1() throws InterruptedException, ExecutionException, TimeoutException {
		
		Atomix atomix1 = atomix(1);
		
		atomix1.getMembershipService().addListener(event -> {
			log.info("node1 lis " + event);
		});
		
		Thread.sleep(Integer.MAX_VALUE);
	}
	
	@Test
	public void testManagerByRaftNode2() throws InterruptedException, ExecutionException, TimeoutException {
		
		Atomix atomix1 = atomix(2);
		
		atomix1.getMembershipService().addListener(event -> {
			log.info("node2 lis " + event);
		});
		
		Thread.sleep(Integer.MAX_VALUE);
	}
	
	@Test
	public void testManagerByRaftNode3() throws InterruptedException, ExecutionException, TimeoutException {
		
		Atomix atomix1 = atomix(3);
		
		atomix1.getMembershipService().addListener(event -> {
			log.info("node3 lis " + event);
		});
		
		Thread.sleep(Integer.MAX_VALUE);
	}

	@Test
	public void testDefaultProtocol() {
		System.out.println(MultiPrimaryProtocol.builder("data").build().group());
		System.out.println(MultiRaftProtocol.builder("raft").build().group());
	}
}
