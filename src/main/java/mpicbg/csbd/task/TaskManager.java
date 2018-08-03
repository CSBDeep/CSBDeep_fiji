
package mpicbg.csbd.task;

import org.scijava.Cancelable;

public interface TaskManager extends Cancelable {

	void initialize();

	void add(Task task);

	void debug(String msg);

	void log(String msg);

	void logError(String msg);

	void finalizeSetup();

	void update(Task task);

	void close();

	void noGPUFound();
}
