package logic;

import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;
import ilog.concert.*;

/**
 * This class can be started (has a main method) and solves
 * the B&C DARP model that is defined in the main() method.
 * 
 * @author Thorben Groos (thorben.groos@student.uni-siegen.de)
 * 
 */
public class model {

	/**
	 * The Cplex model.
	 */
	private static IloCplex cplex;
	/**
	 * The decision variable.
	 */
	private static IloNumVar[][][] x;
	/**
	 * Number of users (number of pick-up locations)
	 */
	private static int n;
	/**
	 * Array containing all nodes.
	 */
	private static Node[] N;
	/**
	 * Array containing all vehicles.
	 */
	private static Truck[] K;
	/**
	 * Time which vehicle k starts its service at node i.
	 */
	private static IloNumVar[][] B;
	/**
	 * Load of vehicle k after visiting node i.
	 */
	private static IloNumVar[][] Q;
	/**
	 * Ride time of user i on vehicle k.
	 */
	private static IloNumVar[][] L;
	/**
	 * Distance between node i and node j.
	 */
	private static double[][] c;
	/**
	 * The travel time between node i and node j.
	 */
	private static double[][] t;


	public static void main(String[] args) {

		// Use a predefined set of nodes (definitely solvable)
		setDefaultNodes();
		// Or generate a random set of nodes (maybe not solvable)
		// If you use a large number of nodes you have to raise
		// the number of vehicles.
		// This would generate a set of 12 nodes (5 Pick up, 5 drop down, origin depot,
		// destination depot)
		// autoGenerateNodes(5);

		K = new Truck[3];
		// This vehicle has a capacity of 1 and can drive for 480 minutes.
		K[0] = new Truck(1, 480);
		// This vehicle has a capacity of 2 and can drive for 180 minutes.
		K[1] = new Truck(2, 180);
		// This vehicle has a capacity of 2 and can drive for 240 minutes.
		K[2] = new Truck(2, 240);

		c = new double[N.length][N.length];
		t = new double[N.length][N.length];

		// Calculate the distance (euclidean distance) and time (distance * 15) between
		// all nodes.
		double xDistance;
		double yDistance;

		for (int i = 0; i < N.length; i++) {
			for (int j = 0; j < N.length; j++) {
				if (i != j) {
					// Calculate the euclidean distance between all nodes.
					xDistance = Math.pow(N[i].getxPosition() - N[j].getxPosition(), 2);
					yDistance = Math.pow(N[i].getyPosition() - N[j].getyPosition(), 2);
					c[i][j] = Math.sqrt(xDistance + yDistance);

					// The travel time (in minutes) between a node i and a node j is the distance *
					// 15.
					t[i][j] = c[i][j] * 15;
				}
			}
		}

		try {
			cplex = new IloCplex();

			// Constraint 14: x has to be binary.
			x = new IloNumVar[N.length][N.length][K.length];
			for (int i = 0; i < N.length; i++) {
				for (int j = 0; j < N.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							x[i][j][k] = cplex.boolVar("x(" + "i" + i + ";j" + j + ";k" + k + ")");
						}
					}
				}
			}

