package logic;

import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;
import ilog.concert.*;

/**
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
	 * Number of users (number of pick-up locations)
	 */
	private static int n;
	
	/**
	 * Number of AFSs
	 */
	private static int f;
	
	/**
	 * Number of dummy AFSs
	 */
	private static int fStrich;
	
	/**
	 * Array containing all nodes.
	 */
	private static Node[] VStrich;
	
	/**
	 * Array containing all vehicles.
	 */
	private static Truck[] K;
	
	/**
	 * Maximum duration of a route.
	 */
	private static double Tmax = 480;
	
	/**
	 * Maximum transportation time of a container.
	 */
	private static double Lmax = 240;
	
	/**
	 * The decision variable x.
	 */
	private static IloNumVar[][][] x;
	
	/**
	 * Load of resource r on vehicle k after visiting node i.
	 */
	private static IloNumVar[][][] Q;
	
	/**
	 * Time which vehicle k starts its service at node i.
	 */
	private static IloNumVar[][] B;
	
	/**
	 * Time variable specifying the arrival time of vehicle k at node i
	 */
	private static IloNumVar[][] A;
	
	/**
	 * Transportation time of a loaded container from node i on vehicle k.
	 */
	private static IloNumVar[][] l;
	
	/**
	 * Fuel level on vehicle k after departure to node i.
	 */
	private static IloNumVar[][] z;
	
	/**
	 * Distance between node i and node j.</br>
	 * Simple euclidean distance.
	 */
	private static double[][] c;
	
	/**
	 * The travel time in minutes between node i and node j.</br>
	 * Is calculated as distance * 15
	 */
	private static double[][] t;



	public static void main(String[] args) {

		// Use a predefined set of nodes (definitely solvable)
		setDefaultNodes();
		// Or generate a random set of nodes (maybe not solvable)
		// If you use a large number of nodes you have to raise
		// the number of vehicles to get a solution
//		autoGenerateNodes(5, 3);
		// This would generate a set of 12 nodes (5 Pick up, 5 drop down, origin depot,
		// destination depot)

		// Print node positions in the console for e.g. a visualization in excel.
//		System.out.println("Node\tx-Pos.\ty-pos");
//		for (int i = 0; i <= 2*n+1; i++) {
//			System.out.println(i + "\t" + N[i].getxPosition() + "\t" + N[i].getyPosition());
//		}

		// Alle Trucks müssen die selben Container transportieren können.
		K = new Truck[3];
		// This truck can carry one full 20" container, is allowed to drive 460 minutes maximum 
		// and has a tank capacity of 7.
		K[0] = new Truck(new int[] { 1, 0, 0, 0 }, 7);
		K[1] = new Truck(new int[] { 1, 0, 0, 0 }, 7);
		K[2] = new Truck(new int[] { 2, 0, 0, 0 }, 7); 
//		K[3] = new Truck(new int[] { 1, 0, 0, 0 }, 7); 
//		K[4] = new Truck(new int[] { 2, 0, 0, 0 }, 5); 
//		K[5] = new Truck(new int[] { 2, 0, 0, 0 }, 5); 
//		K[6] = new Truck(new int[] { 1, 0, 0, 0 }, 5); 
		// Mit einem Truck ohne Kapazität (capacity = 0) gibt es Bound
															// infeasibility column 'Q(i1;k2)'.

		// c enthält die Distanz zwischen allen Knoten
		c = new double[VStrich.length][VStrich.length];
		// t enthält die Fahrzeit zwischen allen Knoten.
		t = new double[VStrich.length][VStrich.length];

		double xDistance;
		double yDistance;

		for (int i = 0; i < VStrich.length; i++) {
			for (int j = 0; j < VStrich.length; j++) {
				if (i != j) {
					// Calculate the euclidean distance between all nodes.
					xDistance = Math.pow(VStrich[i].getxPosition() - VStrich[j].getxPosition(), 2);
					yDistance = Math.pow(VStrich[i].getyPosition() - VStrich[j].getyPosition(), 2);
					c[i][j] = Math.sqrt(xDistance + yDistance);

					// The travel time (in minutes) between a node i and a node j is the distance *
					// 15.
					t[i][j] = c[i][j] * 15;
				}
			}
		}

		try {
			cplex = new IloCplex();

			// Constraint 26: Decision variable x has to be binary.
			x = new IloNumVar[VStrich.length][VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int j = 0; j < VStrich.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							x[i][j][k] = cplex.boolVar("x(" + "i" + i + ";j" + j + ";k" + k + ")");
						}
					}
				}
			}

			// objective function
			IloLinearNumExpr obj = cplex.linearNumExpr();
			for (int i = 0; i < VStrich.length; i++) {
				for (int j = 0; j < VStrich.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							// Diese Zielfunktion ermöglicht unnötige Stopps am Start- und Zieldepot.
//							obj.addTerm(c[i][j], x[i][j][k]);
							
							// Durch einbeziehen der Zeit werden diese sinnlosen Stopps nicht mehr gemacht.
							obj.addTerm(VStrich[j].getServiceDuration(), x[i][j][k]);
						}
					}
				}
			}

			// Minimize the objective.
			cplex.addMinimize(obj);

			// Constraint 2: Visit every Pick up Location. (Serve every request exactly once)
			for (int i = 1; i <= 2*n; i++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
				}
				cplex.addEq(expr, 1.0, "Constraint2");
			}
			
			// Constraint 3. Visit every Dropdown location once. (Serve every request exactly once)
			for (int j = 1; j <= 2*n; j++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int i = 0; i < VStrich.length; i++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
				}
				cplex.addEq(1.0, expr, "Constraint3");
			}

			// Constraint 4: visit pickup and dropdown node by the same vehicle.
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
					
					for (int j = 0; j < VStrich.length; j++) {
						if (n + i != j)
							expr.addTerm(-1.0, x[n + i][j][k]);
					}
					cplex.addEq(expr, 0.0, "Constraint4");
				}
			}

			// Constraint 5: Flow constraint: Every Node from V'
			// must have the same amount of edges going in and edges going out.
			// The Nodes 0 and 2*n+f+1 are not covered by this constraint, because
			// the route should start/end there.
			for (int k = 0; k < K.length; k++) {
				for (int j = 0; j < VStrich.length; j++) {
					// Start- und Zieldepot sind vom Flow Constraint ausgenommen.
					if (j != 0) {
						// Start- und Zieldepot sind vom Flow Constraint ausgenommen.
						if (j != 2*n+f+1) {
							IloLinearNumExpr expr = cplex.linearNumExpr();
							for (int i = 0; i < VStrich.length; i++) {
								if (j != i) {
									expr.addTerm(1.0, x[i][j][k]);
								}
							}
							for (int i = 0; i < VStrich.length; i++) {
								if (j != i) {
									expr.addTerm(-1.0, x[j][i][k]);
								}
							}
							cplex.addEq(expr, 0.0, "Constraint5");
						}
					}
				}
			}
			
			//Constraint 6: If a dummy AFS is visited on a route, it has
			// at least one successor node (client, AFS, depot).
			for (int i = 1; i <= fStrich; i++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int j = 0; j < VStrich.length; j++) {
						if (j != 2*n+f+1+i) {
							expr.addTerm(1.0, x[2*n+f+1+i][j][k]);	
						}
					}
				}
				cplex.addLe(expr, 1.0, "Constraint6");
			}
			
			
			// Constraint 7: Start route at the origin depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int j = 0; j < VStrich.length; j++) {
					if (j != 0) {
						expr.addTerm(1.0, x[0][j][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint7");
			}

			// Constraint 9 Pesch: End Route at destination depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int i = 0; i < VStrich.length; i++) {
					if (i != 2 * n + f + 1) {
						expr.addTerm(1.0, x[i][2 * n + f + 1][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint9");
			}
			
			// Definition Variable Q_ik: Load of vehicle k after visiting node i.
			Q = new IloNumVar[VStrich.length][4][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int r = 0; r <= 3; r++) {
					for (int k = 0; k < K.length; k++) {
						Q[i][r][k] = cplex.numVar(0, K[k].getContainerCapacity()[r], "Q(i" + i + ";r" + r + ";k" + k + ")");
					}
				}
			}

			// Constraint 10: Die geladenen Ressourcen auf LKW k müssen bei Knoten i
			// plus dem Load von Knoten i kleiner/gleich den geladenen Ressourcen
			// bei Knoten j sein.
			for (int k = 0; k < K.length; k++) {
			    // Modell geändert: Es wird anstatt N=PuD V' genommen. Da die Beschränkung für alle 
				// Knoten gelten soll. 
 				for (int i = 0; i < VStrich.length; i++) {
				// Hier das selbe wie beim vorherigen Kommentar. Für Start- und Zielknoten 
				// gilt diese Bedingung auch.
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							for (int r = 0; r <= 3; r++) {
								IloLinearNumExpr expr1 = cplex.linearNumExpr();
								expr1.addTerm(1.0, Q[i][r][k]);
								expr1.setConstant(VStrich[j].getLoad()[r] + K[k].getContainerCapacity()[r]);
								expr1.addTerm(-K[k].getContainerCapacity()[r], x[i][j][k]);
								cplex.addLe(Q[j][r][k], expr1, "Contraint10a");
								
								IloLinearNumExpr expr2 = cplex.linearNumExpr();
								expr2.addTerm(1.0, Q[i][r][k]);
								expr2.setConstant(VStrich[j].getLoad()[r] - K[k].getContainerCapacity()[r]);
								expr2.addTerm(K[k].getContainerCapacity()[r], x[i][j][k]);
								cplex.addGe(Q[j][r][k], expr2, "Constraint10b");
							}
						}
					}
				}
			}
			
			// Constraint 11: impose capacity constraints
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					for (int r = 0; r <= 3; r++) {
						cplex.addLe(0.0, Q[i][r][k], "Constraint11_1");
						cplex.addLe(Q[i][r][k], K[k].getContainerCapacity()[r], "Constraint11_2");
					}
				}
			}
			
			// Constraint 12 Pesch: Leere und volle 30 Fuß Container dürfen die Kapazität des LKWs
			// nicht überschreiten. Bsp. Ein LKW kann nicht 2 volle 30 Fuß Container und 2 
			// leere 30 Fuß Container laden, da er nur Platz für insgesamt 2 Container hat.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, Q[i][0][k]);
					expr.addTerm(1.0, Q[i][1][k]);
					cplex.addLe(expr, K[k].getContainerCapacity()[0], "Constraint12");
				}
			}

			// Constraint 13 Pesch: Leere und volle 60 Fuß Container zusammen dürfen die Kapazität
			// des LKWs nicht übeschreiten. Bsp.: Es kann nicht ein voller und ein leerer 60"
			// Container gleichzeitig geladen sein.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, Q[i][2][k]);
					expr.addTerm(1.0, Q[i][3][k]);
					cplex.addLe(expr, K[k].getContainerCapacity()[2], "Constraint13");
				}
			}
			
			// Constraint 14: Start a route without containers
			for (int k = 0; k < K.length; k++) {
				for (int r = 0; r <= 3; r++) {
					cplex.addEq(Q[0][r][k], 0.0, "Constraint14");
				}
			}

			// Constraint 15 Pesch: End routes without containers
			for (int k = 0; k < K.length; k++) {
				for (int r = 0; r <= 3; r++) {
					cplex.addEq(Q[2*n+f+1][r][k], 0.0, "Constraint15");
				}
			}
			

			// Kontinuirliche Variable B_ik für die Zeit, an der Truck seinen
			// Service an Knoten i beginnt.
			B = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					B[i][k] = cplex.numVar(VStrich[i].getEarliestServiceTime(), VStrich[i].getLatestServiceTime(), "ServiceTimeB(i" + i + ";k" + k + ")");
				}
			}

			// Definition l_i^k: The ride time of user i on vehicle k.
			l = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					l[i][k] = cplex.numVar(0, Tmax, "l(i" + i + ";k" + k + ")");
				}
			}
			
			// Constraint 16: Precedence Relation with Constraints 17 and 18.
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[i][k]);
					expr.setConstant(t[i][i+n]);
					cplex.addGe(B[n+i][k], expr, "Constraint16");
				}
			}
			

			// Constraint 17: Set the ride time of each user.
			// Ride time of user i in vehicle k (L_i^k)
			// ist gleich Ride Time of user i + n minus (Ride time in
			// i plus service time in i).
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[n+i][k]);
					expr.addTerm(-1.0, B[i][k]);
					expr.setConstant(-VStrich[i].getServiceDuration());
					cplex.addEq(l[i][k], expr, "Constraint17");
				}
			}
			
			// Constraint 18 Pesch: Ride time jedes Users muss größer als
			// die Fahrzeit von Knoten i nach Knoten j sein und kleiner als
			// der maximal erlaubte Fahrzeit.
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					cplex.addLe(t[i][n+i], l[i][k], "Constraint18_1");
					cplex.addLe(l[i][k], Lmax, "Constraint18_2");
				}
			}
			
			// Definition of A_i^k: Time a truck k arrives at node i.
			A = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					A[i][k] = cplex.numVar(0, Tmax, "A(i" + i + ";k" + k + ")"); 
				}
			}
			
			// Constraint 19 Pesch: Der Service an Knoten j kann erst beginnen,
			// nachdem der Service an Knoten i abgeschlossen wurde und der
			// LKW von i nach j gefahren ist.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							IloLinearNumExpr expr1 = cplex.linearNumExpr();
							expr1.addTerm(1.0, B[i][k]);
							expr1.addTerm(t[i][j], x[i][j][k]);
							expr1.addTerm(VStrich[i].getServiceDuration(), x[i][j][k]);
							expr1.setConstant(Tmax);
							expr1.addTerm(-Tmax, x[i][j][k]);
							cplex.addLe(B[j][k], expr1, "Constraint19a");
							
							IloLinearNumExpr expr2 = cplex.linearNumExpr();					
							expr2.addTerm(1.0, B[i][k]);
							expr2.addTerm(t[i][j], x[i][j][k]);
							expr2.addTerm(VStrich[i].getServiceDuration(), x[i][j][k]);
							expr2.setConstant(-Tmax);
							expr2.addTerm(Tmax, x[i][j][k]);
							cplex.addGe(B[j][k], expr2, "Constraint19b");
						}
					}
				}
			}
			
			// Constraint 20 Pesch: Knoten müssen innerhalb ihrer Servicezeit besucht werden.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					cplex.addLe(VStrich[i].getEarliestServiceTime(), B[i][k], "Constraint20_1");
					cplex.addLe(B[i][k], VStrich[i].getLatestServiceTime(), "Constraint20_2");
				}
			}
			
			// Constraint 21: Dauer einer Tour darf die maximale Fahrzeit Tmax
			// eines LKWs nicht überschreiten.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0, B[2*n+f+1][k]);
				expr.addTerm(-1.0, B[0][k]);
				cplex.addLe(expr, Tmax, "Constraint21");
			}
			
			
			z = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					z[i][k] = cplex.numVar(0, K[k].getTankCapacity(), "z(i" + i + ";k" + k + ")");
				}
			}
			
			
