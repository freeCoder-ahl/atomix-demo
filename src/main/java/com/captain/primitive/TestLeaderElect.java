package com.captain.primitive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.captain.AtomixCreater;

import io.atomix.core.election.LeaderElection;
import io.atomix.core.election.Leadership;
import io.atomix.protocols.backup.MultiPrimaryProtocol;

/**
 * 分布式原语必须在指定分区组上才能运行，而原语作为状态机的一种，将使用分区组复制协议
 * 
 * @author admin https://atomix.io/docs/latest/user-manual/cluster-management/
 *         partition-groups/
 */
public class TestLeaderElect {
	private static Logger log = LoggerFactory.getLogger(TestLeaderElect.class);
	
	@Test
	public void testElectionMulticast() throws Exception {
		String node1 = "node1";
		LeaderElection<String> election1 = AtomixCreater.atomixMulticast().<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> eleR1 = election1.run(node1);
		assertEquals(node1, eleR1.leader().id());
		assertEquals(1, eleR1.leader().term());
		assertEquals(1, eleR1.candidates().size());
		assertEquals(node1, eleR1.candidates().get(0));

		String node2 = "node2";

		LeaderElection<String> election2 = AtomixCreater.atomixMulticast().<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> eleR2 = election2.run(node2);
		assertEquals(node1, eleR2.leader().id());
		assertEquals(1, eleR2.leader().term());

		assertEquals(2, eleR2.candidates().size());
		assertEquals(node1, eleR2.candidates().get(0));
		assertEquals(node2, eleR2.candidates().get(1));
	}

	@Test
	public void testElectionProfile() throws Exception {
		String node1 = "node1";

		LeaderElection<String> election1 = AtomixCreater.atomixProfilesDataGrid().<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> eleR1 = election1.run(node1);
		assertEquals(node1, eleR1.leader().id());
		assertEquals(1, eleR1.leader().term());
		assertEquals(1, eleR1.candidates().size());
		assertEquals(node1, eleR1.candidates().get(0));

		String node2 = "node2";

		LeaderElection<String> election2 = AtomixCreater.atomixProfilesDataGrid().<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> eleR2 = election2.run(node2);
		assertEquals(node1, eleR2.leader().id());
		assertEquals(1, eleR2.leader().term());

		assertEquals(2, eleR2.candidates().size());
		assertEquals(node1, eleR2.candidates().get(0));
		assertEquals(node2, eleR2.candidates().get(1));
	}

	@Test
	public void testElection() throws Exception {
		String node1 = "node1";
		LeaderElection<String> election1 = AtomixCreater.atomixProfilesDataGrid().<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		String node2 = "node2";
		LeaderElection<String> election2 = AtomixCreater.atomixProfilesDataGrid().<String> leaderElectionBuilder("my-election")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		
		Leadership<String> eleR1 = election1.run(node1);
		election1.addListener(event -> {
			log.info(node1 + " event :" + event.type() + " " + event.subject());
		});
		
		Leadership<String> eleR2 = election2.run(node2);
		election2.addListener(event -> {
			log.info(node2 + " event :" + event.type() + " " + event.subject());
		});
		
		assertEquals(node1, eleR1.leader().id());
		assertEquals(1, eleR1.leader().term());
		assertEquals(1, eleR1.candidates().size());
		assertEquals(node1, eleR1.candidates().get(0));
		
		assertEquals(node1, eleR2.leader().id());
		assertEquals(1, eleR2.leader().term());

		assertEquals(2, eleR2.candidates().size());
		assertEquals(node1, eleR2.candidates().get(0));
		assertEquals(node2, eleR2.candidates().get(1));
		System.out.println(eleR1.candidates());
		System.out.println(eleR2.candidates());
	}

	@Test
	public void testWithdraw() throws Exception {
		LeaderElection<String> election1 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result1 = election1.run("node1");

		LeaderElection<String> election2 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result2 = election2.run("node2");

		assertEquals("node1", result1.leader().id());
		assertEquals("node1", result2.leader().id());

		election1.addListener(event -> {
			assertEquals("node2", result1.leader().id());
		});

		election2.addListener(event -> {
			assertEquals("node2", result2.leader().id());
		});

		election1.withdraw("node1");
	}

	@Test
	public void testAnoint() throws Exception {
		LeaderElection<String> election1 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result1 = election1.run("node1");

		LeaderElection<String> election2 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result2 = election2.run("node2");

		assertEquals("node1", result1.leader().id());
		assertEquals("node1", result2.leader().id());

		election1.addListener(event -> {
			log.info("node1 lis newLeadership " + event.newLeadership());
			assertEquals("node2", result1.leader().id());
		});

		election2.addListener(event -> {
			log.info("node2 lis newLeadership " + event.newLeadership());
			assertEquals("node2", result2.leader().id());
		});

		election1.anoint("node2");
		Thread.sleep(3000);
	}

	@Test
	public void testPromote() throws Exception {
		LeaderElection<String> election1 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result1 = election1.run("node1");

		LeaderElection<String> election2 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result2 = election2.run("node2");

		assertEquals("node1", result1.leader().id());
		assertEquals("node1", result2.leader().id());

		election1.addListener(event -> {
			log.info("node1 lis newLeadership " + event.newLeadership().candidates());
			assertEquals("node2", result1.candidates().get(0));
		});

		election2.addListener(event -> {
			log.info("node2 lis newLeadership " + event.newLeadership().candidates());
			assertEquals("node2", result2.candidates().get(0));
		});

		election1.promote("node2");
		Thread.sleep(3000);
	}

	@Test
	public void testLeaderSessionClose() throws Exception {
		LeaderElection<String> election1 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result1 = election1.run("node1");

		LeaderElection<String> election2 = AtomixCreater.atomix().<String> leaderElectionBuilder("test-elect-withdraw")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Leadership<String> result2 = election2.run("node2");

		assertEquals("node1", result1.leader().id());
		assertEquals("node1", result2.leader().id());

		election2.addListener(event -> {
			log.info("node2 lis newLeadership " + event.newLeadership().candidates());
			assertEquals("node2", result2.leader().id());
		});

		election1.close();
		Thread.sleep(3000);
	}
}
