package logic;

/**
 * An instance of this class represents a simple vehicle 
 * that can transport a certain amount of containers
 * and has maximal allowed driving time and a maximum tank
 * capacity.
 * 
 * @author Thorben Groos (thorben.groos@student.uni-siegen.de)
 *
 */
public class Truck {
	
	/**
	 * Capacity of the truck for each type of resource.</br>
	 * capacity[0] capacity for full 20" containers (maximum 2 containers).</br>
	 * capacity[1] capacity for empty 20" containers (maximum 2 containers).</br>
	 * capacity[2] capacity for full 40" containers (maximum 1 container).</br>
	 * capacity[3] capacity for empty 40" containers (maximum 1 container).</br>
	 */
	private int[] containerCapacity = new int[4];
	
	/**
	 * Maximum tank capacity (like 60 gallons).
	 */
	private int tankCapacity;
	
	/**
	 * Constructor for a new Truck.
	 * @param capacity Amount of containers the truck can carry.</br>
	 * capacity[0] = full 20" containers (maximum 2 containers).</br>
	 * capacity[1] = empty 20" containers (maximum 2 containers).</br>
	 * capacity[2] = full 40" containers (maximum 1 container).</br>
	 * capacity[3] = empty 40" containers (maximum 1 container).</br>
	 * @param maxTourTime Maximal permitted amount of time the truck can drive.
	 * @param fuelCapacity Maximum tank capacity.
	 */
	public Truck(int[] containerCapacity, int tankCapacity) {
		super();
		this.containerCapacity = containerCapacity;
		this.tankCapacity = tankCapacity;
	}
	
	/**
	 * Get the maximal tank capacity.
	 * @return Maximal tank capacity of the truck.
	 */
	public int getTankCapacity() {
		return tankCapacity;
	}
	
	/**
	 * Set the maximal tank capacity.
	 * @param fuelCapacity The new maximal tank capacity.
	 */
	public void setTankCapacity(int fuelCapacity) {
		this.tankCapacity = fuelCapacity;
	}
	
	/**
	 * Maximum amount of containers the truck can carry.
	 * @return Maximum amount of container the truck can carry</br>
	 * capacity[0] = full 20" containers (maximum 2 containers).</br>
	 * capacity[1] = empty 20" containers (maximum 2 containers).</br>
	 * capacity[2] = full 40" containers (maximum 1 container).</br>
	 * capacity[3] = empty 40" containers (maximum 1 container).</br>
	 */
	public int[] getContainerCapacity() {
		return containerCapacity;
	}
	
	/**
	 * Change the amount of containers a truck can carry.
	 * capacity[0] = full 20" containers (maximum 2 containers).</br>
	 * capacity[1] = empty 20" containers (maximum 2 containers).</br>
	 * capacity[2] = full 40" containers (maximum 1 container).</br>
	 * capacity[3] = empty 40" containers (maximum 1 container).</br>
	 * @param containerCapacity New amount of containers the truck can carry.
	 */
	public void setContainerCapacity(int containerCapacity[]) {
		this.containerCapacity = containerCapacity;
	}
	
	

}
