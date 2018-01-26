package mpicbg.csbd.util;


public class Task {

	private int numSteps;
	private boolean finished;
	private boolean failed;
	
	public Task() {
		finished = false;
		failed = false;
		numSteps = 0;
	}

	public void setNumSteps(int numSteps) {
		this.numSteps = numSteps;
	}

	public int getNumSteps() {
		return numSteps;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public boolean isFailed() {
		return failed;
	}
	
	public boolean hasSteps() {
		return numSteps > 0;
	}

}
