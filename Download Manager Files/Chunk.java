/**
 * This class represents a chunk of data, which holds a byte array containing this data.
 * Since we split the work, then we also keep an offset and id for every chunk.
 */
public class Chunk {
    private int offset;
    private byte[] data;
    private int id;

    public Chunk(int offset, byte[] dataToStore, int id) {
        this.offset = offset;
        this.data = dataToStore;
        this.id = id;
    }

    public int getOffset() {
        return this.offset;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getId() {
        return this.id;
    }
}
