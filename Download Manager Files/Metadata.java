import java.io.Serializable;

/**
 * This class represents the metadata about the file that is being downloaded.
 */
public class Metadata implements Serializable {
    private boolean[] bitMap;
    private int bytesWrittenToMetadata = 0;

    public Metadata(int bitMapSize) {
        bitMap = new boolean[bitMapSize];
    }

    public boolean[] getBitMap() {
        return this.bitMap;
    }

    public void setBitMapIndexToTrue(int index) {
        this.bitMap[index] = true;
    }

    public void setBytesWrittenToMetadata(int sizeOfChunk) {
        this.bytesWrittenToMetadata += sizeOfChunk;
    }

    public int getNumOfBytesSoFar() {
        return this.bytesWrittenToMetadata;
    }
}
