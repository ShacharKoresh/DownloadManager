import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class represents a single thread that reads bytes from the file.
 */
public class DownloadRangeWorker implements Runnable {
    private int workerOffset;
    private int range;
    private String workerUrl;
    private URL url;
    private boolean isLast = false;
    private boolean[] bitMap;
    private HttpURLConnection connection;
    private int firstChunkId;
    private int lastChunkId;
    private DownloadManager mainThread;

    public DownloadRangeWorker(int offset, int range, boolean[] bitMap, String urlAsString, DownloadManager mainThread) {
        this.workerOffset = offset;
        this.range = range;
        this.bitMap = bitMap;
        this.mainThread = mainThread;
        try {
            this.workerUrl = urlAsString;
            this.url = new URL(this.workerUrl);
        } catch (Exception e) {
            this.mainThread.endProgram(e);
        }
        firstChunkId = this.workerOffset / DownloadManager.SIZE_OF_CHUNK;
        lastChunkId = firstChunkId + (this.range / DownloadManager.SIZE_OF_CHUNK);
    }

    // Runs the thread of a single worker in order to read from the file and create data chunks accordingly.
    @Override
    public void run() {
        // Make sure this thread still has more bytes left to read, even after resuming the program.
        if (this.workerOffset < this.workerOffset + this.range - 1) {
            String string = String.format("[%s] Start downloading range (%d - %d) from:\n%s", Thread.currentThread().getId(),
                    this.workerOffset, this.workerOffset + this.range - 1, this.workerUrl);
            System.out.println(string);

            // Creates URL and connections
            try {
                String bytesToRead = String.format("Bytes=%d-%d", this.workerOffset, this.workerOffset + this.range - 1);
                this.connection = (HttpURLConnection) this.url.openConnection();
                this.connection.setRequestProperty("Range", (bytesToRead));

                int chunkOffset = firstChunkId * DownloadManager.SIZE_OF_CHUNK;
                int sizeInBytes = DownloadManager.SIZE_OF_CHUNK;
                byte[] data;

                // For the last worker, allocate an extra chunk if needed.
                if (this.isLast && (this.range % DownloadManager.SIZE_OF_CHUNK != 0)) {
                    lastChunkId += 1;
                }

                InputStream inputStream = this.connection.getInputStream();
                for (int i = this.firstChunkId; i < this.lastChunkId; i++) {
                    int bytes = 0;

                    if (!this.bitMap[i]) {
                        if (this.isLast && i == this.lastChunkId - 1) { // Last chunk of last worker
                            if ((int) DownloadManager.sizeOfFile % DownloadManager.SIZE_OF_CHUNK != 0) {
                                sizeInBytes = (int) DownloadManager.sizeOfFile % DownloadManager.SIZE_OF_CHUNK;
                            }
                        }

                        data = new byte[sizeInBytes];
                        // Reads the bytes from the file.
                        int numOfBytesRead = 0;
                        while (numOfBytesRead < sizeInBytes && (bytes != -1)) {
                            bytes = inputStream.read(data, numOfBytesRead, sizeInBytes - numOfBytesRead);
                            numOfBytesRead += bytes;
                        }

                        Chunk chunk = new Chunk(chunkOffset, data, i);
                        DownloadManager.chunkQueue.put(chunk);
                    } else {
                        long numOfBytesSkipped = inputStream.skip(sizeInBytes);
                        while (numOfBytesSkipped < sizeInBytes) {
                            System.err.println("Couldn't skip entire range for input stream.");
                            numOfBytesSkipped = inputStream.skip(sizeInBytes - numOfBytesSkipped);
                        }
                    }

                    chunkOffset += sizeInBytes;
                }
            } catch (IOException | InterruptedException e) {
                this.mainThread.endProgram(e);
            }

            // Thread is done reading chunks from input-stream.
            string = String.format("[%s] Finished downloading", Thread.currentThread().getId());
            System.out.println(string);
            this.connection.disconnect();
        }
    }

    public void setLast() { // Last worker.
        this.isLast = true;
    }
}
