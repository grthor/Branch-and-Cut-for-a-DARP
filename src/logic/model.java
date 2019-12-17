package logic;

import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;
import ilog.concert.*;

public class model {

	/**
	 * Number of Pick up nodes
	 */
	private static int n;
	
	/**
	 * Number of AFSs
	 */
	private static int f;
	private static IloCplex cplex;
	private static IloNumVar[][][] x;
	private static Node[] V;
	private static Truck[] K;
	private static double[][] c;

	private static IloNumVar[][] B;

	public static void main(String[] args) {

		// Uncomment if you want to automatically create Nodes
//		autoGenerateNodes(5);

		// Generate a predefined set of nodes.
		setDefaultNodes();

		// Print Node Positions for Excel.
//		System.out.println("Knoten\txPosition\tyPosition");
//		for (int i = 0; i <= 2*n+1; i++) {
//			System.out.println(i + "\t" + N[i].getxPosition() + "\t" + N[i].getyPosition());
//		}

		// Alle Trucks müssen die selben Container transportieren können.
		K = new Truck[2];
		K[0] = new Truck(new int[] { 1, 0, 0, 0 }, 6000, 5);
		K[1] = new Truck(new int[] { 1, 0, 0, 0 }, 1400, 10);
//		K[2] = new Truck(new int[] { 1, 0, 0, 0 }, 1000, 10); // Mit einem Truck ohne Kapazität (capacity = 0) gibt es Bound
															// infeasibility column 'Q(i1;k2)'.

		// c enthält die Distanz zwischen allen Knoten
		c = new double[V.length][V.length];
		// t enthält die Fahrzeit zwischen allen Knoten.
		double[][] t = new double[V.length][V.length];

		double xDistance;
		double yDistance;

		for (int i = 0; i < V.length; i++) {
			for (int j = 0; j < V.length; j++) {
				if (i != j) {
					xDistance = Math.pow(V[i].getxPosition() - V[j].getxPosition(), 2);
					yDistance = Math.pow(V[i].getyPosition() - V[j].getyPosition(), 2);
					c[i][j] = Math.sqrt(xDistance + yDistance);

					// Die Fahrzeit zwischen i und j ist die Entfernung zwischen den Knoten * 60.
					t[i][j] = c[i][j] * 60;
				}
			}
		}

		try {
			cplex = new IloCplex();

			// Constraint 26 Pesch:Binary decision variable.
			// Constraint 14 Cordeau
			x = new IloNumVar[V.length][V.length][K.length];
			for (int i = 0; i < V.length; i++) {
				for (int j = 0; j < V.length; j++) {
					if (i != j) {
						for (int k = 0; k < K.length; k++) {
							x[i][j][k] = cplex.boolVar("x(" + "i" + i + ";j" + j + ";k" + k + ")");
						}
					}
				}
			}

			// Zielfunktion
			IloLinearNumExpr obj = cplex.linearNumExpr();
			for (int i = 0; i < V.length; i++) {
				for (int j = 0; j < V.length; j++) {
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
			// Funktioniert!
			// Cordeau geht von 1..n, Pesch von 1..2n
			// Von 1..n ist schneller aus von 1..2n.
			// Beides funktioniert.
			for (int i = 1; i <= 2*n; i++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int j = 0; j < V.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
				}
				cplex.addEq(expr, 1.0, "Constraint2Pesch");
			}
			
			// Constraint 3 Pesch: Es darf nur eine Kante in einen pick up oder dropdown Node führen.
			for (int j = 1; j <= 2*n; j++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int k = 0; k < K.length; k++) {
					for (int i = 0; i < V.length; i++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
				}
				cplex.addEq(1.0, expr, "Constraint3Pesch");
			}

			// Constraint 4 Pesch: visit pickup and dropdown depot by the same vehicle.
			// Constraint 3 Cordeau
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					for (int j = 0; j < V.length; j++) {
						if (i != j) {
							expr.addTerm(1.0, x[i][j][k]);
						}
					}
					
					for (int j = 0; j < V.length; j++) {
						if (n+i != j)
							expr.addTerm(-1.0, x[n + i][j][k]);
					}
					cplex.addEq(expr, 0.0, "Constraint4Pesch");
				}
			}

			// Constraint 5 Pesch: Flow constraint: Every Node from P union D (1..2*n)
			// must have the same amount of edges going and edges going out.
			// The Nodes 0 and 2n+1 (7) are not covered by this constraint, because
			// the route should start/end there.
			// Constraint 5 Cordeau
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					// Start und Zieldepot sind vom Flow Constraint ausgenommen.
					if (i != 0) {
						// Start und Zieldepot sind vom Flow Constraint ausgenommen.
						if (i != 2*n+f+1) {
							IloLinearNumExpr expr = cplex.linearNumExpr();
							for (int j = 0; j < V.length; j++) {
								if (i != j) {
									expr.addTerm(1.0, x[j][i][k]);
								}
							}
							for (int j = 0; j < V.length; j++) {
								if (i != j) {
									expr.addTerm(-1.0, x[i][j][k]);
								}
							}
							cplex.addEq(expr, 0.0, "Constraint5Pesch");
						}
					}
				}
			}
			
			// Constraint 7 Pesch: Start route at the origin depot.
			// Constraint 4 Cordeau
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int j = 0; j < V.length; j++) {
					if (j != 0) {
						expr.addTerm(1.0, x[0][j][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint7Pesch");
			}

			// Constraint 9 Pesch: End Route at destination depot.
			// Constraint 6 Cordeau
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (int i = 0; i < V.length; i++) {
					if (i != 2 * n + f + 1) {
						expr.addTerm(1.0, x[i][2 * n + f + 1][k]);
					}
				}
				cplex.addEq(expr, 1.0, "Constraint9Pesch");
			}
			
			// Definition Variable Q_ik: Load of vehicle k after visiting node i.
			IloNumVar[][][] Q = new IloNumVar[V.length][4][K.length];
			for (int i = 0; i < V.length; i++) {
				for (int r = 0; r <= 3; r++) {
					for (int k = 0; k < K.length; k++) {
						Q[i][r][k] = cplex.numVar(0, K[k].getCapacity()[r], "Q(i" + i + ";r" + r + ";k" + k + ")");
					}
				}
			}

			// Constraint 10 Pesch: Die geladenen Ressourcen auf LKW k müssen bei Knoten i
			// plus dem Load von Knoten i kleiner/gleich den geladenen Ressourcen
			// bei Knoten j sein.
			// Version von Cordeau: Ist schneller als die Version von Pesch.
			// Liefert das selbe Ergebnis wie Pesch.
			// Constraint 8 Cordeau
			//Cordeau
//			double W;
//			for (int i = 0; i < V.length; i++) {
//				for (int j = 0; j < V.length; j++) {
//					if (i != j) {
//						for (int k = 0; k < K.length; k++) {
//							for (int r = 0; r <= 3; r++) {
//								W = Math.min(K[k].getCapacity()[r], K[k].getCapacity()[r] + V[i].getLoad()[r]);
//								IloLinearNumExpr expr = cplex.linearNumExpr();
//								expr.addTerm(1.0, Q[i][r][k]);
//								expr.setConstant(V[j].getLoad()[r] - W);
//								expr.addTerm(W, x[i][j][k]);
//								cplex.addGe(Q[j][r][k], expr, "Constraint8(i" + i + ";j" + j + ";k" + k + ";r" + r + ")Cordeau");
//							}
//						}
//					}
//				}
//			}

			// Version von Pesch: Ist langsamer als die Version von Cordeau.
			// Liefert das selbe Ergebnis wie Pesch.
			for (int k = 0; k < K.length; k++) {
			//Modell geändert: Im Original wird anstatt N=PuD N=PuDu{0, 2n+1} genommen.
			//Start und Zieldepot sind in dieser Variante inbegriffen.
				for (int i = 0; i < V.length; i++) {
				// Hier das selbe wie beim vorherigen Kommentar. Für Start- und Zielknoten 
				// gilt diese Bedingung auch.
					for (int j = 0; j < V.length; j++) {
						if (i != j) {
							for (int r = 0; r <= 3; r++) {
								IloLinearNumExpr expr1 = cplex.linearNumExpr();
								expr1.addTerm(1.0, Q[i][r][k]);
								expr1.setConstant(V[j].getLoad()[r] + K[k].getCapacity()[r]);
								expr1.addTerm(-K[k].getCapacity()[r], x[i][j][k]);
								cplex.addLe(Q[j][r][k], expr1, "Contraint10a(k" + k + ";i" + i + ";j" + j + ";r" + r + ")Pesch");
								
								IloLinearNumExpr expr2 = cplex.linearNumExpr();
								expr2.addTerm(1.0, Q[i][r][k]);
								expr2.setConstant(V[j].getLoad()[r] - K[k].getCapacity()[r]);
								expr2.addTerm(K[k].getCapacity()[r], x[i][j][k]);
								cplex.addGe(Q[j][r][k], expr2, "Constraint10b(k" + k + ";i" + i + ";j" + j + ";r" + r + ")Pesch");
							}
						}
					}
				}
			}
			
			// Constraint 11 Pesch: impose capacity constraint
			// Constraint 12 Cordeau
			// Cordeau
//			for (int i = 0; i < N.length; i++) {
//				for (int k = 0; k < K.length; k++) {
//					for (int r = 0; r <= 3; r++) {
//						cplex.addLe(Math.max(0, N[i].getLoad()[r]), Q[i][r][k], "Constraint13_1Cordeau");
//						cplex.addLe(Q[i][r][k],
//								Math.min(K[k].getCapacity()[r], K[k].getCapacity()[r] + N[i].getLoad()[r]),
//								"Constraint13_2Cordeau");
//					}
//				}
//			}

			// Pesch
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					for (int r = 0; r <= 3; r++) {
						cplex.addLe(0.0, Q[i][r][k], "Constraint11_1Pesch");
						cplex.addLe(Q[i][r][k], K[k].getCapacity()[r], "Constraint11_2Pesch");
					}
				}
			}
			
			// Constraint 12 Pesch: Leere und volle 30 Fuß Container dürfen die Kapazität des LKWs
			// nicht überschreiten. Bsp. Ein LKW kann nicht 2 volle 30 Fuß Container und 2 
			// leere 30 Fuß Container laden, da er nur Platz für insgesamt 2 Container hat.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, Q[i][0][k]);
					expr.addTerm(1.0, Q[i][1][k]);
					cplex.addLe(expr, K[k].getCapacity()[0], "Constraint12Pesch");
				}
			}

			// Constraint 13 Pesch: Leere und volle 60 Fuß Container zusammen dürfen die Kapazität
			// des LKWs nicht übeschreiten. Bsp.: Es kann nicht ein voller und ein leerer 60"
			// Container gleichzeitig geladen sein.
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, Q[i][2][k]);
					expr.addTerm(1.0, Q[i][3][k]);
					cplex.addLe(expr, K[k].getCapacity()[2], "Constraint13Pesch");
				}
			}
			
			// Constraint 14 Pesch: Start with empty
			// Constraint 14 und 15 haben den Algorithmus doppelt so schnell gemacht.
			// Beide Constraints sind aber nicht notwendig.
			// Pesch
			for (int k = 0; k < K.length; k++) {
				for (int r = 0; r <= 3; r++) {
					cplex.addEq(Q[0][r][k], 0.0, "Constraint14Pesch");
				}
			}

			// Constraint 15 Pesch: Route beenden ohne container.
			// Constraint 14 und 15 haben den Algorithmus doppelt so schnell gemacht.
			// Beide Constraints sind aber nicht notwendig.
			// Pesch
			for (int k = 0; k < K.length; k++) {
				for (int r = 0; r <= 3; r++) {
					cplex.addEq(Q[2*n+f+1][r][k], 0.0, "Constraint15Pesch");
				}
			}
			
			
			

			// Kontinuirliche Variable B_ik für die Zeit, an der Truck seinen
			// Service an Knoten i beginnt.
			B = new IloNumVar[V.length][K.length];
			for (int i = 0; i < V.length; i++) {
				for (int k = 0; k < K.length; k++) {
					B[i][k] = cplex.numVar(V[i].getBeginServiceTime(), V[i].getEndServiceTime(), "ServiceTimeB(i" + i + ";k" + k + ")");
				}
			}

			// Maximum ride time of a user: For example 180 Minutes.
			double lMaxRideTime = 360;

			// Definition l_i^k: The ride time of user i on vehicle k.
			IloNumVar[][] l = new IloNumVar[V.length][K.length];
			for (int i = 1; i <= n; i++) {
				for (int k = 0; k < K.length; k++) {
					l[i][k] = cplex.numVar(0, lMaxRideTime, "l(i" + i + ";k" + k + ")");
				}
			}
			
			// Constraint 16 Pesch: Presedence Relation with Constraits 17 and 18.
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[i][k]);
					expr.setConstant(t[i][i+n]);
					cplex.addGe(B[n+i][k], expr, "Constraint16Pesch");
				}
			}
			

			// Constraint 17 Pesch: Set the ride time of each user.
			// Ride time of user i in vehicle k (L_i^k)
			// ist gleich Ride Time of user i + n minus (Ride time in
			// i plus service time in i).
			// Constraint 9 Cordeau
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					IloLinearNumExpr expr = cplex.linearNumExpr();
					expr.addTerm(1.0, B[n + i][k]);
					expr.addTerm(-1.0, B[i][k]);
					expr.setConstant(-V[i].getServiceDuration());
					cplex.addEq(l[i][k], expr, "Constraint17Pesch");
				}
			}
			
			// Constraint 18 Pesch: Ride time jedes Users muss größer als
			// die Fahrzeit von Knoten i nach Knoten j sein und kleiner als
			// der maximal erlaubte Fahrzeit.
			// Constraint 12 Cordeau
			for (int k = 0; k < K.length; k++) {
				for (int i = 1; i <= n; i++) {
					cplex.addLe(t[i][n + i], l[i][k], "Constraint18_1Pesch");
					cplex.addLe(l[i][k], lMaxRideTime, "Constraint18_2Pesch");
				}
			}
			
			// Constraint 19 Pesch: Der Service an Knoten j kann erst beginnen,
			// nachdem der Service an Knoten i abgeschlossen wurde und der
			// LKW von i nach j gefahren ist.
			// Constraint 7 Cordeau
			//Cordeau
