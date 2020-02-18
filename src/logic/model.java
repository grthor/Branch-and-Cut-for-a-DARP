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
	private static double Lmax = 360;

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
	 * Is calculated as distance * v
	 */
	private static double[][] t;

	/**
	 * Fuel Rate: 40 l / 100km constant fuel consumption 0.0003 liter/meter
	 */
	private static double FR = 0.0004;

	/**
	 * travel speed 80 km/h 0.00075 minutes/meter
	 */
	private static double v = 0.00075;

	public static void main(String[] args) {

		// Use a predefined set of trucks
		createTrucks();

		// Use a predefined set of nodes (definitely solvable)
		// 0 = no dummy AFS
		// 1 = same amount of dummy AFS as real AFS
		// 2 = twice the amount of dummy AFS as real AFS
		// ...
		createNodes(0);

		// Print node positions in the console for e.g. a visualization in excel.
		printNodesToConsole();

		// Calculate the distance and time between the nodes
		calculateDistanceAndTime();

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
							obj.addTerm(t[i][j] + VStrich[j].getServiceDuration(), x[i][j][k]);
						}
					}
				}
			}

			// Minimize the objective.
			cplex.addMinimize(obj);

			// Constraint 2: Visit every Pick up Location. (Serve every request exactly
			// once)
			for (int i = 1; i <= 2 * n; i++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
				}
				cplex.addEq(expr, 1.0, "Constraint2(i" + i + ")");
			}

			// Constraint 3. Visit every Dropdown location once. (Serve every request
			// exactly once)
			for (int j = 1; j <= 2 * n; j++) {
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
					// Orginal: Für alle j aus N führt zu unzulässigen Routen
//					for (int j = 1; j <= 2*n; j++) {
					// Abgeändert: Für alle j aus V' funktioniert.
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}

					// Orginal: Für alle j aus N führt zu unzulässigen Routen