			// objective function
			IloLinearNumExpr obj = cplex.linearNumExpr();
			for (int i = 0; i < N.length; i++) {
				for (int j = 0; j < N.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							obj.addTerm(c[i][j], x[i][j][k]);
						}
					}
				}
			}

			// Minimize the objective.
			cplex.addMinimize(obj);

			// Constraint 2: Visit every Pick up Location. (Serve every request exactly
			// once)
			for (int i = 1; i <= n; i++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
				}
				cplex.addEq(expr, 1.0, "Constraint2");
			}

			// Constraint 3: visit pickup and dropdown depot by the same vehicle.
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							if (n + i != j) {
								expr.addTerm(-1.0, x[n + i][j][k]);
							}
						}
					}
					cplex.addEq(expr, 0.0, "Constraint3");
				}
			}

			// Constraint 4: Start route at the origin depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int j = 0; j < N.length; j++) {
					if (j != 0) {
						expr.addTerm(1.0, x[0][j][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint4");
			}

			// Constraint 5: Flow constraint: Every Node from P union D (1..2n)
			// must have the same amount of edges going and edges going out.
			// The Nodes 0 and 2n+1 are not covered by this constraint, because
			// the route should start/end there.
			// The nodes must be visited by the same vehicle k.
			for (int i = 1; i <= 2 * n; i++) {
				for (int k = 0; k < K.length; k++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[j][i][k]);
						}
					}
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							expr.addTerm(-1.0, x[i][j][k]);
						}
					}
					cplex.addEq(expr, 0.0, "Constraint5");
				}
			}

			// Constraint 6: End Route at destination depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int i = 0; i < N.length; i++) {
					if (i != 2 * n + 1) {
						expr.addTerm(1.0, x[i][2 * n + 1][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint6");
			}

			// Continuous variable B_ik for the time a vehicle k starts its
			// service at node i.
			B = new IloNumVar[N.length][K.length];
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					B[i][k] = cplex.numVar(0, 1440, "ServiceTimeB(i" + i + ";k" + k + ")");
				}
			}

			// Constraint 7: Constraint is not linear. The linearized form
			// that is implemented here is listed in the paper as Constraint 15.
			// Constraint 15: The service at node j has to start after the
			// service at node i has been finished and the vehicle has driven
			// from node i to node j.
			double M;
			for (int i = 0; i < N.length; i++) {
				for (int j = 0; j < N.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							// Calculate M
							M = Math.max(0, N[i].getLatestServiceTime() + N[i].getServiceDuration() + t[i][j]
									- N[j].getEarliestServiceTime());

							IloLinearNumExpr expr = cplex.linearNumExpr();
							expr.addTerm(1.0, B[i][k]);
							expr.setConstant(N[i].getServiceDuration() + t[i][j] - M);
							expr.addTerm(M, x[i][j][k]);
							cplex.addGe(B[j][k], expr, "Constraint15");
						}
					}
				}
			}

			// Definition Variable Q_ik: Load of vehicle k after visiting node i.
			Q = new IloNumVar[N.length][K.length];
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					Q[i][k] = cplex.numVar(0, K[k].getCapacity(), "Q(i" + i + ";k" + k + ")");
				}
			}

			// Constraint 8: Constraint 8 is not linear. The linearized form of this
			// constraint
			// is listed as constraint 16 in the paper.
			// Constraint 16: The amount of load on vehicle k on node i plus the
			// load of node j does not exceed the capacity of vehicle k.
			double W;
			for (int i = 0; i < N.length; i++) {
				for (int j = 0; j < N.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							// Calculate W
							W = Math.min(K[k].getCapacity(), K[k].getCapacity() + N[i].getLoad());

							IloLinearNumExpr expr = cplex.linearNumExpr();
							expr.addTerm(1.0, Q[i][k]);
							expr.setConstant(N[j].getLoad() - W);
							expr.addTerm(W, x[i][j][k]);
							cplex.addGe(Q[j][k], expr, "Constraint16");
						}
					}
				}
			}

			// Maximum ride time of a user: For example 480 minutes = 8 hours.
			double lMaxRideTime = 480;

			// Definition L_i^k: The ride time of user i on vehicle k.
			L = new IloNumVar[N.length][K.length];
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					L[i][k] = cplex.numVar(0, lMaxRideTime, "L(i" + i + ";k" + k + ")");
				}
			}

			// Constraint 9: Set the ride time of each user.
			// Ride time of user i in vehicle k (L_i^k)
			// is equal to the ride time of user i + n minus (Ride time in
			// i plus service time in node i).
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[n + i][k]);
					expr.addTerm(-1.0, B[i][k]);
					expr.setConstant(-N[i].getServiceDuration());
					cplex.addEq(L[i][k], expr, "Constraint9");
				}
			}

			// Constraint 10: The duration of a tour may not exceed the
			// maximum time allowed for one vehicle.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0, B[2 * n + 1][k]);
				expr.addTerm(-1.0, B[0][k]);
				cplex.addLe(expr, K[k].getMaxTourTime(), "Constraint10");
			}

			// Constraint 11: Nodes must be visited within their service time.
			// Impose time window constraints.
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addLe(N[i].getEarliestServiceTime(), B[i][k], "Constraint11_1");
					cplex.addLe(B[i][k], N[i].getLatestServiceTime(), "Constraint11_2");
				}
			}

			// Constraint 12: The current travel time must be longer than the travel
			// time from node i to node j and must not be longer than the permitted
			// travel time.
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addLe(t[i][n + i], L[i][k], "Constraint12_1");
					cplex.addLe(L[i][k], lMaxRideTime, "Constraint12_2");
				}
			}

			// Constraint 13: impose capacity constraint
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addLe(Math.max(0, N[i].getLoad()), Q[i][k], "Constraint13_1");
					cplex.addLe(Q[i][k], Math.min(K[k].getCapacity(), K[k].getCapacity() + N[i].getLoad()),
							"Constraint13_2");
				}
			}

			// Export the model and saves it in the same location
			// where this file is stored.
			cplex.exportModel("Cordeau.lp");

			solveModel();

			cplex.end();

		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to look up the route of a vehicle.
	 * 
	 * @param row
	 * @param truck
	 * @return The next node on the route.
	 */
	public static int getNextNode(int row, int truck) {
		for (int i = 0; i <= 2 * n + 1; i++) {
			try {
				if (i != row) {
					if (Math.round(cplex.getValue(x[row][i][truck])) == 1) {
						return i;
					}
				}
			} catch (UnknownObjectException e) {
				e.printStackTrace();
			} catch (IloException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * Automatically generates random nodes.
	 * 
	 * @param numberOfNodes Amount of pick-up nodes.
	 */
	public static void autoGenerateNodes(int numberOfNodes) {
		// Auto generate Nodes:
		n = numberOfNodes;

		N = new Node[2 * n + 2];
		
		// Explanation of the arguments: new Node(1, 2, 0, 480, 0, 0);
		// 		x-Position of the node = 1
		// 		y-Position of the node = 2
		// 		earliest service time = 0 (start service directly)
		// 		latest service time = 480 (end service after 8 hours)
		// 		load (how many people should be transported) = 0 people
		// 		service duration (how long takes it to load the people in the vehicle) = 0 minutes
		
		// Start Node is the origin depot.
		N[0] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, 0, 0);

		// Pick up nodes 1..n
		for (int i = 1; i <= n; i++) {
			N[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, 1, 5);
		}

		// Drop down nodes n+1..2n
		for (int i = n + 1; i <= 2 * n; i++) {
			N[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, -1, 5);
		}

		// Destination Node 2n+1 is the last node.
		N[2 * n + 1] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, 0, 0);
	}

	/**
	 * Generate a default set of Nodes.</br>
	 * 5 pickup locations, 5 dropdown locations, the start node and a end node will
	 * be created.
	 */
	public static void setDefaultNodes() {
		n = 5;

		// A total of 12 nodes.
		N = new Node[12];

		// Explanation of the arguments: new Node(1, 2, 0, 480, 0, 0);
		// 		x-Position of the node = 1
		// 		y-Position of the node = 2
		// 		earliest service time = 0 (start service directly)
		// 		latest service time = 480 (end service after 8 hours)
		// 		load (how many people should be transported) = 0 people
		// 		service duration (how long takes it to load the people in the vehicle) = 0 minutes
		
		// Start node.
		N[0] = new Node(1, 2, 0, 480, 0, 0);

		// Pick up nodes.
		N[1] = new Node(1, 1, 0, 480, 1, 5);
		N[2] = new Node(1, 4, 0, 480, 1, 5);
		N[3] = new Node(4, 3, 0, 480, 1, 5);
		N[4] = new Node(2, 2, 0, 480, 1, 5);
		N[5] = new Node(2, 4, 0, 480, 1, 5);

		// Drop down nodes.
		N[6] = new Node(4, 1, 0, 480, -1, 5);
		N[7] = new Node(4, 4, 0, 480, -1, 5);
		N[8] = new Node(1, 3, 0, 480, -1, 5);
		N[9] = new Node(3, 4, 0, 480, -1, 5);
		N[10] = new Node(3, 1, 0, 480, -1, 5);

		// End depot.
		N[11] = new Node(3, 2, 0, 480, 0, 0);
	}

	/**
	 * Solve the model and print the output to the console.
	 */
	private static void solveModel() {
		try {
			// Solve the model
			if (cplex.solve()) {
				// Print the result
				System.out.println("Solution status: " + cplex.getStatus());
				System.out.println("--------------------------------------------");
				System.out.println();
				System.out.println("Solution found:");
				System.out.println(" Objective value = " + cplex.getObjValue());
				System.out.println();

				for (int k = 0; k < K.length; k++) {
					System.out.println("Solution for Truck " + k + ":");

					System.out.println("Route duration: " + Math.round(cplex.getValue(B[2 * n + 1][k])) + " minutes.");

					int nextNode = getNextNode(0, k);
					System.out.print("Route: 0 -> ");
					while (nextNode != 0) {
						if (nextNode != 2 * n + 1) {
							System.out.print(nextNode + " -> ");
						} else {
							System.out.print(nextNode);
						}
						nextNode = getNextNode(nextNode, k);
					}
					System.out.println();
					System.out.println();
				}
			} else {
				System.out.println("No solution exists");
			}
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

}
