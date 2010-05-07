/**
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.util;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.regionserver.HRegionServer;

/**
 * Utility used running a cluster all in the one JVM.
 */
public class JVMClusterUtil {
  private static final Log LOG = LogFactory.getLog(JVMClusterUtil.class);

  /**
   * Datastructure to hold RegionServer Thread and RegionServer instance
   */
  public static class RegionServerThread extends Thread {
    private final HRegionServer regionServer;

    public RegionServerThread(final HRegionServer r, final int index) {
      super(r, "RegionServer:" + index);
      this.regionServer = r;
    }

    /** @return the region server */
    public HRegionServer getRegionServer() {
      return this.regionServer;
    }

    /**
     * Block until the region server has come online, indicating it is ready
     * to be used.
     */
    public void waitForServerOnline() {
      while (!regionServer.isOnline()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // continue waiting
        }
      }
    }
  }

  /**
   * Creates a {@link RegionServerThread}.
   * Call 'start' on the returned thread to make it run.
   * @param c Configuration to use.
   * @param hrsc Class to create.
   * @param index Used distingushing the object returned.
   * @throws IOException
   * @return Region server added.
   */
  public static JVMClusterUtil.RegionServerThread createRegionServerThread(final HBaseConfiguration c,
    final Class<? extends HRegionServer> hrsc, final int index)
  throws IOException {
      HRegionServer server;
      try {
        server = hrsc.getConstructor(HBaseConfiguration.class).newInstance(c);
      } catch (Exception e) {
        IOException ioe = new IOException();
        ioe.initCause(e);
        throw ioe;
      }
      return new JVMClusterUtil.RegionServerThread(server, index);
  }

  /**
   * Start the cluster.
   * @param m
   * @param regionServers
   * @return Address to use contacting master.
   */
  public static String startup(final HMaster m,
      final List<JVMClusterUtil.RegionServerThread> regionservers) {
    if (m != null) m.start();
    if (regionservers != null) {
      for (JVMClusterUtil.RegionServerThread t: regionservers) {
        t.start();
      }
    }
    return m == null? null: m.getMasterAddress().toString();
  }

  /**
   * @param master
   * @param regionservers
   */
  public static void shutdown(final HMaster master,
      final List<RegionServerThread> regionservers) {
    LOG.debug("Shutting down HBase Cluster");
    // Be careful how the hdfs shutdown thread runs in context where more than
    // one regionserver in the mix.
    Thread hdfsClientFinalizer = null;
    for (JVMClusterUtil.RegionServerThread t: regionservers) {
      Thread tt = t.getRegionServer().setHDFSShutdownThreadOnExit(null);
      if (hdfsClientFinalizer == null && tt != null) {
        hdfsClientFinalizer = tt;
      }
    }
    if (master != null) {
      master.shutdown();
    }
    // regionServerThreads can never be null because they are initialized when
    // the class is constructed.
      for(Thread t: regionservers) {
        if (t.isAlive()) {
          try {
            t.join();
          } catch (InterruptedException e) {
            // continue
          }
        }
      }
    if (master != null) {
      while (master.isAlive()) {
        try {
          // The below has been replaced to debug sometime hangs on end of
          // tests.
          // this.master.join():
          Threads.threadDumpingIsAlive(master);
        } catch(InterruptedException e) {
          // continue
        }
      }
    }
    if (hdfsClientFinalizer != null) {
      // Don't run the shutdown thread.  Plays havoc if we try to start a
      // minihbasecluster immediately after this one has gone down (In
      // Filesystem, the shutdown thread is kept in a static and is created
      // on classloading.  Can only run it once).
      // hdfsClientFinalizer.start();
      // Threads.shutdown(hdfsClientFinalizer);
    }
    LOG.info("Shutdown " +
      ((regionservers != null)? master.getName(): "0 masters") +
      " " + regionservers.size() + " region server(s)");
  }
}