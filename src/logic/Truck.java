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
	private int fuelCapacity;
	
	public Truck(int[] capacity, int maxTourTime, int fuelCapacity) {
		super();
		this.capacity = capacity;
		this.maxTourTime = maxTourTime;
		this.fuelCapacity = fuelCapacity;
	}
	
	
	public int getFuelCapacity() {
		return fuelCapacity;
	}
	public void setFuelCapacity(int fuelCapacity) {
		this.fuelCapacity = fuelCapacity;
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
