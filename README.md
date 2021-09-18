## Getting Started

Project code for **CS6378 Advanced Operating Systems** *Project 1*

## Folder Structure

- `src` holds the code for the project. Sub-packages too are present under this directory.

- The compiled output files will be generated in the `bin` folder by default.

- This project is written using VS Code with the *Extension Pack for Java* enabled.

## Configutaion file guidelines
- Plain-text file (max 100kB).
- Valid lines begin with unsigned integers.
- Ignore invalid lines.

### Sample config file
<code>
    
    5 # number of nodes

    0 dc02 12234    # nodeID hostName listeningPort
    1 dc03 11233
    2 dc04 22233
    3 dc05 15232
    4 dc06 16233
    1 4             # space delimited list of neighbors for node 0
    0 2 3           # space delimited list of neighbors for node 1
    1 3             # ... node 2
    1 2 4           # ... node 3
    0 3             # ... node 4    

</code>