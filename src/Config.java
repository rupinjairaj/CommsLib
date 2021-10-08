
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;

public class Config {
    private String filePath;

    public Config(String path) {
        filePath = path;
    }

    // If the line does not begin with a number
    // the the line is invalid.
    private boolean isLineValid(String line) {
        return !line.isEmpty() && Character.isDigit(line.charAt(0));
    }

    // remove comment if present in line
    private String cleanLine(String line) {
        if (line.indexOf('#') != -1)
            line = line.substring(0, line.indexOf('#'));
        return line.trim();
    }

    // get arraylist of space delimited values of each line.
    private String[] lineItems(String line) {
        return line.split(" ");
    }

    // helper method to get the next valid line from the file.
    private String getNextValidLine(BufferedReader reader) {
        String line = null;
        try {
            line = reader.readLine();
            while (line != null && !isLineValid(line)) {
                line = reader.readLine();
            }
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    /**
     * Reads only valid lines from the configuration file. - First valid line is the
     * number of nodes in the system. - Next n valid lines are the details about the
     * node: eg: 0 dc02 12234 # node ID, host name, host port. - Next n valid lines
     * are mapping info about node and their neighbors. eg: 1 4 # space delimited
     * list of neighbors for node 0
     */
    public void readFile() {
        File f = new File(getCurrentDir() + filePath);
        if (!f.exists() || f.isDirectory()) {
            System.out.println("No valid file path provided for Config file.");

        }

        BufferedReader reader;
        NetworkInfo networkInfo = NetworkInfo.getInstance();
        String line;
        try {
            reader = new BufferedReader(new FileReader(getCurrentDir() + filePath));

            // first valid line
            line = getNextValidLine(reader);
            System.out.println(line);
            line = cleanLine(line);
            int numNodes = Integer.parseInt(line);
            networkInfo.setNumberOfNodes(numNodes);

            // k valid lines hold info about nodes (i.e., node ID, host name & host port)
            int k = numNodes;
            while (k > 0 && line != null) {
                k--;
                line = getNextValidLine(reader);
                line = cleanLine(line);
                String items[] = lineItems(line);
                int id = Integer.parseInt(items[0].trim());
                String hostName = items[1].trim();
                int port = Integer.parseInt(items[2].trim());
                NodeInfo node = new NodeInfo(id, hostName, port, null, null, null);
                networkInfo.addNode(node);
            }

            // n valid lines hold IDs of neighbors of kth node.
            k = 0;
            while (k < numNodes && line != null) {
                k++;
                line = getNextValidLine(reader);
                line = cleanLine(line);
                String items[] = lineItems(line);
                ArrayList<Integer> list = new ArrayList<Integer>();
                for (String i : items) {
                    list.add(Integer.parseInt(i));
                }
                networkInfo.setNeighbor(list);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // helper method to get the dir of projects
    private String getCurrentDir() {
        return FileSystems.getDefault().getPath("").toAbsolutePath().toString();
    }
}