//					for (int j = 1; j <= 2*n; j++) {
					// Abgeändert: Für alle j aus V' funktioniert.
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
						if (j != 2 * n + f + 1) {
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

			// Constraint 6: If an AFS is visited on a route, it has
			// at least one successor (nachfolger) node (client, AFS, depot).
//			for (int i = 2*n+1; i <= 2*n+f+fStrich+1; i++) {
//				// Nur AFS und dummy AFS, Zieldeepot ausnehmen.
//				if (i != 2*n+f+1) {
//					IloLinearNumExpr expr = cplex.linearNumExpr();
//					for (int k = 0; k < K.length; k++) {
//						for (int j = 0; j < VStrich.length; j++) {
//							if (i != j) {
//								expr.addTerm(1.0, x[i][j][k]);	
//							}
//						}
//					}
//					cplex.addLe(expr, 1.0, "Constraint6");
//				}
//			}

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

			// Constraint 8: End Route at destination depot.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int i = 0; i < VStrich.length; i++) {
					if (i != 2 * n + f + 1) {
						expr.addTerm(1.0, x[i][2 * n + f + 1][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint8");
			}

			// Definition Variable Q_ik: Load of vehicle k after visiting node i.
			Q = new IloNumVar[VStrich.length][4][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int r = 0; r <= 3; r++) {
					for (int k = 0; k < K.length; k++) {
						Q[i][r][k] = cplex.numVar(0, K[k].getContainerCapacity()[r],
								"Q(i" + i + ";r" + r + ";k" + k + ")");
					}
				}
			}

			// Constraint 9: Die geladenen Ressourcen auf LKW k müssen bei Knoten i
			// plus dem Load von Knoten i kleiner/gleich den geladenen Ressourcen
			// bei Knoten j sein.
			for (int k = 0; k < K.length; k++) {
				// Modell geändert: Es wird anstatt N=PuD V' genommen. Da die Beschränkung für
				// alle
				// Knoten gelten soll.
				for (int i = 0; i < VStrich.length; i++) {
					// Hier das selbe wie beim vorherigen Kommentar. Für Start- und Zielknoten
					// gilt diese Bedingung auch.
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							for (int r = 0; r <= 3; r++) {
								IloLinearNumExpr expr1 = cplex.linearNumExpr();
								expr1.addTerm(1.0, Q[i][r][k]);
								// 5 ist M
								expr1.setConstant(VStrich[j].getLoad()[r] + 5);
								expr1.addTerm(-5, x[i][j][k]);
								cplex.addLe(Q[j][r][k], expr1, "Contraint9a");

								IloLinearNumExpr expr2 = cplex.linearNumExpr();
								expr2.addTerm(1.0, Q[i][r][k]);
								// 5 ist M
								expr2.setConstant(VStrich[j].getLoad()[r] - 5);
								expr2.addTerm(5, x[i][j][k]);
								cplex.addGe(Q[j][r][k], expr2,
										"Constraint9b(i" + i + ";j" + j + ";k" + k + ";r" + r + ")");
							}
						}
					}
				}
			}

			// Constraint 10: impose capacity constraints
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					for (int r = 0; r <= 3; r++) {
						cplex.addLe(0.0, Q[i][r][k], "Constraint10_1");
						cplex.addLe(Q[i][r][k], K[k].getContainerCapacity()[r], "Constraint10_2");
					}
				}
			}

			// Constraint 11 Pesch: Leere und volle 30 Fuß Container dürfen die Kapazität
			// des LKWs
			// nicht überschreiten. Bsp. Ein LKW kann nicht 2 volle 30 Fuß Container und 2
			// leere 30 Fuß Container laden, da er nur Platz für insgesamt 2 Container hat.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, Q[i][0][k]);
					expr.addTerm(1.0, Q[i][1][k]);
					cplex.addLe(expr, K[k].getContainerCapacity()[0], "Constraint11");
				}
			}

			// Constraint 12 Pesch: Leere und volle 60 Fuß Container zusammen dürfen die
			// Kapazität
			// des LKWs nicht übeschreiten. Bsp.: Es kann nicht ein voller und ein leerer
			// 60"
			// Container gleichzeitig geladen sein.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, Q[i][2][k]);
					expr.addTerm(1.0, Q[i][3][k]);
					cplex.addLe(expr, K[k].getContainerCapacity()[2], "Constraint12");
				}
			}

			// Constraint 13: Start a route without containers
			for (int k = 0; k < K.length; k++) {
				for (int r = 0; r <= 3; r++) {
					cplex.addEq(Q[0][r][k], 0.0, "Constraint13");
				}
			}

			// Constraint 14: End routes without containers
			for (int k = 0; k < K.length; k++) {
				for (int r = 0; r <= 3; r++) {
					cplex.addEq(Q[2 * n + f + 1][r][k], 0.0, "Constraint14");
				}
			}

			// Kontinuirliche Variable B_ik für die Zeit, an der Truck seinen
			// Service an Knoten i beginnt.
			B = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					B[i][k] = cplex.numVar(VStrich[i].getEarliestServiceTime(), VStrich[i].getLatestServiceTime(),
							"ServiceTimeB(i" + i + ";k" + k + ")");
				}
			}

			// Definition l_i^k: The ride time of user i on vehicle k.
			l = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					l[i][k] = cplex.numVar(0, Tmax, "l(i" + i + ";k" + k + ")");
				}
			}

			// Constraint 15: Precedence Relation with Constraints 17 and 18.
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[i][k]);
					expr.setConstant(t[i][i + n]);
					cplex.addGe(B[n + i][k], expr, "Constraint15");
				}
			}

			// Constraint 16: Set the ride time of each user.
			// Ride time of user i in vehicle k (L_i^k)
			// ist gleich Ride Time of user i + n minus (Ride time in
			// i plus service time in i).
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[n + i][k]);
					expr.addTerm(-1.0, B[i][k]);
					expr.setConstant(-VStrich[i].getServiceDuration());
					cplex.addEq(l[i][k], expr, "Constraint17");
				}
			}

			// Constraint 17: Ride time jedes Users muss größer als
			// die Fahrzeit von Knoten i nach Knoten j sein und kleiner als
			// der maximal erlaubte Fahrzeit.
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					cplex.addLe(t[i][n + i], l[i][k], "Constraint17_1");
					cplex.addLe(l[i][k], Lmax, "Constraint17_2");
				}
			}

			// Constraint 18: Der Service an Knoten j kann erst beginnen,
			// nachdem der Service an Knoten i abgeschlossen wurde und der
			// LKW von i nach j gefahren ist.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							IloLinearNumExpr expr1 = cplex.linearNumExpr();
							expr1.addTerm(1.0, B[i][k]);
							expr1.setConstant(t[i][j] + VStrich[i].getServiceDuration() + Tmax);
							expr1.addTerm(-Tmax, x[i][j][k]);
							cplex.addLe(B[j][k], expr1, "Constraint18a(k" + k + ";i" + i + ";j" + j + ")");

							IloLinearNumExpr expr2 = cplex.linearNumExpr();
							expr2.addTerm(1.0, B[i][k]);
							expr2.setConstant(t[i][j] + VStrich[i].getServiceDuration() - Tmax);
							expr2.addTerm(Tmax, x[i][j][k]);
							cplex.addGe(B[j][k], expr2, "Constraint18b(k" + k + ";i" + i + ";j" + j + ")");
						}
					}
				}
			}

			// Constraint 19 Pesch: Knoten müssen innerhalb ihrer Servicezeit besucht
			// werden.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					cplex.addLe(VStrich[i].getEarliestServiceTime(), B[i][k], "Constraint19_1");
					cplex.addLe(B[i][k], VStrich[i].getLatestServiceTime(), "Constraint19_2");
				}
			}

			// Constraint 20: Dauer einer Tour darf die maximale Fahrzeit Tmax
			// eines LKWs nicht überschreiten.
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0, B[2 * n + f + 1][k]);
				expr.addTerm(-1.0, B[0][k]);
				cplex.addLe(expr, Tmax, "Constraint21");
			}

			z = new IloNumVar[VStrich.length][K.length];
			for (int i = 0; i < VStrich.length; i++) {
				for (int k = 0; k < K.length; k++) {
					z[i][k] = cplex.numVar(0, K[k].getTankCapacity(), "z(i" + i + ";k" + k + ")");
				}
			}

			// Constraint 22 Erdogan 10: Reduce Fuel based on traveled distance
			// Fahrten zu einer normalen Knoten (Start/Ziel, Kunden)
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					// Für alle PuDu{0,2n+f+1} (Alle Knoten außer die AFSs und dummy AFSs)
					for (int j = 0; j <= 2 * n + f + 1; j++) {
						if (j < 2 * n + 1 || j > 2 * n + f) {
							if (i != j) {
								IloLinearNumExpr expr = cplex.linearNumExpr();
								expr.addTerm(1.0, z[i][k]);
								expr.addTerm(-FR * c[i][j], x[i][j][k]);
								expr.setConstant(K[k].getTankCapacity());
								expr.addTerm(-K[k].getTankCapacity(), x[i][j][k]);
								cplex.addLe(z[j][k], expr, "Constraint22Erdogan10");
							}
						}
					}
				}
			}

			// Constraint 23 Erdogan 11: Set fuel level on max. fuel level when visiting an
			// AFS.
			// Fahrten zu AFSs und dummy AFS.
			for (int j = 2 * n + 1; j <= 2 * n + f + fStrich + 1; j++) {
				// Um alle AFSs, auch die dummy AFSs, muss j von 2*n+1 bis 2*n+f+fStrich+1 gehen
				// und das Zieldepot 2*n+f+1 muss ausgenommen sein.
				if (j != 2 * n + f + 1) {
					for (int k = 0; k < K.length; k++) {
						// Original
						cplex.addEq(z[j][k], K[k].getTankCapacity(), "Constraint23Erdogan11");

					}
				}
			}

			// Constraint 25: Stellt sicher, dass der Sprit bis zum nächsten Knoten
			// reicht. Gleichzeitig Verbesserung von Constraint 24: Sprit fällt vor AFS
			// nicht mehr unter 0.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < VStrich.length; i++) {
					for (int j = 0; j < VStrich.length; j++) {
						if (i != j) {
							IloLinearNumExpr expr = cplex.linearNumExpr();
							expr.addTerm(c[i][j] * FR, x[i][j][k]);
							cplex.addGe(z[i][k], expr, "Constraint25");
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
	 * Calculates the distance between the coordinates of the Nodes und time
	 * required to drive from one node to another.
	 */
	private static void calculateDistanceAndTime() {
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

					xDistance = Math.pow(111.3 * (VStrich[i].getlatitude() - VStrich[j].getlatitude()), 2);
					yDistance = Math.pow(71.5 * (VStrich[i].getlongtitude() - VStrich[j].getlongtitude()), 2);
					c[i][j] = Math.sqrt(xDistance + yDistance) * 1000;

					// The travel time tij (in minutes) between a node i and node j is the distance
					// cij * v
					t[i][j] = c[i][j] * v;
				}
			}
		}
	}

	/**
	 * Creates a default set of Trucks.
	 */
	private static void createTrucks() {
		// Alle Trucks müssen die selben Container transportieren können.
		K = new Truck[3];

		// Truck mit Kapzität für einen 40 FUß Container und Tankkapazität von 60 liter
		K[0] = new Truck(new int[] { 2, 2, 0, 0 }, 60);
		// Truck mit Kapzität für einen 20 FUß Container und Tankkapazität von 75 liter
		K[1] = new Truck(new int[] { 2, 2, 0, 0 }, 60);
		// Truck mit Kapzität für zwei 20 FUß Container und Tankkapazität von 60 liter
		K[2] = new Truck(new int[] { 2, 2, 0, 0 }, 60);
		// Truck mit Kapzität für einen 40 FUß Container und Tankkapazität von 60 liter
//		K[3] = new Truck(new int[] { 2, 2, 0, 0 }, 60);
		// Truck mit Kapzität für einen 20 FUß Container und Tankkapazität von 75 liter
//		K[4] = new Truck(new int[] { 2, 2, 0, 0 }, 75);
		// Truck mit Kapzität für zwei 20 FUß Container und Tankkapazität von 60 liter
//		K[5] = new Truck(new int[] { 2, 2, 0, 0 }, 60);
	}

	/**
	 * Erzeugt Start- und Zieldepot, Pick-up Nodes, Dropdown Nodes, AFS und dummy AFSs.
	 */
	public static void createNodes(int amountOfDummyNodes) {
		n = 7;
		f = 6;

		fStrich = f * amountOfDummyNodes;
		VStrich = new Node[2 + 2 * n + f + fStrich];

		// The start depot.
		VStrich[0] = new Node("Start Düsseldorf", 51.225402, 6.776314, 0, Tmax, new int[] { 0, 0, 0, 0 }, 1);

		// Pick up nodes.
		VStrich[1] = new Node("1 Düsseldorf", 51.225402, 6.776314, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[2] = new Node("2 Leverkusen", 51.032474, 6.988119, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[3] = new Node("3 Duisburg", 51.434999, 6.759562, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[4] = new Node("4 Bonn", 50.735851, 7.100660, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[5] = new Node("5 Grevenbroich", 51.090578, 6.583537, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[6] = new Node("6 Grefrath", 51.336077, 6.343738, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
		VStrich[7] = new Node("7 Siegen", 50.874980, 8.022723, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[8] = new Node("8 Lüdenscheid", 51.218116, 7.639551, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[9] = new Node("9 Olpe", 51.512054, 7.463573, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[10] = new Node("10 Neuss", 51.198178, 6.691648, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[11] = new Node("11 Mönchengladbach", 51.194698, 6.435364, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[12] = new Node("12 Hennef", 50.775442, 7.284795, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[13] = new Node("13 Plettenberg", 51.213680, 7.874563, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[14] = new Node("14 Frechen", 50.909622,6.808194, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);
//		VStrich[15] = new Node("15 Mettmann", 51.252778, 6.977778, 0, Tmax, new int[] { 1, 0, 0, 0 }, 30);

		// Drop down nodes.
		VStrich[n + 1] = new Node("1+n Gummersbach", 51.027766, 7.563055, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[n + 2] = new Node("2+n Wuppertal", 51.264018, 7.178037, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[n + 3] = new Node("3+n Wuppertal", 51.264018, 7.178037, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[n + 4] = new Node("4+n Bergisch Gladbach", 50.992930, 7.127738, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[n + 5] = new Node("5+n Ratingen", 51.297326, 6.849350, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[n + 6] = new Node("6+n Düren", 50.804340, 6.492990, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		VStrich[n + 7] = new Node("7+n Hagen", 51.358295, 7.473296, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 8] = new Node("8+n Essen", 51.457087, 7.011429, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 9] = new Node("9+n Dortmund", 51.514227, 7.465279, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 10] = new Node("10+n Moers", 51.451283, 6.628430, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 11] = new Node("11+n Bottrop", 51.521581, 6.929204, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 12] = new Node("12+n Windeck", 50.790421, 7.582751, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 13] = new Node("13+n Bochum", 51.481804, 7.219660, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 14] = new Node("14+n Erkelenz", 51.080992, 6.316068, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
//		VStrich[n + 15] = new Node("15+n Willich", 51.264143, 6.544696, 0, Tmax, new int[] { -1, 0, 0, 0 }, 30);
		
		// AFSs
		VStrich[2 * n + 1] = new Node("AFS Startdepot", 51.225402, 6.776314, 0, Tmax, new int[] { 0, 0, 0, 0 }, 60);
		VStrich[2 * n + 2] = new Node("AFS Zieldepot", 51.032474, 6.988119, 0, Tmax, new int[] { 0, 0, 0, 0 }, 60);
		VStrich[2 * n + 3] = new Node("AFS Iserlohn", 51.374678, 7.699971, 0, Tmax, new int[] { 0, 0, 0, 0 }, 60);
		VStrich[2 * n + 4] = new Node("AFS Brühl", 50.828714, 6.903295, 0, Tmax, new int[] { 0, 0, 0, 0 }, 60);
		VStrich[2 * n + 5] = new Node("AFS Krefeld", 51.333121, 6.562334, 0, Tmax, new int[] { 0, 0, 0, 0 }, 60);
		VStrich[2 * n + 6] = new Node("AFS Porz", 50.879923, 7.095075, 0, Tmax, new int[] { 0, 0, 0, 0 }, 60);

		// The end depot.
		VStrich[2 * n + f + 1] = new Node("Ziel Leverkusen", 51.032474, 6.988119, 0, Tmax, new int[] { 0, 0, 0, 0 }, 1);

		// Dummy AFSs
		int dummyIndex = 2 * n + f + 2;
		for (int i = 0; i < amountOfDummyNodes; i++) {
			for (int j = 2 * n + 1; j <= 2 * n + f; j++) {
				VStrich[dummyIndex] = new Node(VStrich[j].getName() + i, VStrich[j].getlatitude(),
						VStrich[j].getlongtitude(), VStrich[j].getEarliestServiceTime(),
						VStrich[j].getLatestServiceTime(), VStrich[j].getLoad(), VStrich[j].getServiceDuration());
				dummyIndex++;
			}
		}
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
					System.out.print("Route for Truck " + k + ": ");
					int node = 0;
					int tempNode = 0;
					double distance = 0;
					do {
						if (node != 2 * n + f + 1) {
							System.out.print(VStrich[node].getName() + " -> ");
						} else {
							System.out.print(VStrich[node].getName());
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

					System.out.println("Knoten\t\tx-Pos\ty-Pos\tTime\tSTime\tDTime\tFuel");
					int previousNode = 0;
					node = 0;
					do {
						previousNode = node;
						node = getNextNode(node, k);
						System.out.print(VStrich[previousNode].getName());
						if (VStrich[previousNode].getName().length() < 8) {
							System.out.print("\t\t");
						} else if (VStrich[previousNode].getName().length() < 16) {
							System.out.print("\t");
						}
						System.out.print((int) VStrich[previousNode].getlatitude() + "\t");
						System.out.print((int) VStrich[previousNode].getlongtitude() + "\t");
						System.out.print((double) Math.round(cplex.getValue(B[previousNode][k]) * 100) / 100 + "\t");
						System.out.print(VStrich[previousNode].getServiceDuration() + "\t");

						if (node != 0) {
							System.out.print((double) Math.round(t[previousNode][node] * 100) / 100 + "\t");
						} else {
							System.out.print("\t");
						}
						System.out.println((double) Math.round(cplex.getValue(z[previousNode][k]) * 100) / 100);
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

	/**
	 * Method to look up the route of a vehicle.
	 * 
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
	 * Print all Nodes in the console as comma separated values for an easy
	 * presentation in eg Google Maps.
	 */
	private static void printNodesToConsole() {
		System.out.println("Start/Zieldepot");
		System.out.println("latitude,longitude,name");
		System.out.println(VStrich[0].getlatitude() + "," + VStrich[0].getlongtitude() + "," + VStrich[0].getName());
		System.out.println(VStrich[2 * n + f + 1].getlatitude() + "," + VStrich[2 * n + f + 1].getlongtitude() + ","
				+ VStrich[2 * n + f + 1].getName());
		System.out.println();

		System.out.println("Pick up Knoten");
		System.out.println("latitude,longitude,name");
		for (int i = 1; i <= n; i++) {
			System.out
					.println(VStrich[i].getlatitude() + "," + VStrich[i].getlongtitude() + "," + VStrich[i].getName());
		}
		System.out.println();

		System.out.println("Drop down Knoten");
		System.out.println("latitude,longitude,name");
		for (int i = n + 1; i <= 2 * n; i++) {
			System.out
					.println(VStrich[i].getlatitude() + "," + VStrich[i].getlongtitude() + "," + VStrich[i].getName());
		}
		System.out.println();

		System.out.println("AFS");
		System.out.println("latitude,longitude,name");
		for (int i = 2 * n + 1; i <= 2 * n + f; i++) {
			System.out
					.println(VStrich[i].getlatitude() + "," + VStrich[i].getlongtitude() + "," + VStrich[i].getName());
		}
		System.out.println();
	}
}
