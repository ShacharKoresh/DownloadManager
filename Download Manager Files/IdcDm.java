/**
 * This class is the starting point of the program. It creates the DownloadManager according
 * to the given arguments from the user.
 */
public class IdcDm {

    public static void main(String[] args) throws InterruptedException {
        String urlAsString = "";
        int numOfArgs = args.length;
        // Default number of threads.
        int numOfWorkers = 1;

        // Checks input arguments.
        // If no arguments were given, print a usage message.
        if (numOfArgs == 0) {
            System.err.println("usage:\n\tjava IdcDm URL|URL-LIST-FILE [MAX-CONCURRENT-CONNECTIONS]");
            return;
        }

        // User asked for concurrent connections.
        if (numOfArgs == 2) {
            numOfWorkers = Integer.parseInt(args[1]);
        }

        // Gets the URL from the first given argument.
        urlAsString = args[0];
        // Initializes the DownloadManager in order to start downloading.
        new DownloadManager(urlAsString, numOfWorkers);
    }
}
