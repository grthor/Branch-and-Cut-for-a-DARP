# Branch-and-Cut-Algorithm-for-a-Dial-a-Ride-Problem
This is the implementation of the paper "A Branch-and-Cut Algorithm for the Dial-a-Ride Problem" from Jean-Francois Cordeau. The paper can be found [here](https://pdfs.semanticscholar.org/a047/2611e636eb8d7f4225affb9980a9cd3c2791.pdf).

**The algorithm is implemented using the CPLEX JAVA API.**

The main part of the implementation is in located in [model.java](https://github.com/grthor/Branch-and-Cut-for-a-DARP/blob/master/src/logic/model.java). This file contains all the constraints. The classes [Truck.java](https://github.com/grthor/Branch-and-Cut-for-a-DARP/blob/master/src/logic/Truck.java) contains the code for a vehicle and [Node.java](https://github.com/grthor/Branch-and-Cut-for-a-DARP/blob/master/src/logic/Node.java) contains the code for a node in the graph.

**The code is fully commented and works.**

## How to use

1. Download the src/log foldere.
2. Open Eclipse and opoen the project via 'File' -> 'Open Projects From File System' and select the download folder.
3. Add the cplex.jar to the referenced libraries. You need Cplex being installed on your PC. How you add cplex.jar to the project you can read in the [official documentation of IBM on how to add cplex.jar to Eclipse IDE](https://www.ibm.com/support/pages/configuring-eclipse-java-ide-use-cplex-libraries).
4. Simply run [model.java](https://github.com/grthor/Branch-and-Cut-for-a-DARP/blob/master/src/logic/model.java) via 'right click on the file' and 'Run As' -> '1 Java Application'. The model can be solved with the default settings.
