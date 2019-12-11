package logic;

public class Truck {
	
	/**
	 * An array with 4 fields.</br>
	 * capacity[0] the trucks capacity to carry full 20 foot containers.</br>
	 * capacity[1] the trucks capacity to carry empty 20 foot containers.</br>
	 * capacity[2] the trucks capacity to carry full 40 foot containers.</br>
	 * capacity[3] the trucks capacity to carry empty 40 foot containers.</br>
	 */
	private int[] capacity = new int[4];
	private int maxTourTime;
	
	
	public Truck(int[] capacity, int maxTourTime) {
		super();
		this.capacity = capacity;
		this.maxTourTime = maxTourTime;
	}
	
	
	public int[] getCapacity() {
		return capacity;
	}
	public void setCapacity(int capacity[]) {
		this.capacity = capacity;
	}
	public int getMaxTourTime() {
		return maxTourTime;
	}
	public void setMaxTourTime(int maxTourTime) {
		this.maxTourTime = maxTourTime;
	}
	
	

}
