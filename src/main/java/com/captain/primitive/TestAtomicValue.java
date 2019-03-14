package com.captain.primitive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.captain.AtomixCreater;

import io.atomix.core.value.DistributedValue;
import io.atomix.protocols.backup.MultiPrimaryProtocol;
public class TestAtomicValue {

	private static Logger log = LoggerFactory.getLogger(TestAtomicValue.class);
	
	@Test
	public void testV() throws Exception {
		DistributedValue<String> dv1 = AtomixCreater.atomix().<String>valueBuilder("my-value").withProtocol(MultiPrimaryProtocol.builder("data").build())
				.withValueType(String.class).build();
		dv1.set("1");

		assertEquals("1", dv1.get());
		
		DistributedValue<String> dv2 = AtomixCreater.atomix().<String>valueBuilder("my-value").withProtocol(MultiPrimaryProtocol.builder("data").build())
				.withValueType(String.class).build();
		assertEquals("1", dv2.get());
		
		dv1.addListener(event -> {
			log.info("node1 value " + event.newValue());
		});
		
		dv2.addListener(event -> {
			log.info("node2 value " + event.newValue());
		});
		
		dv2.set("2");
		
		assertEquals("2", dv2.get());
		
		Thread.sleep(3000);
	}
}