//			double M;
//			for (int i = 0; i < V.length; i++) {
//				for (int j = 0; j < V.length; j++) {
//					if (i != j) {
//						for (int k = 0; k < K.length; k++) {
//							IloLinearNumExpr expr = cplex.linearNumExpr();
//							M = Math.max(0, V[i].getEndServiceTime() + V[i].getServiceDuration() + t[i][j]
//									- V[j].getBeginServiceTime());
//							expr.addTerm(1.0, B[i][k]);
//							expr.setConstant(V[i].getServiceDuration() + t[i][j] - M);
//							expr.addTerm(M, x[i][j][k]);
//							cplex.addGe(B[j][k], expr, "Constraint7Cordeau");
//						}
//					}
//				}
//			}
			
			//Pesch
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					for (int j = 0; j < V.length; j++) {
						if (i != j) {
							IloLinearNumExpr expr1 = cplex.linearNumExpr();
							expr1.addTerm(1.0, B[i][k]);
							// 10000 represents Tmax.
							expr1.setConstant(t[i][j] + V[i].getServiceDuration() + 10000);
							expr1.addTerm(-10000, x[i][j][k]);
							cplex.addLe(B[j][k], expr1, "Constraint19aPesch");
							
							IloLinearNumExpr expr2 = cplex.linearNumExpr();
							expr2.addTerm(1.0, B[i][k]);
//							expr2.addTerm(t[i][j] + V[i].getServiceDuration(), x[i][j][k]);
							expr2.setConstant(-10000 + t[i][j] + V[i].getServiceDuration());
							expr2.addTerm(10000, x[i][j][k]);
							cplex.addGe(B[j][k], expr2, "Constraint19bPesch");
						}
					}
				}
			}
			
			// Constraint 20 Pesch: Knoten müssen innerhalb ihrer Servicezeit besucht werden.
			// Constraint 11 Cordeau
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					cplex.addLe(V[i].getBeginServiceTime(), B[i][k], "Constraint20_1Pesch");
					cplex.addLe(B[i][k], V[i].getEndServiceTime(), "Constraint20_2Pesch");
				}
			}
			
			// Constraint 21 Pesch: Dauer einer Tour darf die maximale
			// Tourzeit eines LKWs nicht überschreiten.
			// Constraint 10 Cordeau
			for (int k = 0; k < K.length; k++) {
				IloLinearNumExpr expr = cplex.linearNumExpr();
				expr.addTerm(1.0, B[2 * n + f + 1][k]);
				expr.addTerm(-1.0, B[0][k]);
				cplex.addLe(expr, K[k].getMaxTourTime(), "Constraint21Pesch");
			}
			
			
			IloNumVar[][] z = new IloNumVar[V.length][K.length];
			for (int i = 0; i < V.length; i++) {
				for (int k = 0; k < K.length; k++) {
					z[i][k] = cplex.numVar(0, K[k].getFuelCapacity(), "z(i" + i + ";k" + k + ")");
				}
			}
			
			
