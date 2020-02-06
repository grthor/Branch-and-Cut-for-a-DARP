package logic;

/**
 * An instance of this class represents a simple vehicle 
 * that can transport a certain amount of people (capacity)
 * and has maximal allowed driving time (maxTourTime)
 * 
 * @author Thorben Groos (thorben.groos@student.uni-siegen.de)
 *
 */
public class Truck {
	
	/**
	 * Capacity of the vehicle Q.
	 */
	private int capacity;
	/**
	 * Maximum permitted travel time L.
	 */
	private int maxTourTime;
	
	/**
	 * Constructor for a new vehicle.
	 * @param capacity The maximal amount of people Q the vehicle can carry.
	 * @param maxTourTime The maximal time L the vehicle is allowed to drive in one day.
	 */
	public Truck(int capacity, int maxTourTime) {
		super();
		this.capacity = capacity;
		this.maxTourTime = maxTourTime;
	}
	
	/**
	 * Get the amount of people Q the vehicle can transport.</b>
	 * @return The amount of people Q the car can transport.
	 */
	public int getCapacity() {
		return capacity;
	}
	
	/**
	 * Set the amount of people Q the vehicle can transport.
	 * @param capacity The amount of people Q the vehicle can transport.
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	/**
	 * Get the maximal allowed time L the vehicle can drive.
	 * @return The maximal time L the vehicle is allowed to drive.
	 */
	public int getMaxTourTime() {
		return maxTourTime;
	}
	
	/**
	 * Set the maximal time L the vehicle can drive.
	 * @param maxTourTime The maximal drive time L.
	 */
	public void setMaxTourTime(int maxTourTime) {
		this.maxTourTime = maxTourTime;
	}
	

}
