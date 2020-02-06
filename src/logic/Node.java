package logic;

/**
 * Instances of this class represent a node in graph
 * with a transportation demand, an earliest time and a latest
 * time for this transport and a time it takes to
 * load the persons in the car.
 * 
 * @author Thorben Groos (thorben.groos@student.uni-siegen.de)
 *
 */
public class Node {
	
	/**
	 * x-Position of the node in a graph.
	 */
	private double xPosition;
	/**
	 * y-Position of the node in a graph.
	 */
	private double yPosition;
	/**
	 * Earliest time to pick up.
	 */
	private double earliestServiceTime;
	/**
	 * Latest time to pick up.
	 */
	private double latestServiceTime;
	/**
	 * Amount of people that want to be transported from this node.
	 */
	private int load;
	/**
	 * Time it takes to load the people in the car.
	 */
	private int serviceDuration;
	
	/**
	 * Constructor for a new node.
	 * @param xPosition X-Position of the node in a two dimensional graph.
	 * @param yPosition Y-Position of the node in a two dimensional graph.
	 * @param earliestServiceTime Time the service can start at this node.
	 * @param latestServiceTime Time the service has to be finished at this node.
	 * @param load Amount of Persons that want to be transported away from this node.
	 * @param serviceDuration Time it takes to put the people in the car.
	 */
	public Node(double xPosition, double yPosition, double earliestServiceTime, double latestServiceTime, int load,
			int serviceDuration) {
		super();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.earliestServiceTime = earliestServiceTime;
		this.latestServiceTime = latestServiceTime;
		this.load = load;
		this.serviceDuration = serviceDuration;
	}

	/**
	 * Get the x-position of the node in a graph.</b>
	 * Used for calculating the distance between two nodes.
	 * @return The x-position of the node in a graph.
	 */
	public double getxPosition() {
		return xPosition;
	}

	/**
	 * Set the x-position of the node in a graph.
	 * @param xPosition The new x-position of this node.
	 */
	public void setxPosition(double xPosition) {
		this.xPosition = xPosition;
	}

	/**
	 * Get the y-position of the node in the graph.</b>
	 * Used for calculating the distance between two nodes.
	 * @return The y-position of the node.
	 */
	public double getyPosition() {
		return yPosition;
	}

	/**
	 * Set the y-position of this node in the graph.
	 * @param yPosition new y-position of the node.
	 */
	public void setyPosition(double yPosition) {
		this.yPosition = yPosition;
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
	 * Get the load q of the node (The amount of persons who want to be transported.</br>
	 * q_i = -q_i+n for all i in n.
	 * @return Load q of the node.
	 */
	public int getLoad() {
		return load;
	}
	
	/**
	 * Set the load q of the node. (How many people want to be transported from this node?)</br>
	 * q_i = -q_i+n for all i in n.
	 * @param Load q of the node.
	 */
	public void setLoad(int load) {
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
