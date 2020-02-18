package logic;

/**
 * Instances of this class represent a node in graph
 * with a transportation demand, an earliest time and a latest
 * time for this transport and a time it takes to
 * do the service at this node.
 * 
 * @author Thorben Groos (thorben.groos@student.uni-siegen.de)
 *
 */
public class Node {
	/**
	 * For humans readable name
	 */
	private String name = "";
	
	/**
	 * x-Position of the node in a graph.
	 */
	private double latitude;
	
	/**
	 * y-Position of the node in a graph.
	 */
	private double longitude;
	
	/**
	 * Earliest time to pick up.
	 */
	private double earliestServiceTime;
	
	/**
	 * Latest time to pick up.
	 */
	private double latestServiceTime;
	
	/**
	 * Amount of containers of each resource type that should be transported away from this node.</br> 
	 * load[0] = Full 20" containers</br>
	 * load[1] = Empty 20" containers</br>
	 * load[2] = Full 40" containers</br>
	 * load[3] = Empty 40" container
	 */
	private int[] load = new int[4];
	
	/**
	 * Time that is used to load or unload container or to refuel.
	 */
	private int serviceDuration;
	
	
	/**
	 * Constructor for a new node.
	 * @param xPosition xPosition X-Position of the node in a two dimensional graph.
	 * @param yPosition yPosition Y-Position of the node in a two dimensional graph.
	 * @param earliestServiceTime earliestServiceTime Time the service can start at this node.
	 * @param latestServiceTime latestServiceTime Time the service has to be finished at this node.
	 * @param load How many containers of which type should be transported.</br>
	 * load[0] = Full 20" containers</br>
	 * load[1] = Empty 20" containers</br>
	 * load[2] = Full 40" containers</br>
	 * load[3] = Empty 40" container
	 * @param serviceDuration Time for loading/unloading or refueling.
	 */
	public Node(String name, double latitude, double longtitude, double earliestServiceTime, double latestServiceTime, int[] load,
			int serviceDuration) {
		super();
		this.name = name;
		this.latitude = latitude;
		this.longitude = longtitude;
		this.earliestServiceTime = earliestServiceTime;
		this.latestServiceTime = latestServiceTime;
		this.load = load;
		this.serviceDuration = serviceDuration;
	}


	/**
	 * Getter for the name of the node.
	 * @return The name of the node
	 */
	public String getName() {
		return name;
	}


	/**
	 * Setter for the name of the node.
	 * @param name The new name of the node
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Get the latitude of the node in a graph.</b>
	 * Used for calculating the distance between two nodes.
	 * @return The latitude of the node in a graph.
	 */
	public double getlatitude() {
		return latitude;
	}

	/**
	 * Set the latitude of the node in a graph.
	 * @param latitude The new latitude of this node.
	 */
	public void setxPosition(double latitude) {
		this.latitude = latitude;
	}


	/**
	 * Get the ongtitude of the node in the graph.</b>
	 * Used for calculating the distance between two nodes.
	 * @return The longtitude of the node.
	 */
	public double getlongtitude() {
		return longitude;
	}

	/**
	 * Set the longtitude of this node in the graph.
	 * @param longtitude new longtitude of the node.
	 */
	public void setlongtitude(double longtitude) {
		this.longitude = longtitude;
	}


	/**
	 * Get the time e the service can start at the node.
	 * @return Time e the service can start at the node.
	 */
	public double getEarliestServiceTime() {
		return earliestServiceTime;
	}

	/**
	 * Set the time e the service can start at the node.
	 * @param beginServiceTime Time e the service can start at the node.
	 */
	public void setEarliestServiceTime(double beginServiceTime) {
		this.earliestServiceTime = beginServiceTime;
	}

	/**
	 * Get the time l the service has to be finished at the node.
	 * @return Time l the service has to be finished at the node.
	 */
	public double getLatestServiceTime() {
		return latestServiceTime;
	}

	/**
	 * Set the time l the service has to be finished at the node.
	 * @param endServiceTime Time l the service has to be finished at the node.
	 */
	public void setLatestServiceTime(double endServiceTime) {
		this.latestServiceTime = endServiceTime;
	}

	/**
	 * Get the load q of the node (The amount of containers that should be shipped).
	 * @return Load q of the node.</br>
	 * load[0] = Full 20" containers</br>
	 * load[1] = Empty 20" containers</br>
	 * load[2] = Full 40" containers</br>
	 * load[3] = Empty 40" container
	 */
	public int[] getLoad() {
		return load;
	}

	/**
	 * Set the load q of the node.</br>
	 * load[0] = Full 20" containers</br>
	 * load[1] = Empty 20" containers</br>
	 * load[2] = Full 40" containers</br>
	 * load[3] = Empty 40" container
	 * @param load Load q of the node.
	 */
	public void setLoad(int load[]) {
		this.load = load;
	}


	/**
	 * Get the non-negative service duration d.</br>
	 * For node 0 and 2+n+1: d=0.
	 * @return The service duration d.
	 */
	public int getServiceDuration() {
		return serviceDuration;
	}
	
	/**
	 * Set the non-negative service duration d.</br>
	 * For node 0 and 2+n+1: d=0.
	 * @param serviceDuration Service duration d of the node.
	 */
	public void setServiceDuration(int serviceDuration) {
		this.serviceDuration = serviceDuration;
	}

	
	
}