//			// Constraint 22 Pesch : Fuel Capacity verringert sich mit jedem besuchten Knoten.
//			// FIXME: Funktioniert nicht. Im ersten Versuch wurde der Spritverbrauch nicht reduziert.
//			for (int k = 0; k < K.length; k++) {
//				for (int i = 0; i < V.length; i++) {
//					for (int j = 0; j < V.length; j++) {
//						if (i != j) {
//							IloLinearNumExpr expr = cplex.linearNumExpr();
//							expr.setConstant(K[k].getFuelCapacity());
//							expr.addTerm(-K[k].getFuelCapacity(), x[i][j][k]);
//							expr.addTerm(1.0, z[i][k]);
//							expr.addTerm(1.0, z[j][k]);
//							//FR ist 1.
//							expr.addTerm(-c[i][j], x[i][j][k]);
//							cplex.addGe(expr, 0.0, "Constraint22Pesch");
//						}
//					}
//				}
//			}
//			
//			
//			//Constraint 23 Pesch: Guarantee that remaining fuel is enough to reach an AFS.
//			for (int k = 0; k < K.length; k++) {
//				for (int i = 0; i <= 2*n; i++) {
//					for (int j = 2*n+1; j < 2*n+f; j++) {
//						if (i != j) {
//							IloLinearNumExpr expr = cplex.linearNumExpr();
//							expr.setConstant(K[k].getFuelCapacity());
//							expr.addTerm(-K[k].getFuelCapacity(), x[i][j][k]);
//							expr.addTerm(1.0, z[i][k]);
//							expr.addTerm(-1.0, z[j][k]);
//							expr.addTerm(-c[i][j], x[i][j][k]);
//							cplex.addGe(expr, 0.0, "Constraint23Pesch");
//						}
//					}
//				}
//			}
//			
//			// Constraint 24 Pesch: Set z to fuelCapacity of the vehicle.
//			// Eigene Linearisierung
//			for (int i = 0; i < V.length; i++) {
//				for (int j = 2*n+1; j <= 2*n+f; j++) {
//					if (i != j) {
//						for (int k = 0; k < K.length; k++) {
//							IloLinearNumExpr expr = cplex.linearNumExpr();
//							expr.addTerm(1.0, z[j][k]);
//							expr.setConstant(-K[k].getFuelCapacity());
//							expr.addTerm(K[k].getFuelCapacity(), x[i][j][k]);
//							cplex.addLe(expr, 15.0, "Constraint24Pesch");
//						}
//					}
//				}
//			}
//			
//			// Constraint 25 Pesch: Set Fuel level on start depot to fuelCapacity.
//			// Geht das überhaupt oder muss dort x = 1 => Constraint, wie bei Constraint 24.
//			// Das ist eine sinnlose Angelegenheit, da eine Route maximal einmal (nämlich beim Start)
//			// beim Startdepot vorbeikommt. Tanken am Startdepot ist also nicht.
//			for (int k = 0; k < K.length; k++) {
//				cplex.addEq(z[0][k], K[k].getFuelCapacity(), "Constraint25Pesch");
//			}
//			
//			// Constraint 26 Groos: Set Fuel level on destination depot to fuelCapacity.
//			// Geht das überhaupt oder muss dort x = 1 => Constraint, wie bei Constraint 24.
//			// Genauso wie Constraint 25 ist dieser hier sinnlose, da eine Route genau einmal
//			// beim Zieldepot vorbeikommt. Tanken am Zieldepot ist also ausgeschlossen.
//			for (int k = 0; k < K.length; k++) {
//				cplex.addEq(z[2*n+f+1][k], K[k].getFuelCapacity(), "Constraint26Groos");
//			}
			
			//Constraint 10 Erdogan: Reduce Fuel based on traveled distance
			for (int k = 0; k < K.length; k++) {
				// Hier müsste meiner Meinung nach j = 0 bis j < V.length hin, da es für 
				// alle Knoten und nicht nur für die Kunden gelten soll.
				for (int j = 1; j <= 2*n; j++) {
					for (int i = 0; i < V.length; i++) {
						if (i != j) {
							IloLinearNumExpr expr = cplex.linearNumExpr();
							expr.addTerm(1.0, z[i][k]);
							expr.addTerm(-c[i][j], x[i][j][k]);
							expr.setConstant(K[k].getFuelCapacity());
							expr.addTerm(-K[k].getFuelCapacity(), x[i][j][k]);
							cplex.addLe(z[j][k], expr, "Constraint10Erdogan");
						}
					}
				}
			}
			
			// Constraint 11 Erdogan: Set fuel level on max. fuel level when visiting an AFS.
			for (int j = 2*n+1; j <= 2*n+f; j++) {
				for (int k = 0; k < K.length; k++) {
					cplex.addEq(z[j][k], K[k].getFuelCapacity(),"Constraint11Erdogan");
				}
			}
			
			// Constraint 12 Erdogan: Remaining fuel must be sufficient to reach destination depot or AFS
			for (int k = 0; k < K.length; k++) {
				for (int i = 0; i < V.length; i++) {
					for (int j = 2*n+1; j <= 2*n+f; j++) {
						if (i != j) {
							cplex.addGe(z[i][k], Math.min(c[i][j], c[i][2*n+f+1]), "Constraint12Erdogan");
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
		for (int i = 0; i < V.length; i++) {
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
	 * 3 pickup locations, 3 dropdown locations, the start node and a end node will
	 * be created.
	 */
	public static void setDefaultNodes() {
		n = 5;
		f = 3;

		V = new Node[15];
		// The start node.
		V[0] = new Node(1, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 0);

		// The pick up nodes.
		V[1] = new Node(1, 1, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		V[2] = new Node(1, 4, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		V[3] = new Node(4, 3, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		V[4] = new Node(2, 2, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);
		V[5] = new Node(2, 4, 0, 2000, new int[] { 1, 0, 0, 0 }, 30);

		// The drop down nodes.
		V[6] = new Node(4, 1, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		V[7] = new Node(4, 4, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		V[8] = new Node(1, 3, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		V[9] = new Node(3, 4, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);
		V[10] = new Node(3, 1, 0, 2000, new int[] { -1, 0, 0, 0 }, 30);

		// AFS
		V[11] = new Node(2, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		V[12] = new Node(4, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		V[13] = new Node(3, 3, 0, 2000, new int[] { 0, 0, 0, 0 }, 15);
		
		// The end depot.
		V[14] = new Node(3, 2, 0, 2000, new int[] { 0, 0, 0, 0 }, 0);
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
					System.out.println("Solution for Truck " + k);
					for (int i = 0; i < V.length; i++) {
						System.out.print("\t" + i);
					}
					System.out.println();
					for (int i = 0; i < V.length; i++) {
						System.out.print(i + "\t");
						for (int j = 0; j < V.length; j++) {
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
					System.out.println();

					int node = getNextNode(0, k);
					double distance = c[0][node];
//					int tempNode = 0;
					System.out.print("Route: 0 -> ");
					while (node != 0) {						
						if (node != 2 * n + f + 1) {
							System.out.print(node + " -> ");
						} else {
							System.out.print(node);
						}
//						tempNode = node;
						node = getNextNode(node, k);
//						distance += c[tempNode][node];
					}
					
					System.out.println();
					
					System.out.println("Route duration for Truck " + k + ": "
							+ Math.round(cplex.getValue(B[2 * n + f + 1][k])) + " minutes.");

					System.out.println("Route distance for Truck " + k + ": " + distance + ".");
					
					System.out.println();
					
					node = getNextNode(0, k);
					System.out.println("Knoten\txPosition\tyPosition");
					System.out.println("0\t" + V[0].getxPosition() + "\t" + V[0].getyPosition());
					while (node != 0) {
						System.out.println(
								node + "\t" + V[node].getxPosition() + "\t" + V[node].getyPosition());
						node = getNextNode(node, k);
					}
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