//			// Constraint 22 Pesch : Fuel Capacity verringert sich mit jedem besuchten Knoten.
//			for (int k = 0; k < K.length; k++) {
//				// Nur Startdepot, Pick-up und Dropdown, Zieldepot.
//				for (int i = 0; i <= 2*n+f+1; i++) {
//					// AFSs auslassen
//					if (i < 2*n+1 || i > 2*n+f) {
//						// Nur die AFS und dummy AFS
//						for (int j = 0; j <= 2*n; j++) {
//							// AFSs auslassen
//							if (j < 2*n+1 || j > 2*n+f) {
//								if (i != j) {
//									IloLinearNumExpr expr = cplex.linearNumExpr();
//									expr.setConstant(K[k].getTankCapacity());
//									expr.addTerm(-K[k].getTankCapacity(), x[i][j][k]);
//									expr.addTerm(1.0, z[i][k]);
//									expr.addTerm(-1.0, z[j][k]);
//									//FR ist 1 daher taucht es hier nicht explizit auf.
//									expr.addTerm(-c[i][j], x[i][j][k]);
//									cplex.addGe(expr, 0.0, "Constraint22");
//								}
//							}
//						}
//					}
//				}
//			}
			
			
//			//Constraint 23 Pesch: 
//			for (int k = 0; k < K.length; k++) {
//				// Nur Startdepot, Pick-up und Dropdown, Zieldepot.
//				for (int i = 0; i <= 2*n+f+1; i++) {
//					if (i < 2*n+1 || i > 2*n+f) { // AFSs auslassen
//						// Nur die AFS und dummy AFS
//						for (int j = 2*n+1; j <= 2*n+f+fStrich+1; j++) {
//							if (j != 2*n+f+1) { // Zieldepot auslassen
//								if (i != j) {
//									IloLinearNumExpr expr = cplex.linearNumExpr();
//									expr.setConstant(K[k].getTankCapacity());
//									expr.addTerm(-K[k].getTankCapacity(), x[i][j][k]);
//									expr.addTerm(1.0, z[i][k]);
//									expr.addTerm(-1.0, z[j][k]);
//									expr.addTerm(-c[i][j], x[i][j][k]);
//									cplex.addGe(expr, 0.0, "Constraint23");
//								}
//							}
//						}
//					}
//				}
//			}
		
			
//			// Constraint 24 Pesch: Set z to fuelCapacity of the vehicle.
//			// Eigene Linearisierung
//			for (int i = 0; i < VStrich.length; i++) {
//				// Für alle AFS und dummy AFS
//				for (int j = 2*n+1; j <= 2*n+f+fStrich+1; j++) {
//					// Zieldepot herausnehmen
//					if (j != 2*n+f+1) {
//						if (i != j) {
//							for (int k = 0; k < K.length; k++) {
//								IloLinearNumExpr expr = cplex.linearNumExpr();
//								expr.addTerm(1.0, z[j][k]);
//								expr.setConstant(-K[k].getTankCapacity());
//								expr.addTerm(K[k].getTankCapacity(), x[i][j][k]);
//								cplex.addLe(expr, 15.0, "Constraint24");
//							}
//						}
//					}
//				}
//			}
			
			// Constraint 25 Pesch: Set Fuel level on start depot to tankCapacity.
			// Geht das überhaupt oder muss dort x = 1 => Constraint, wie bei Constraint 24.
			// Das ist eine sinnlose Angelegenheit, da eine Route maximal einmal (nämlich beim Start)
			// beim Startdepot vorbeikommt. Tanken am Startdepot ist also nicht.
			// Macht das Modell minimal schneller, da kein "optimaler" Starttankinhalt berechnet wird.
			for (int k = 0; k < K.length; k++) {
				cplex.addEq(z[0][k], K[k].getTankCapacity(), "Constraint25");
			}
			
			//Constraint 10 Erdogan: Reduce Fuel based on traveled distance
			for (int k = 0; k < K.length; k++) {
				// Hier müsste meiner Meinung nach j = 0 bis j < V.length hin, da es für 
				// alle Knoten und nicht nur für die Kunden gelten soll.
				// Das funktioniert aber nicht, warum auch immer.
//				for (int j = 1; j <= 2*n; j++) {
//				for (int j = 0; j <  VStrich.length; j++) {
				for (int i = 0; i < VStrich.length; i++) {
					// Für alle Pudu{o,2n+f+1} (Alle Knoten außer die AFSs und dummy AFSs)
					for (int j = 0; j <= 2*n+f+1; j++) {
						if (j < 2*n+1 || j > 2*n+f) {
							if (i != j) {
								IloLinearNumExpr expr = cplex.linearNumExpr();
								expr.addTerm(1.0, z[i][k]);
								expr.addTerm(-c[i][j], x[i][j][k]);
								expr.setConstant(K[k].getTankCapacity());
								expr.addTerm(-K[k].getTankCapacity(), x[i][j][k]);
								cplex.addLe(z[j][k], expr, "Constraint10Erdogan");
							}
						}
					}
				}
			}
			
			// Constraint 11 Erdogan: Set fuel level on max. fuel level when visiting an AFS.
			for (int j = 2*n+1; j <= 2*n+f+fStrich+1; j++) {
				// Um alle AFSs, auch die dummy AFSs, muss j von 2*n+1 bis 2*n+f+fStrich+1 gehen
				// und das Zieldepot 2*n+f+1 muss ausgenommen sein.
				if (j != 2*n+f+1) {
					for (int k = 0; k < K.length; k++) {
						cplex.addEq(z[j][k], K[k].getTankCapacity(),"Constraint11Erdogan");
					}
				}
			}
			
			// Constraint 12 Erdogan: Remaining fuel must be sufficient to reach AFS
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					for (int j = 2*n+1; j <= 2*n+f+fStrich+1; j++) {
						if (j != 2*n+f+1) {
							if (i != j) {
								// Fuel must be sufficient to reach destination depot or next AFS. 
								// Doesn't work right in some cases.
//								cplex.addGe(z[i][k], Math.min(c[i][j], c[i][2*n+f+1]), "Constraint12Erdogan");
								
								// A dummy AFS is behind the destination and origin depot.
								// I check only if fuel is enough to reach the next AFSs. This is thus the depot.
								cplex.addGe(z[i][k], c[i][j], "Constraint12Erdogan");
							}
						}
					}
				}
			}
			


			// Exportieren des Modells
			cplex.exportModel("Cordeau.lp");

			solveModel();

			cplex.end();

		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to look up the route of a vehicle.
	 * @param row
	 * @param truck
	 * @return The next node on the route.
	 */
	public static int getNextNode(int row, int truck) {
		for (int i = 0; i < VStrich.length; i++) {
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
	 * Generate a default set of Nodes.</br>
	 * 6 pickup locations, 6 dropdown locations, the starand end depot and
	 * Alternative Fuel Stations will be created. Additionally 2 dummy 
	 * Alternative Fuel Stations will be created at the same locations 
	 * as the origin and end depot.
	 */
	public static void setDefaultNodes() {
		n = 4;
		f = 3;
		fStrich = 2;

		VStrich = new Node[15];
		// The start depot.
		VStrich[0] = new Node(3, 3, 0, 2000, new int[] { 0, 0, 0, 0 }, 5);

		// The pick up nodes.
		VStrich[1] = new Node(1, 1, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[2] = new Node(1, 4, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[3] = new Node(4, 3, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[4] = new Node(2, 4, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[5] = new Node(2, 1, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);		
//		VStrich[6] = new Node(5, 4, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[7] = new Node(5, 2, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[8] = new Node(2, 5, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);

		// The drop down nodes.
		VStrich[5] = new Node(4, 1, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[6] = new Node(4, 4, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[7] = new Node(1, 3, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[8] = new Node(3, 4, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[11] = new Node(3, 1, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);		
//		VStrich[12] = new Node(3, 5, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[14] = new Node(2, 3, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[16] = new Node(5, 3, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);

		// AFSs
		VStrich[9] = new Node(2, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		VStrich[10] = new Node(4, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		VStrich[11] = new Node(1, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);		
//		VStrich[14] = new Node(4, 5, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
//		VStrich[15] = new Node(1, 5, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		
		// The end depot.
		VStrich[12] = new Node(3, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 5);
		
		// dummy AFSs
		VStrich[13] = new Node(3, 3, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		VStrich[14] = new Node(3, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);	
//		VStrich[17] = new Node(4, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
//		VStrich[18] = new Node(1, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
//		VStrich[27] = new Node(2, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
//		VStrich[28] = new Node(4, 5, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
//		VStrich[29] = new Node(1, 5, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
	}
	
 /**
  * Automatically generate a random set of nodes.
  * @param numberOfNodes Number of Pick up nodes. (The same amount of drop down nodes are generated)
  * @param numberOfAFS Number of Alternative Fuel Stations that should be generated.
  */
	public static void autoGenerateNodes(int numberOfNodes, int numberOfAFS) {
		n = numberOfNodes;
		f = numberOfAFS;
		fStrich = 2;	// 2 Dummy Alternative Fuel Stations for the origin and end depot.

		VStrich = new Node[2 * n + f + fStrich + 1];
		
		// Start Node is the origin depot. Knoten hat keinen load.
		double xPositionOriginDepot = Math.random() * 4 + 1;
		double yPositionOriginDepot = Math.random() * 4 + 1;
		VStrich[0] = new Node(xPositionOriginDepot, yPositionOriginDepot, 0, 480, new int[] {0, 0, 0, 0}, 5);

		// Pick up nodes 1..n
		for (int i = 1; i <= n; i++) {
			VStrich[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, new int[] {1, 0, 0, 0}, 30);
		}

		// Drop down nodes n+1..2n
		for (int i = n + 1; i <= 2 * n; i++) {
			VStrich[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, new int[] {-1, 0, 0, 0}, 30);
		}
		
		// Create the Alternative Fuel Stations
		for (int i = 2 * n + 1; i <= 2 * n + 1 + f; i++) {
			VStrich[i] = new Node(Math.random() * 4 + 1, Math.random() * 4 + 1, 0, 480, new int[] {0, 0, 0, 0}, 15);
		}

		// Destination Node 2n+f+1. Knoten hat keinen load.
		double xPositionestinatioDepot = Math.random() * 4 + 1;
		double yPositioDestinationDepot = Math.random() * 4 + 1; 
		VStrich[2 * n + f + 1] = new Node(xPositionestinatioDepot, yPositioDestinationDepot, 0, 480, new int[] {0, 0, 0, 0}, 5);
		
		// Create the 2 two dummie AFS that are at the origin an destination depot.
		VStrich[2 * n + f + 2] = new Node(xPositionOriginDepot, yPositionOriginDepot, 0, 480, new int[] {0, 0, 0, 0}, 15);
		VStrich[2 * n + f + 3] = new Node(xPositionestinatioDepot, yPositioDestinationDepot, 0, 480, new int[] {0, 0, 0, 0}, 15);

	}

	private static void solveModel() {
		try {
			// Solve the model
			if (cplex.solve()) {
				// Print the result
				System.out.println();
				System.out.println("Solution status: " + cplex.getStatus());
				System.out.println("--------------------------------------------");
				System.out.println();
				System.out.println("Solution found:");
				System.out.println(" Objective value = " + cplex.getObjValue());
				System.out.println();

				for (int k = 0; k < K.length; k++) {
					System.out.println("Solution for Truck " + k + ":");
					for (int i = 0; i < VStrich.length; i++) {
						System.out.print("\t" + i);
					}
					System.out.println();
					for (int i = 0; i < VStrich.length; i++) {
						System.out.print(i + "\t");
						for (int j = 0; j < VStrich.length; j++) {
							if (i != j) {
								// Möglicherweise müssen Werte gerundet werden.
								if (Math.round(cplex.getValue(x[i][j][k])) == 0) {
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
					System.out.println();

					System.out.print("Route Truck " + k + ": ");
					int node = 0;
					int tempNode = 0;
					double distance = 0;
					do {
						if (node != 2 * n + f + 1) {
							System.out.print(node + " -> ");
						} else {
							System.out.print(node);
						}
						tempNode = node;
						node = getNextNode(node, k);
						if (tempNode != 2 * n + f + 1) {
							distance += c[tempNode][node];
						}
					} while (node != 0);
					System.out.println();
					
					
					System.out.println("Route duration for Truck " + k + ": "
							+ Math.round(cplex.getValue(B[2 * n + f + 1][k]) - cplex.getValue(B[0][k])) + " minutes.");
					System.out.println("Route distance for Truck " + k + ": " + distance + ".");
					System.out.println();
					
					
					System.out.println("Knoten\tx-Pos\ty-Pos\tTime\tSTime\tFuel");
					node = 0;
					do {
						System.out.print(node + "\t" + VStrich[node].getxPosition() + "\t");
						System.out.print(VStrich[node].getyPosition() + "\t");
						System.out.print(Math.round(cplex.getValue(B[node][k])) + "\t");
						System.out.print(VStrich[node].getServiceDuration() + "\t");
						System.out.println(cplex.getValue(z[node][k]));
						node = getNextNode(node, k);
					} while (node != 0);
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


