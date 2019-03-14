package com.captain;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.minlog.Log;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.profile.Profile;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;
import io.atomix.utils.net.Address;

public class AtomixCreater {

	private static Logger log = LoggerFactory.getLogger(AtomixCreater.class);
	
	private static AtomicInteger id = new AtomicInteger();

	private static Collection<Node> bootstrapLocations = Arrays.asList(
			Node.builder().withId("node1").withHost("localhost").withPort(5001).build(),
			Node.builder().withId("node2").withHost("localhost").withPort(5002).build(),
			Node.builder().withId("node3").withHost("localhost").withPort(5003).build());

	/**
	 * 通过广播发现几圈其他节点
	 * @return
	 * @throws Exception
	 */
	public static Atomix atomixMulticast() throws Exception {
		final int num = id.incrementAndGet();
		Atomix atomix = Atomix.builder().withMemberId(MemberId.from("node" + num)).withHost("localhost")
				.withPort(5000 + num).withClusterId("elect").withMulticastEnabled().withProfiles(Profile.dataGrid())
				.build();
		atomix.start().get(10, TimeUnit.SECONDS);
		return atomix;
	}

	/**
	 * 通过Bootstrap方式列出集群节点列表
	 * @return
	 * @throws Exception
	 */
	public static Atomix atomix() throws Exception {
		final int num = id.incrementAndGet();
		log.info("node id :" + num);
		Atomix atomix = Atomix.builder().withMemberId(MemberId.from("node" + num)).withHost("localhost")
				.withPort(5000 + num).withClusterId("elect")
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.withManagementGroup(PrimaryBackupPartitionGroup.builder("mg-data").build())
				.withPartitionGroups(PrimaryBackupPartitionGroup.builder("data").build()).build();
		atomix.start().get(30, TimeUnit.SECONDS);
		return atomix;
	}

	/**
	 * 通过默认提供的模板管理分区组：
	 * Profile.consensus("atomix-1", "atomix-2", "atomix-3"): 创建一个Raft系统管理组和一个名为raft都在最初配置的命名成员上复制的Raft原始组
	 * Profile.dataGrid(): 如果尚不存在组，则创建主备份系统管理组，并创建名为的主备份基元组data
	 * Profile.client(): 不配置任何管理或原始组的占位符配置文件
	 * @return
	 * @throws Exception
	 */
	public static Atomix atomixProfilesDataGrid() throws Exception {
		final int num = id.incrementAndGet();
		Atomix atomix = Atomix.builder().withMemberId(MemberId.from("node" + num)).withHost("localhost")
				.withPort(5000 + num).withClusterId("elect")
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.withProfiles(Profile.dataGrid()).build();
		atomix.start().get(30, TimeUnit.SECONDS);
		return atomix;
	}
}
