package com.liveramp.captain.request_lock.zk_request_lock;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//import com.liveramp.zk_tools.ZKProductionLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestZKRequestLock {
  private static final String REQUEST_TYPE = "CUSTOMER_LINK_REQUEST";
  private static final String PATH = "/zk_production_locks/" + REQUEST_TYPE;

  private TestingCluster cluster;

  @Before
  public void setUp() throws Exception {
    cluster = new TestingCluster(3);
    cluster.start();
  }

  @After
  public void tearDown() throws Exception {
    cluster.close();
    cluster.stop();
  }

  @Test
  public void testLockUnlockMultipleRequestIds() throws Exception {
    CuratorFramework testFramework = makeAndStartFramework();
    ZKRequestLock lock = makeLock(testFramework);

    assertNull("No locks to start", testFramework.checkExists().forPath(PATH));

    lock.acquire("1");
    Set<String> locks1 = Sets.newHashSet(testFramework.getChildren().forPath(PATH));
    assertEquals("Locks for single request", Sets.newHashSet("1"), locks1);

    lock.acquire("2");
    Set<String> locks2 = Sets.newHashSet(testFramework.getChildren().forPath(PATH));
    assertEquals("Locks for multiple requests", Sets.newHashSet("1", "2"), locks2);

    lock.release("1");
    Set<String> locks3 = Sets.newHashSet(testFramework.getChildren().forPath(PATH));
    assertEquals("Single lock after releasing one of two locks", Sets.newHashSet("2"), locks3);

    lock.release("2");
    Set<String> locks4 = Sets.newHashSet(testFramework.getChildren().forPath(PATH));
    assertEquals("No locks after all locks released", Sets.newHashSet(), locks4);
  }

  @Test
  public void testReturnLockedRequestIds() throws Exception {
    CuratorFramework testFramework = makeAndStartFramework();
    ZKRequestLock lock = makeLock(testFramework);

    Set<String> locks0 = lock.getLockedIds();
    assertEquals("Locks for single request", Sets.newHashSet(), locks0);

    lock.acquire("1");
    Set<String> locks1 = lock.getLockedIds();
    assertEquals("Locks for single request", Sets.newHashSet("1"), locks1);

    lock.acquire("2");
    Set<String> locks2 = lock.getLockedIds();
    assertEquals("Locks for multiple requests", Sets.newHashSet("1", "2"), locks2);

    lock.release("1");
    Set<String> locks3 = lock.getLockedIds();
    assertEquals("Single lock after releasing one of two locks", Sets.newHashSet("2"), locks3);

    lock.release("2");
    Set<String> locks4 = lock.getLockedIds();
    assertEquals("No locks after all locks released", Sets.newHashSet(), locks4);
  }

  @Test
  public void testThreadedLockUnlockWithMultipleRequestIds() throws Exception {
    /*
      Mock two requests (1 and 2) and 10 steps that they perform (0 through 9). Create an array for each requests that
      represents its steps.

      Create multiple threads (analogous to multiple captain nodes). Each thread tries to acquire a lock on each
      request, remove the first element from the list (i.e. process it), and append that element to the list of output
      strings.

      Start all threads.

      Remove the lock we created in the beginning. (This initial locking step is necessary to prevent the first
      threads run from completing their task and releasing the acquire before the subsequent thread has run, thereby
      making this test useless).

      Wait until all threads have finished.

      Assert that the output list is a identical to the input list.
   */

    ZKRequestLock initialLock = makeLock(makeAndStartFramework());
    initialLock.acquire("1");
    initialLock.acquire("2");
    List<String> input1 = Lists.newArrayList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    List<String> input2 = Lists.newArrayList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    List<String> expectedOutput = Lists.newArrayList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    List<String> actualOutput1 = Lists.newArrayList();
    List<String> actualOutput2 = Lists.newArrayList();
    List<Thread> threads = Lists.newArrayList();

    for (int i = 0; i < 10; i++) {
      Thread thread = new Thread(() -> {
        ZKRequestLock threadLock = makeLock(makeAndStartFramework());
        while (input1.size() > 0 || input2.size() > 0) {

          try {
            threadLock.acquire("1");
            String firstElement = input1.remove(0);
            Uninterruptibles.sleepUninterruptibly(new Random().nextInt(100), TimeUnit.MILLISECONDS);
            actualOutput1.add(firstElement);
          } catch (IndexOutOfBoundsException e) {
            System.out.println("e = " + e);
          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            try {
              threadLock.release("1");
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          try {
            threadLock.acquire("2");
            String firstElement = input2.remove(0);
            Uninterruptibles.sleepUninterruptibly(new Random().nextInt(100), TimeUnit.MILLISECONDS);
            actualOutput2.add(firstElement);
          } catch (IndexOutOfBoundsException e) {
            System.out.println("e = " + e);
          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            try {
              threadLock.release("2");
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

        }

      });

      threads.add(thread);
    }

    for (Thread thread : threads) {
      thread.start();
    }

    initialLock.release("1");
    initialLock.release("2");

    for (Thread thread : threads) {
      thread.join();
    }

    assertEquals("request1", expectedOutput, actualOutput1);
    assertEquals("request2", expectedOutput, actualOutput2);
  }

  private CuratorFramework makeAndStartFramework() {
    CuratorFramework framework = CuratorFrameworkFactory.newClient(cluster.getConnectString(), new RetryNTimes(10, 100));
    framework.start();
    return framework;
  }

  private ZKRequestLock makeLock(CuratorFramework framework) {
    return ZKRequestLock.getTest(framework, PATH);
  }
}
