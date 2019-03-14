package com.captain.primitive;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.captain.AtomixCreater;

import io.atomix.core.lock.AtomicLock;
import io.atomix.protocols.backup.MultiPrimaryProtocol;
import io.atomix.utils.time.Version;

public class TestAtomicLock {

	@Test
	public void testLock() throws Exception {
		AtomicLock lock = AtomixCreater.atomix().atomicLockBuilder("my-lock")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		
		assertEquals(false, lock.isLocked());
		assertEquals(true, lock.tryLock());
		
		assertEquals(true, lock.isLocked());
		lock.unlock();
		
		assertEquals(false, lock.isLocked());
	}
	
	@Test
	public void testLockClose() throws Exception {
		AtomicLock lock1 = AtomixCreater.atomix().atomicLockBuilder("my-lock")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		
		AtomicLock lock2 = AtomixCreater.atomix().atomicLockBuilder("my-lock")
				.withProtocol(MultiPrimaryProtocol.builder("data").build()).build();
		Version version = lock1.lock();
		
		assertEquals(true, lock1.isLocked());
		assertEquals(true, lock1.isLocked(version));
		
		CompletableFuture<Version> future = lock2.async().lock();
	    lock1.close();
	    future.get(10, TimeUnit.SECONDS);
	}
}
