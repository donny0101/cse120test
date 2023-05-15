package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

    // thread id -> wake time map
    private final Map<Integer, ThreadSleep> sleepingThreads = new HashMap<>();

    private static class ThreadSleep {
        long wakeTime;
        KThread thread;

        public ThreadSleep(long wakeTime, KThread thread) {
            this.wakeTime = wakeTime;
            this.thread = thread;
        }


    }

	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
        long currTime = Machine.timer().getTime();
        ArrayList<Integer> removeList = new ArrayList<Integer>();
        sleepingThreads.forEach((id, sleep) -> {
            if (currTime >= sleep.wakeTime) {
                removeList.add(id);
            }
        });
        boolean oldState = Machine.interrupt().disable();
        for (Integer id : removeList) {
            ThreadSleep sleep = sleepingThreads.get(id);
            sleepingThreads.remove(id);
            sleep.thread.ready();
        }
        Machine.interrupt().setStatus(oldState);
        KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 *
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 *
	 * @param x the minimum number of clock ticks to wait.
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
        if (x <= 0) {
            return;
        }
		// for now, cheat just to get something working (busy waiting is bad)
        KThread currThread = KThread.currentThread();
		long wakeTime = Machine.timer().getTime() + x;

        boolean oldState = Machine.interrupt().disable();
        sleepingThreads.put(currThread.id, new ThreadSleep(wakeTime, currThread));
        KThread.sleep();
        Machine.interrupt().restore(oldState);
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
    public boolean cancel(KThread thread) {
		int id = thread.id;
        ThreadSleep sleep = sleepingThreads.get(id);
        boolean oldState = Machine.interrupt().disable();
        if (sleep != null) {
            sleepingThreads.remove(id);
            sleep.thread.ready();
        }
        Machine.interrupt().setStatus(oldState);
        return sleep != null;
	}

    // Add Alarm testing code to the Alarm class

    public static void alarmTest1() {
        int durations[] = {1000, 10*1000, 100*1000};
        long t0, t1;

        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil (d);
            t1 = Machine.timer().getTime();
            System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
        }
    }

    // Implement more test methods here ...

    // Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
    public static void selfTest() {
        System.out.println("Alarm test");
        alarmTest1();

        // Invoke your other test methods here ...
    }
}
