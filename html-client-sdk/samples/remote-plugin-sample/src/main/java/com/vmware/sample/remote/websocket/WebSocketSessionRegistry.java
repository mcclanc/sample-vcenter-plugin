/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.websocket;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.web.socket.WebSocketSession;

public class WebSocketSessionRegistry {

   private final Set<WebSocketSession> sessions = new HashSet<>();
   private final ReadWriteLock lock = new ReentrantReadWriteLock();
   private final Lock readLock = lock.readLock();
   private final Lock writeLock = lock.writeLock();

   public void addSession(WebSocketSession session) {
      writeLock.lock();
      sessions.add(session);
      writeLock.unlock();
   }

   public void removeSession(WebSocketSession session) {
      writeLock.lock();
      sessions.remove(session);
      writeLock.unlock();
   }

   public Set<WebSocketSession> getAllSessions() {
      readLock.lock();
      final Set<WebSocketSession> result = new HashSet<>(sessions);
      readLock.unlock();
      return result;
   }
}
