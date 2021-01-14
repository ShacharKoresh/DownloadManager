# Download Manager
## In general
This application is used for downloading files from the internet.

It supports the following functionality:
- Resume Downloads- the download manager is able to recover from a previously stopped download (process stopped with a signal, power outage, network disconnection, etc.)
- Concurrent Connections- the download manager supports downloading a file using multiple HTTP connections.
- Multi-server download- the download manager is able to fetch different parts of the file from different servers. 

## Files description

**IdcDm.java** - This class is the starting point of the program. It creates the DownloadManager according to the given arguments from the user.

**DownloadManager.java** - This class Manage the whole download process by dividing the work to the threads, and creating the writer thread.

**DownloadRangeWorker.java** - This class represents a single thread that reads bytes from the file.

**Chunk.java** - This class represents a chunk of data, which holds a byte array containing this data.

**FileWriter.java** - This class is responsible for writing the data of the file using a single thread which manage the whole writing to disk process.

**Metadata.java** - This class represents the metadata about the file that is being downloaded.
