package com.captain.cluster;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.atomix.cluster.AtomixCluster;
import io.atomix.cluster.ClusterMembershipEvent;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.cluster.discovery.MulticastDiscoveryProvider;
import io.atomix.utils.net.Address;

public class TestCluster {

	private static Logger log = LoggerFactory.getLogger(TestCluster.class);

	@Test
	public void testBootstrap() throws Exception {
		Collection<Node> bootstrapLocations = Arrays.asList(
				Node.builder().withId("foo").withAddress(Address.from("localhost:5000")).build(),
				Node.builder().withId("bar").withAddress(Address.from("localhost:5001")).build(),
				Node.builder().withId("baz").withAddress(Address.from("localhost:5002")).build());

		AtomixCluster node1 = AtomixCluster.builder().withMemberId("foo").withHost("localhost").withPort(5000)
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.build();
		
		node1.getMembershipService().addListener(event -> {
			if(event.type() == ClusterMembershipEvent.Type.MEMBER_ADDED && event.subject().id().id() != "foo") {
				log.info("node1 members :" + node1.getMembershipService().getMembers());
			}
		});
		
		node1.start().join();

		assertEquals("foo", node1.getMembershipService().getLocalMember().id().id());

		AtomixCluster node2 = AtomixCluster.builder().withMemberId("bar").withHost("localhost").withPort(5001)
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.build();
		node2.getMembershipService().addListener(event -> {
			if(event.type() == ClusterMembershipEvent.Type.MEMBER_ADDED && event.subject().id().id() != "bar") {
				log.info("node2 members :" + node1.getMembershipService().getMembers());
			}
		});
		node2.start().join();

		assertEquals("bar", node2.getMembershipService().getLocalMember().id().id());

		AtomixCluster node3 = AtomixCluster.builder().withMemberId("baz").withHost("localhost").withPort(5002)
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.build();
		node3.getMembershipService().addListener(event -> {
			if(event.type() == ClusterMembershipEvent.Type.MEMBER_ADDED && event.subject().id().id() != "baz") {
				log.info("node3 members :" + node1.getMembershipService().getMembers());
			}
		});
		node3.start().join();

		assertEquals("baz", node3.getMembershipService().getLocalMember().id().id());

		Thread.sleep(3000);
	}

	@Test
	public void testDiscovery() throws Exception {
		AtomixCluster cluster1 = AtomixCluster.builder().withMemberId("foo").withHost("localhost").withPort(5000)
				.withMulticastEnabled().withMembershipProvider(new MulticastDiscoveryProvider()).build();
		AtomixCluster cluster2 = AtomixCluster.builder().withMemberId("bar").withHost("localhost").withPort(5001)
				.withMulticastEnabled().withMembershipProvider(new MulticastDiscoveryProvider()).build();
		AtomixCluster cluster3 = AtomixCluster.builder().withMemberId("car").withHost("localhost").withPort(5002)
				.withMulticastEnabled().withMembershipProvider(new MulticastDiscoveryProvider()).build();

		List<CompletableFuture<Void>> startFutures = Stream.of(cluster1, cluster2, cluster3).map(AtomixCluster::start)
				.collect(Collectors.toList());
		CompletableFuture.allOf(startFutures.toArray(new CompletableFuture[startFutures.size()])).get(10,
				TimeUnit.SECONDS);

		assertEquals("foo", cluster1.getMembershipService().getLocalMember().id().id());
		assertEquals("bar", cluster2.getMembershipService().getLocalMember().id().id());
		assertEquals("car", cluster3.getMembershipService().getLocalMember().id().id());

		// 注：集群成员通过网络通信收到成员集合
		// assertEquals(3, cluster1.getMembershipService().getMembers().size());
		// assertEquals(3, cluster2.getMembershipService().getMembers().size());
		// assertEquals(3, cluster3.getMembershipService().getMembers().size());
		//
		// assertEquals("bar",
		// cluster1.getMembershipService().getMember(MemberId.from("bar")));
		// assertEquals("car",
		// cluster1.getMembershipService().getMember(MemberId.from("car")));
		// assertEquals("foo",
		// cluster2.getMembershipService().getMember(MemberId.from("foo")));

		List<CompletableFuture<Void>> stopFutures = Stream.of(cluster1, cluster2, cluster3).map(AtomixCluster::stop)
				.collect(Collectors.toList());
		try {
			CompletableFuture.allOf(stopFutures.toArray(new CompletableFuture[stopFutures.size()])).get(10,
					TimeUnit.SECONDS);
		} catch (Exception e) {
			// Do nothing
		}
	}

}
