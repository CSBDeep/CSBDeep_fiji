package mpicbg.csbd.task;

public interface Task {

	public void setManager( TaskManager manager );

	public boolean isStarted();

	public boolean isFinished();

	public boolean isFailed();

	public void setStarted();

	public void setIdle();

	public void setFailed();

	public void setFinished();

	public void setFinished( boolean finished );

	public boolean hasMultipleSteps();

	public int numSteps();

	public void setNumSteps( int steps );

	public void nextStep();

	public boolean hasIterations();

	public int numIterations();

	public void addIteration();

	public void startNewIteration();

	public void setIterations( int iterations );

	public void nextIteration();

	public void debug( String msg );

	public void log( String msg );

	public void logError( String msg );

	public int getCurrentIteration();

	public void setCurrentIteration( int currentIteration );

	public int getCurrentStep();

	public void setCurrentStep( int currentStep );

	public String getTitle();

	public void setTitle( String title );

	public void cancel();
}
