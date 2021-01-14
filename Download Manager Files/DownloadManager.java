import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class Manage the whole download process by dividing the work to the threads, and creating the writer thread.
 */
public class DownloadManager {
    public static long sizeOfFile;
    public static int numberOfChunks;
    public static ArrayBlockingQueue<Chunk> chunkQueue;
    public static final int SIZE_OF_CHUNK = 1024 * 32; // = 32KB
    public static boolean[] bitMap;
    public List<String> urlsAsStringList;
    public int numberOfWorkers;
    public FileWriter fileWriter;
    public Thread[] threads;
    public int sizeOfLastChunk;

    // Initializes DownloadManager variables.
    public DownloadManager(String urlsList, int numOfWorkers) {
        urlsAsStringList = getUrlsList(urlsList);
        sizeOfFile = getURLSize(urlsAsStringList.get(0));
        numberOfWorkers = numOfWorkers;
        numberOfChunks = (int) (Math.ceil((float) sizeOfFile / SIZE_OF_CHUNK));
        sizeOfLastChunk = (int) sizeOfFile % SIZE_OF_CHUNK;
        DivideWork();
    }

    // Returns the size of the file in the given URL, represented as a string.
    public static long getURLSize(String urlAsString) {
        long size = 0;

        try {
            URL url = new URL(urlAsString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // Opens stream to get size of file
            connection.setRequestMethod("HEAD"); // Set request method to HEAD
            size = connection.getContentLength(); // Get size of file
            connection.disconnect();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Download failed.");
        }

        return size;
    }

    // Determines the path where the downloaded file will be saved.
    // The program will download the file specified in the URL (following redirects) into the current directory, e.g.,
    // https://archive.org/download/Mario1_500/Mario1_500.avi will be downloaded to Mario1_500.avi.
    public static String getPath(String urlAsString) {
        int position = urlAsString.lastIndexOf("/");
        String directory = new File("").getAbsolutePath();
        return directory + urlAsString.substring(position, urlAsString.length());
    }

    // Creates the program workers and divides the work between them.
    public void DivideWork() {
        try {
            threads = new Thread[numberOfWorkers];

            chunkQueue = new ArrayBlockingQueue<Chunk>(numberOfChunks);
            String path = getPath(urlsAsStringList.get(0));
            fileWriter = new FileWriter(path, this);
            bitMap = fileWriter.getBitmap();
            Thread writerThread = new Thread(fileWriter);
            // Starts the writer thread.
            writerThread.start();

            // Calculates worker's parameters according to the given data.
            int chunksPerWorker = (int) (numberOfChunks / numberOfWorkers);
            int range = chunksPerWorker * SIZE_OF_CHUNK;
            int workerOffset = 0;
            int currentFirstChunk = 0, currentLastChunk = 0;

            if (numberOfWorkers == 1) {
                System.out.println("Downloading...");
            } else {
                System.out.println("Downloading using " + numberOfWorkers + " connections...");
            }
            // Creates workers.
            DownloadRangeWorker worker = null;

            // If some of the workers have finished, they will not actually execute their run() method.
            // In this case, the user will see less thread "start" prints than he asked for.
            for (int i = 0; i < numberOfWorkers; i++) {
                String workerUrl = getUrlForWorker(i);

                // First chunk in original worker range
                currentFirstChunk = workerOffset / SIZE_OF_CHUNK;
                // Last chunk in original worker range
                currentLastChunk = currentFirstChunk + chunksPerWorker;

                if (i == numberOfWorkers - 1) { // last worker
                    chunksPerWorker += numberOfChunks - (numberOfWorkers * chunksPerWorker);
                    range = (int) sizeOfFile - workerOffset;
                    currentLastChunk = currentFirstChunk + chunksPerWorker;

                    for (int j = currentFirstChunk; j < currentLastChunk; j++) {
                        if (bitMap[j]) {
                            if (j == currentLastChunk - 1) { // Last chunk.
                                workerOffset += sizeOfLastChunk;
                                range -= sizeOfLastChunk;
                            } else {
                                workerOffset += SIZE_OF_CHUNK;
                                range -= SIZE_OF_CHUNK;
                            }
                        } else {
                            break;
                        }
                    }
                    worker = new DownloadRangeWorker(workerOffset, range, bitMap, workerUrl, this);
                    worker.setLast();
                } else {
                    for (int j = currentFirstChunk; j < currentLastChunk; j++) {
                        if (bitMap[j]) {
                            workerOffset += SIZE_OF_CHUNK;
                            range -= SIZE_OF_CHUNK;
                        } else {
                            break;
                        }
                    }
                    worker = new DownloadRangeWorker(workerOffset, range, bitMap, workerUrl, this);
                    workerOffset += range;
                    range = chunksPerWorker * SIZE_OF_CHUNK;
                }

                // Starts the workers threads.
                threads[i] = new Thread(worker);
                threads[i].start();
            }
        } catch (Exception e) {
            System.err.println("No Internet Connection. Try Again.");
            System.err.println("Download failed.");
            System.exit(1);
        }
    }

    private static List<String> getUrlsList(String urlsList) {
        List<String> list = new ArrayList<String>();
        // Creates a file from the given list in order to read line after line.
        File file = new File(urlsList);

        if (!file.exists()) {
            list.add(urlsList);
            return list;
        }

        // If the Given url is a File - we'll break it to mirrors and adds them one by one to the array
        try {
            BufferedReader reader = new BufferedReader(new FileReader(urlsList));
            String currentUrl = reader.readLine();
            while (currentUrl != null) {
                list.add(currentUrl);
                currentUrl = reader.readLine();
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("Download failed.");
        }

        return list;
    }

    private String getUrlForWorker(int workerId) {
        int numOfServers = urlsAsStringList.size();

        // One url is given.
        if (numOfServers == 1) {
            return urlsAsStringList.get(0);
        }
        // If servers outnumber workers, we can skip the additional urls.
        if (numOfServers > numberOfWorkers || numOfServers == numberOfWorkers) {
            return urlsAsStringList.get(workerId);
        }
        // More workers than servers.
        else {
            return urlsAsStringList.get(workerId % numOfServers);
        }
    }

    public synchronized void endProgram(Exception err) {
        System.err.println(err.getMessage());
        System.err.println("Download failed.");
        System.exit(1);
    }
}
