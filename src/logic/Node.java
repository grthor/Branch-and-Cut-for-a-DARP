package logic;

public class Node {
	
	private double xPosition;
	private double yPosition;
	private double beginServiceTime;
	private double endServiceTime;
	private int[] load = new int[4];
	private int serviceDuration;
	
	
	public Node(double xPosition, double yPosition, double beginServiceTime, double endServiceTime, int[] load,
			int serviceDuration) {
		super();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.beginServiceTime = beginServiceTime;
		this.endServiceTime = endServiceTime;
		this.load = load;
		this.serviceDuration = serviceDuration;
	}


	public double getxPosition() {
		return xPosition;
	}


	public void setxPosition(double xPosition) {
		this.xPosition = xPosition;
	}


	public double getyPosition() {
		return yPosition;
	}


	public void setyPosition(double yPosition) {
		this.yPosition = yPosition;
	}


	public double getBeginServiceTime() {
		return beginServiceTime;
	}


	public void setBeginServiceTime(double beginServiceTime) {
		this.beginServiceTime = beginServiceTime;
	}


	public double getEndServiceTime() {
		return endServiceTime;
	}


	public void setEndServiceTime(double endServiceTime) {
		this.endServiceTime = endServiceTime;
	}


	public int[] getLoad() {
		return load;
	}


	public void setLoad(int load[]) {
		this.load = load;
	}


	public int getServiceDuration() {
		return serviceDuration;
	}


	public void setServiceDuration(int serviceDuration) {
		this.serviceDuration = serviceDuration;
	}

	
	
}
