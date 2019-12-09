package logic;

public class Truck {
	
	private int capacity;
	private int maxTourTime;
	
	
	public Truck(int capacity, int maxTourTime) {
		super();
		this.capacity = capacity;
		this.maxTourTime = maxTourTime;
	}
	
	
	public int getCapacity() {
		return capacity;
	}
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	public int getMaxTourTime() {
		return maxTourTime;
	}
	public void setMaxTourTime(int maxTourTime) {
		this.maxTourTime = maxTourTime;
	}
	
	

}
