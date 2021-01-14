import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * This class is responsible for writing the data of the file.
 * The writing happens using a single thread which manage the whole writing to disk process.
 * Using the queue that is held in the DownloadManager (and filled by the workers), the FileWriter removes
 * elements from the queue one by one and writes it to the disk.
 */
public class FileWriter implements Runnable {
    private RandomAccessFile downloadedFile;
    private String path;
    private Metadata metadata;
    private float bytesWritten;
    private float downloaded;
    private float lastPercentage;
    private DownloadManager mainThread;

    public FileWriter(String path, DownloadManager mainThread) {
        try {
            this.path = path;
            this.mainThread = mainThread;
            downloadedFile = new RandomAccessFile(this.path, "rw");
            deserialize();
            this.bytesWritten = this.metadata.getNumOfBytesSoFar();
            this.downloaded = (int) (bytesWritten / DownloadManager.sizeOfFile * 100);
        } catch (FileNotFoundException e) {
            this.mainThread.endProgram(e);
        }
    }

    @Override
    public void run() {
        File tempMetadata = null, realMetadata = null;
        while (this.metadata.getNumOfBytesSoFar() < DownloadManager.sizeOfFile) { // While there is still more bytes to write to the disk
            if (!DownloadManager.chunkQueue.isEmpty()) {
                try {
                    Chunk chunk = DownloadManager.chunkQueue.take();

                    if (bytesWritten == 0) {
                        System.out.println("Downloaded 0%");
                    }
                    // Seeks the right position and writes the chunk's data into the downloaded file.
                    downloadedFile.seek(chunk.getOffset());
                    downloadedFile.write(chunk.getData());
                    this.metadata.setBytesWrittenToMetadata(chunk.getData().length);
                    this.metadata.setBitMapIndexToTrue(chunk.getId());

                    FileOutputStream fileOutputStream = new FileOutputStream(this.path + ".tmp1");
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(this.metadata);
                    objectOutputStream.close();
                    fileOutputStream.close();

                    tempMetadata = new File(this.path + ".tmp1");
                    Path tempPath = Paths.get(tempMetadata.getAbsolutePath());

                    realMetadata = new File(this.path + ".tmp");
                    Path realPath = Paths.get(realMetadata.getAbsolutePath());
                    try {
                        // Renames file from temp to real.
                        Files.move(tempPath, realPath, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException ignored) { // Ignoring this since even if the renaming didn't work, we will read this chunk again.
                    }

                    bytesWritten += chunk.getData().length;
                    // Calculates download percentage.
                    downloaded = (int) (bytesWritten / DownloadManager.sizeOfFile * 100);

                    if (downloaded != lastPercentage) {
                        System.out.println("Downloaded " + (int) downloaded + "%");
                        lastPercentage = downloaded;
                    }
                } catch (IOException | InterruptedException e) {
                    // Closes the RandomAccessFile in case of an error.
                    closeFile(downloadedFile);
                    this.mainThread.endProgram(e);
                }
            }
        }

        System.out.println("Download succeeded");
        // Deletes the remaining files.
        if (realMetadata != null) {
            boolean deletedReal = realMetadata.delete();
            while (!deletedReal) {
                deletedReal = realMetadata.delete();
            }
        }
    }

    public boolean[] getBitmap() {
        return this.metadata.getBitMap();
    }

    public void closeFile(RandomAccessFile file) {
        try {
            file.close();
        } catch (IOException e) {
            this.mainThread.endProgram(e);
        }
    }

    // Deserializes the metadata from the temporary file.
    public void deserialize() {
        try {
            File tempFile = new File(this.path + ".tmp");

            if (tempFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(tempFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                this.metadata = (Metadata) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            } else {
                this.metadata = new Metadata(DownloadManager.numberOfChunks);
            }
        } catch (Exception e) {
            this.mainThread.endProgram(e);
        }
    }
}
