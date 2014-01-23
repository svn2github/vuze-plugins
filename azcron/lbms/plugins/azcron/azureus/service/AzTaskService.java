package lbms.plugins.azcron.azureus.service;

import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import lbms.plugins.azcron.service.Task;
import lbms.plugins.azcron.service.TaskService;

import org.jdom.Element;

/**
 * @author Damokles
 * 
 */
public class AzTaskService extends TaskService implements Runnable {

	private boolean			running;
	private Thread			runnerThread;

	private boolean			terminate	= false;

	ExecutorService			threadPool	= Executors.newCachedThreadPool(new ThreadFactory() {
											public Thread newThread (Runnable r) {
												Thread t = new Thread(r);
												t.setDaemon(true);
												return t;
											}
										});

	PriorityQueue<AzTask>	scheduler	= new PriorityQueue<AzTask>();

	/**
	 * 
	 */
	public AzTaskService () {
	}

	@Override
	public AzTaskService createFromElement (Element e) {
		AzTaskService ts = new AzTaskService();
		ts.readFromElement(e);
		return ts;
	}

	@Override
	public void readFromElement (Element e) {
		List<Element> elems = e.getChildren("Task");
		tasks.clear();
		for (Element ele : elems) {
			tasks.add(new AzTask(ele));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		running = true;
		terminate = false;

		reschedule();

		long delay = 0;
		boolean empty;

		while (!terminate) {
			try {
				{
					AzTask task = scheduler.peek();
					if (task == null) {
						delay = 600000;
						empty = true;
					} else {
						delay = task.getTimeUntilNextExecution();
						empty = false;
					}
				}
				if (delay > 0) {
					if (!empty) {
						System.out.println("Service: next Execution = "
								+ scheduler.peek().getTaskName() + " in "
								+ delay);
						System.out.println();
					}
					Thread.sleep(delay);
				} else {
					while (delay <= 0) {
						// get Task
						AzTask task = scheduler.poll();

						System.out.println("Service: executing "
								+ task.getTaskName() + " " + new Date());
						// execute Task
						threadPool.submit(task);
						System.out.println("Service: finished execution of "
								+ task.getTaskName());
						// reschedule Task
						task.calculateNextExcecution();
						scheduler.offer(task);
						System.out.println();
						delay = scheduler.peek().getTimeUntilNextExecution();
					}
				}

			} catch (InterruptedException e) {
			}
		}
		running = false;
	}

	public void start () {
		if (!running) {
			running = true;
			runnerThread = new Thread(this);
			runnerThread.setDaemon(true);
			runnerThread.start();
		}
	}

	public void terminate () {
		if (runnerThread != null) {
			runnerThread.interrupt();
		}
		this.terminate = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.plugins.azcron.service.TaskService#addTask(lbms.plugins.azcron.service.Task)
	 */
	@Override
	public void addTask (Task t) {
		if (!(t instanceof AzTask)) {
			t = new AzTask(t.toElement()); // convert the task
		}
		super.addTask(t);
		System.out.println("Task Added: " + t.getTaskName());
		if (running) {
			AzTask task = (AzTask) t;
			task.calculateNextExcecution();
			scheduler.offer(task);

			// if the new Task is at the top reschedule
			if (task.equals(scheduler.peek())) {
				if (runnerThread != null) {
					runnerThread.interrupt();
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lbms.plugins.azcron.service.TaskService#removeTask(lbms.plugins.azcron.service.Task)
	 */
	@Override
	public void removeTask (Task t) {
		super.removeTask(t);
		if (running) {
			if (t instanceof AzTask) {
				AzTask task = (AzTask) t;
				if (task.equals(scheduler.peek())) {
					scheduler.remove(task);
					if (runnerThread != null) {
						runnerThread.interrupt();
					}
				} else {
					scheduler.remove(task);
				}
			}
		}
	}

	public void reschedule () {
		if (running) {
			scheduler.clear();

			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i) instanceof AzTask) {
					AzTask task = (AzTask) tasks.get(i);
					task.calculateNextExcecution();
					scheduler.offer(task);
				}
			}
		}
	}

	/**
	 * Returns the Task which is executed next.
	 * 
	 * @return may return null
	 */
	public AzTask getNextExecutedTask () {
		return scheduler.peek();
	}
}
