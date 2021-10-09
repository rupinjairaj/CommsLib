## Getting Started

Project code for **CS6378 Advanced Operating Systems** *Project 1*

## Folder Structure

- `src` holds the code for the project. Includes all files created to successfully run a *Node*.

## How to run the project

> Step 1) cd into src and compile
<code>

    src$ javac -d ../bin *.java
</code>

> Step 2) copy config file into bin directory
<code>

    src$ cp ./config ../bin/config
</code>

> Step 3) cd into bin and run application `NOTE: Please start the nodes in decreasing order of node ID. (Eg: Given nodes: 1, 2, 3, 4 -> start node 4, then node 3, then node 2 and finally node 1. This does not affect the working of the system but prevents unnecessary logging done when the node fails to connect with a peer and attempts to connect again. The system does work either way. This note is just for easy of usability`.
<code>

    bin$ java Main [nodeID] [path to config file from pwd starting with a '/']
</code>

## Team Members
- Mehak LNU (MXL200018)
- Akhila Gunjari (AXG210064)
- Rupin Jairaj (RXJ200003)