package mpicbg.csbd.task;

public interface TaskManager {

	public void initialize();

	public void add( Task task );

	public void cancel();

	public void log( String msg );

	public void logError( String msg );

	public void update( Task task );

	public void close();

}
