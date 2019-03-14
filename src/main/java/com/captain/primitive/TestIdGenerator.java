package com.captain.primitive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.captain.AtomixCreater;

import io.atomix.core.idgenerator.AtomicIdGenerator;
import io.atomix.core.lock.AtomicLock;
import io.atomix.protocols.backup.MultiPrimaryProtocol;

public class TestIdGenerator {

	@Test
	public void testIdGen() throws Exception {
		AtomicIdGenerator idG = AtomixCreater.atomix().atomicIdGeneratorBuilder("data")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		
		System.out.println(idG.nextId());
	}
	
}
