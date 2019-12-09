package logic;

import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;
import ilog.concert.*;

public class model {
	
	/**
	 * Here you can change the number of users. 
	 */
	private static int n;
	private static IloCplex cplex;
	private static IloNumVar[][][] x;
	private static Node[] N;

	public static void main(String[] args) {
		
		// Uncomment if you want to automatically create Nodes
//		autoGenerateNodes(5);
		
		//Generate a predefined set of nodes.
		setDefaultNodes();
		
		
		//Print Node Positions for Excel.
//		System.out.println("Knoten\txPosition\tyPosition");
//		for (int i = 0; i <= 2*n+1; i++) {
//			System.out.println(i + "\t" + N[i].getxPosition() + "\t" + N[i].getyPosition());
//		}
		
		Truck[] K = new Truck[2];
		K[0] = new Truck(2, 1000);
		K[1] = new Truck(2, 1000);
//		K[2] = new Truck(0, 1000); 		// Mit einem Truck ohne Kapazität (capacity = 0) gibt es Bound infeasibility column 'Q(i1;k2)'.
		

		// c enthält die Distanz zwischen allen Knoten
		double[][] c = new double[N.length][N.length];
		// t enthält die Fahrzeit zwischen allen Knoten.
		double[][] t = new double[N.length][N.length];
		
		double xDistance;
		double yDistance;
		
		for (int i = 0; i < N.length; i++) {
			for (int j = 0; j < N.length; j++) {
				if (i != j) {
					xDistance = Math.pow(N[i].getxPosition() - N[j].getxPosition(), 2);
					yDistance = Math.pow(N[i].getyPosition() - N[j].getyPosition(), 2);
					c[i][j] = Math.sqrt(xDistance + yDistance);
					
					// Die Fahrzeit zwischen i und j ist die Entfernung zwischen den Knoten * 60.
					t[i][j] = c[i][j] * 60;
				}
			}
		}
		
		try {
			cplex = new IloCplex();
			
			// Binary decision variable.
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
			
			// Zielfunktion
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
			
			//Constraint 2: Visit every Pick up Location. (Serve every request exactly once)
			//Funktioniert!
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
			
			//Constraint 3: visit pickup and dropdown depot by the same vehicle.
			for (int i = 1; i <= n; i++)  {
				for (int k = 0; k < K.length; k++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
					for (int j = 0; j < N.length; j++) {
						if (i != j) {
							if (n+i != j)
							expr.addTerm(-1.0, x[n+i][j][k]);
						}
					}
					cplex.addEq(expr, 0.0, "Constraint3");
				}
			}
			
			//Constraint 4: Start route at the origin depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int j = 0; j < N.length; j++) {
					if (j != 0) {
						expr.addTerm(1.0, x[0][j][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint4");
			}
			
			//Constraint 5: Flow constraint: Every Node from P union D (1..2*n) 
			//must have the same amount of edges going and edges going out.
			//The Nodes 0 and 2n+1 (7) are not covered by this constraint, because
			//the route should start/end there.
			for (int i = 1; i <= 2*n; i++) {
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
			
			//Constraint 6: End Route at destination depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int i = 0; i < N.length; i++) {
					if (i != 2*n+1) {
						expr.addTerm(1.0, x[i][2*n+1][k]);	
					}
				}
				cplex.addEq(expr, 1.0, "Constraint6");
			}
			
			// Kontinuirliche Variable B_ik für die Zeit, an der Truck seinen
			// Service an knoten i beginnt.
			IloNumVar[][] B = new IloNumVar[N.length][K.length];
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					B[i][k] = cplex.numVar(0, Integer.MAX_VALUE, "ServiceTimeB(i" + i + ";k" + k + ")");
				}
			}
			
			//Constraint 7: Der Service an Knoten j kann erst beginnen, 
			//nachdem der Service an Knoten i abgeschlossen wurde und der 
			//LKW von i nach j gefahren ist.
			double M;
			for (int i = 0; i < N.length; i++) {
				for (int j = 0; j < N.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							IloLinearNumExpr expr = cplex.linearNumExpr();
							M = Math.max(0, N[i].getEndServiceTime() + N[i].getServiceDuration() + t[i][j] - N[j].getBeginServiceTime());
							expr.addTerm(1.0, B[i][k]);
							expr.setConstant(N[i].getServiceDuration() + t[i][j] - M);
							expr.addTerm(M, x[i][j][k]);
							cplex.addGe(B[j][k], expr, "Constraint7");
						}
					}
				}
			}
			
			//Definition Variable Q_ik: Load of vehicle k after visiting node i.
			IloNumVar[][] Q = new IloNumVar[N.length][K.length];
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					Q[i][k] = cplex.numVar(-2, 2, "Q(i" + i + ";k" + k + ")");
				}
			}
			
			//Constraint 8: Die geladenen Ressourcen auf LKW k müssen bei Knoten i 
			//plus dem Load von Knoten i kleiner/gleich den geladenen Ressourcen
			//bei Knoten j sein.
			double W;
			for (int i = 0; i < N.length; i++) {
				for (int j = 0; j < N.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							W = Math.min(K[k].getCapacity(), K[k].getCapacity() + N[i].getLoad());
							IloLinearNumExpr expr = cplex.linearNumExpr();
							expr.addTerm(1.0, Q[i][k]);
							expr.setConstant(N[j].getLoad() - W);
							expr.addTerm(W, x[i][j][k]);
							cplex.addGe(Q[j][k], expr, "Constraint8");
						}
					}
				}
			}
			
			//Maximum ride time of a user: For example 180 Minutes.
			double lMaxRideTime = 360;
			
			//Definition L_i^k: The ride time of user i on vehicle k.
			IloNumVar[][] L = new IloNumVar[N.length][K.length];
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					L[i][k] = cplex.numVar(0, lMaxRideTime, "L(i" + i + ";k" + k + ")");
				}
			}
			
			//Constraint 9: Set the ride time of each user.
			//Ride time of user i in vehicle k (L_i^k) 
			//ist gleich Ride Time of user i + n minus (Ride time in 
			//i plus service time in i).
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[n+i][k]);
					expr.addTerm(-1.0, B[i][k]);
					expr.setConstant(-N[i].getServiceDuration());
					cplex.addEq(L[i][k], expr, "Constraint9");
				}
			}
			
			//Constraint 10: Dauer einer Tour darf die maximale 
			//Tourzeit eines LKWs nicht überschreiten.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0, B[2*n+1][k]);
				expr.addTerm(-1.0, B[0][k]);
				cplex.addLe(expr, K[k].getMaxTourTime(), "Constraint10");
			}
			
			
			//Constraint 11: Knoten müssen innerhalb ihrer Servicezeit besucht werden.
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addLe(N[i].getBeginServiceTime(), B[i][k], "Constraint11_1");
					cplex.addLe(B[i][k], N[i].getEndServiceTime(), "Constraint11_2");
				}
			}
			
			//Constraint 12: Ride time jedes users muss größer als
			//die Fahrzeit von Knoten i nach Knoten j sein und kleiner als 
			//der maximal erlaubte Fahrzeit.
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addLe(t[i][n+i], L[i][k], "Constraint12_1");
					cplex.addLe(L[i][k], lMaxRideTime, "Constraint12_2");
				}
			}
			
			//Constraint 13: 
			for (int i = 0; i < N.length; i++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addLe(Math.max(0, N[i].getLoad()), Q[i][k], "Constraint13_1");
					cplex.addLe(Q[i][k], Math.min(K[k].getCapacity(), K[k].getCapacity() + N[i].getLoad()), "Constraint13_2");
				}
			}
			
			// Exportieren des Modells
			cplex.exportModel("Cordeau.lp");
			
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
					System.out.println("Solution for Truck " + k);
					for (int i = 0; i <= 2*n+1; i++) {
						System.out.print("\t" + i);
					}
					System.out.println();
					for (int i = 0; i < N.length; i++) {
						System.out.print(i + "\t");
						for (int j = 0; j < N.length; j++) {
							if (i != j) {
								// Möglicherweise müssen Werte gerundet werden.
								if (cplex.getValue(x[i][j][k]) == 0) {
									System.out.print("-\t");
								} else {
									System.out.print(Math.round(cplex.getValue(x[i][j][k])) + "\t");
								}
							} else {
								System.out.print("\\\t");
							}
						}
						System.out.println();
					}
					
					System.out.println("Route duration for Truck " + k + ": " + Math.round(cplex.getValue(B[2*n+1][k])) + " minutes.");
					
					int nextNode = getNextNode(0, k);
					System.out.print("Route: 0 -> ");
					while (nextNode != 0) {
						if (nextNode != 2*n+1) {
							System.out.print(nextNode + " -> ");
						} else {
							System.out.print(nextNode);
						}
						nextNode = getNextNode(nextNode, k);
					}
					System.out.println();
					nextNode = getNextNode(0, k);
					System.out.println("Knoten\txPosition\tyPosition");
					System.out.println("0\t" + N[0].getxPosition() + "\t" + N[0].getyPosition());
					while (nextNode != 0) {
						System.out.println(nextNode + "\t" + N[nextNode].getxPosition() + "\t" + N[nextNode].getyPosition());
						nextNode = getNextNode(nextNode, k);
					}
					System.out.println();
				}
			} else {
				System.out.println("No solution exists");
			}
			
			
			
			cplex.end();
			
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	public static int getNextNode(int row, int truck) {
		for (int i = 0; i <= 2*n+1; i++) {
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
	 * Automatically generates random Nodes.
	 * @param numberOfNodes
	 */
	public static void autoGenerateNodes(int numberOfNodes) {
		//Auto generate Nodes:
		n = numberOfNodes;
		
		N = new Node[2*n+2];
		//Start Node is the origin depot.
		N[0] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 1440, 0, 0);
		
		//Pick up nodes 1..n
		for (int i = 1; i <= n; i++) {			
			N[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 1440, 1, 30);
		}
		
		//Drop down nodes n+1..2n
		for (int i = n+1; i <= 2*n; i++) {
			N[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 1440, -1, 30);
		}
		
		//Destination Node 2n+1 is the last node.
		N[2*n+1] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 1440, 0, 0);
	}
	
	/**
	 * Generate a default set of Nodes.</br>
	 * 3 pickup locations, 3 dropdown locations, the start node and a end node will be created.
	 */
	public static void setDefaultNodes() {
		n = 5;
		
		N = new Node[12];
		// The start node.
		N[0] = new Node(1, 2, 0, 1440, 0, 0);
		
		//The pick up nodes.
		N[1] = new Node(1, 1, 0, 1440, 1, 30);
		N[2] = new Node(1, 4, 0, 1440, 1, 30);
		N[3] = new Node(4, 3, 0, 1440, 1, 30);
		N[4] = new Node(2, 2, 0, 1440, 1, 30);
		N[5] = new Node(2, 4, 0, 1440, 1, 30);
		
		//The drop down nodes.
		N[6] = new Node(4, 1, 0, 1440, -1, 30);
		N[7] = new Node(4, 4, 0, 1440, -1, 30);
		N[8] = new Node(1, 3, 0, 1440, -1, 30);
		N[9] = new Node(3, 4, 0, 1440, -1, 30);
		N[10] = new Node(3, 1, 0, 1440, -1, 30);
		
		//The end depot.
		N[11] = new Node(3, 2, 0, 1440, 0, 0);
	}

}
