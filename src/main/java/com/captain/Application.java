package com.captain;


import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.election.LeaderElection;
import io.atomix.core.profile.Profile;
import io.atomix.protocols.backup.MultiPrimaryProtocol;

/**
 * atomix：
 * https://atomix.io/docs/latest/user-manual/introduction/what-is-atomix/
 */
@RestController
@SpringBootApplication
public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	private static List<Node> bootstrapLocations = Lists.newArrayList();
	
	private static List<Atomix> atomixList = Lists.newArrayList();
	
	private final AtomicInteger id = new AtomicInteger();
	
	private Map<String, LeaderElection<String>> map = Maps.newHashMap();
	
	/**
	 * 启用节点
	 * @return
	 */
	@RequestMapping(value = "/startPoint", method = RequestMethod.GET)
	public String startPoint(@RequestParam("nodeId") String nodeId) {
		int port = 5000 + id.incrementAndGet();
		log.info("start node {} port {}", nodeId, port);
		bootstrapLocations.add(Node.builder().withId(nodeId).withHost("localhost").withPort(port).build());
		
		Atomix atomix = Atomix.builder().withMemberId(MemberId.from(nodeId)).withHost("localhost")
				.withPort(port).withClusterId("elect")
				.withMembershipProvider(BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
				.withProfiles(Profile.dataGrid()).build();
		atomix.start().join();
		
		atomixList.add(atomix);
		return nodeId + " start success!";
	}
	
	/**
	 * 获取集群节点信息
	 * @return
	 */
	@RequestMapping(value = "/getClusterMember", method = RequestMethod.GET)
	public String getClusterMember(){
		StringBuilder builder = new StringBuilder();
		atomixList.stream().map(atomix -> atomix.getMembershipService()).forEach(memberShipService -> {
			builder.append("<p>");
			builder.append(memberShipService.getLocalMember().id());
			builder.append(" --> ");
			builder.append(memberShipService.getMembers());
			builder.append("</p>");
		});
		return builder.toString();
	}
	
	/**
	 * 集群所有节点参与选举
	 * @return
	 */
	@RequestMapping(value = "/joinElection", method = RequestMethod.GET)
	public String joinElection(){
		atomixList.stream().forEach(atomix -> {
			LeaderElection<String> election = atomix.<String>leaderElectionBuilder("my-election").withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
			String nodeId = atomix.getMembershipService().getLocalMember().id().id();
			election.run(nodeId);
			election.addListener(event -> {
				if(event.newLeadership().leader().id().equals(nodeId)){
					log.info(nodeId + " is the new leader!");
				}
			});
			map.put(nodeId, election);
		});
		return "success";
	}
	
	@RequestMapping(value = "/election/iterQuery", method = RequestMethod.GET)
	public String iterQuery(){
		return iterElection();
	}
	
	private String iterElection(){
		StringBuilder builder = new StringBuilder();
		map.forEach((a, b) -> {
			builder.append("<p>");
			builder.append(a);
			builder.append("-> candidates:");
			builder.append(b.getLeadership().candidates());
			builder.append("-> leader:");
			builder.append(b.getLeadership().leader().id());
			builder.append("</p>");
		});
		return builder.toString();
	}
	
	@RequestMapping(value = "/election/withdraw", method = RequestMethod.GET)
	public String withdraw(@RequestParam("nodeId") String nodeId){
		LeaderElection<String> election = map.get(nodeId);
		if(election != null){
			election.withdraw(nodeId);
			return nodeId + " withdraw leadership race success!";
		}
		return nodeId + " withdraw leadership race fail!";
	}
	
	@RequestMapping(value = "/election/anoint", method = RequestMethod.GET)
	public String anoint(@RequestParam("nodeId") String nodeId){
		LeaderElection<String> election = map.get(nodeId);
		if(election != null){
			election.anoint(nodeId);
			return nodeId + " become current leader!";
		}
		return nodeId + " become current leader fail!";
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
