package nachos.threads;

import nachos.machine.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {

    private Map<Integer, Exchange> map = new HashMap<>();
    private Lock mapLock = new Lock();

    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        mapLock.acquire();
        Exchange exchange;
        boolean first;
        if (!map.containsKey(tag)) { // no exchange for this tag exists yet, create one and put thread to sleep
            exchange = new Exchange(tag, value);
            map.put(tag, exchange);
            first = true;

        } else {
            exchange = map.remove(tag);
            first = false;
        }
        mapLock.release();
        if (first) {
            exchange.lock.acquire();
            exchange.condition.sleep();
            value = exchange.val2;
            exchange.lock.release();
        } else {
            exchange.lock.acquire();
            exchange.val2 = value;
            exchange.condition.wake();
            value = exchange.val;
            exchange.lock.release();
        }
	    return value;
    }

    private static class Exchange {
        int syncTag;
        int val;
        int val2;
        KThread thread;
        Lock lock = new Lock();
        Condition2 condition = new Condition2(lock);

        Exchange(int syncTag, int value) {
            this.syncTag = syncTag;
            this.val = value;
            this.thread = KThread.currentThread();
        }
    }

    // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();

        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;

                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");

        t1.fork(); t2.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join();
    }

    // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
        // place calls to your Rendezvous tests that you implement here
        rendezTest1();
    }

}
