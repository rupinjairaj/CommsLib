## Getting Started

Project code for **CS6378 Advanced Operating Systems** *Project 3*

## Folder Structure

- `src` holds the code for the project. Includes all files created to successfully run a *Node* and the validation *Server*.

## How to run the project

> Step 1) cd into src and compile.
<code>

    src$ javac -d ../bin *.java

</code>

> Step 2) copy config file into bin directory.
<code>

    src$ cp ./config ../bin/config

</code>

> Step 3) cd into bin and run application.
<code>

    bin$ java Main [nodeID] [name of config file in the same directory as the executable]

</code>

# Important Instructions:
- The correctness of the algorithm is verified using a test server which should run at DC01. The value is currently hardcoded because it would otherwise break the specified config file & command line argument contract.
- The server has to start before the nodes to ensure it doesn't miss any notifications from the nodes.
- To start the server run the following command:

<code>

    bin$ java -ea Server [port number] [expected num of cs reqs] [num of nodes]

</code>

- The server keeps track of the number of critical sections being executed. It should match the sum of critical sections executed in all nodes.
- The server will only terminate after it receives a termination request from all nodes.
- **NOTE:** The server should run with the '-ea' flag enabled since we want to enable assertions while executing Server.java 

## Team Members
- Mehak LNU (MXL200018)
- Akhila Gunjari (AXG210064)
- Rupin Jairaj (RXJ200003)