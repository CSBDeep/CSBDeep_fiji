package mpicbg.csbd.task;

public class DefaultTask implements Task {

	private boolean started = false;
	private boolean finished = false;
	private boolean failed = false;
	private TaskManager manager;
	private int steps = 1;
	private int iterations = 1;
	private int currentIteration = 0;
	private int currentStep = 0;
	private String title;

	@Override
	public void setManager( final TaskManager manager ) {
		this.manager = manager;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public boolean isFailed() {
		return failed;
	}

	void updateManager() {
		if ( manager != null ) {
			manager.update( this );
		}
	}

	@Override
	public void setStarted() {
		started = true;
		failed = false;
		updateManager();
	}

	@Override
	public void setIdle() {
		started = false;
		failed = false;
		updateManager();
	}

	@Override
	public void setFailed() {
		failed = true;
		updateManager();
	}

	@Override
	public void setFinished() {
		setFinished( true );
	}

	@Override
	public void setFinished( final boolean finished ) {
		this.finished = finished;
		updateManager();
	}

	@Override
	public boolean hasMultipleSteps() {
		return numSteps() > 1;
	}

	@Override
	public int numSteps() {
		return steps;
	}

	@Override
	public void setNumSteps( final int steps ) {
		this.steps = steps;
	}

	@Override
	public void nextStep() {
		currentStep++;
	}

	@Override
	public boolean hasIterations() {
		return numIterations() > 0;
	}

	@Override
	public int numIterations() {
		return iterations;
	}

	@Override
	public void addIteration() {
		iterations++;
	}

	@Override
	public void startNewIteration() {
		iterations++;
		currentIteration++;
	}

	@Override
	public void setIterations( final int iterations ) {
		this.iterations = iterations;
	}

	@Override
	public void nextIteration() {
		currentIteration++;
		setFinished( false );
	}

	@Override
	public void log( final String msg ) {
		if ( manager != null ) {
			manager.log( msg );
		} else {
			System.out.println( msg );
		}
	}

	@Override
	public void logError( final String msg ) {
		if ( manager != null ) {
			manager.logError( msg );
		} else {
			System.out.println( "ERROR: " + msg );
		}
	}

	@Override
	public int getCurrentIteration() {
		return currentIteration;
	}

	@Override
	public void setCurrentIteration( final int currentIteration ) {
		this.currentIteration = currentIteration;
	}

	@Override
	public int getCurrentStep() {
		return currentStep;
	}

	@Override
	public void setCurrentStep( final int currentStep ) {
		this.currentStep = currentStep;
	}

	@Override
	public void cancel() {

	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle( final String title ) {
		this.title = title;
	}
}
