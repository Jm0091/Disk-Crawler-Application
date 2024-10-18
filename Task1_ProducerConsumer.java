import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProducerConsumer
 * <p/>
 * Producer and consumer tasks in a desktop search application
 *
 * @author Brian Goetz and Tim Peierls
 */
public class Task1_ProducerConsumer {
    /**
     * FileCrawler class: producer class
     * Adding/putting file to Blocking Queue if it's not directory
     */
    static class FileCrawler implements Runnable {
        /**
         * FileQueue adding files into
         */
        private final BlockingQueue<File> fileQueue;
        /**
         * FileFilter
         */
        private final FileFilter fileFilter;
        /**
         * Root of the file structure
         */
        private final File root;
        /**
         * fileWithDirArrayList to check the file is already indexed or not
         */
        private static ArrayList<FileWithDir> fileWithDirArrayList = new ArrayList<>();

        /**
         * Constructor initializing fileQueue, fileFilter, root
         *
         * @param fileQueue  Getting queue with specific bound
         * @param fileFilter filter to accept file's pathname
         * @param root       root file from file structure
         */
        public FileCrawler(BlockingQueue<File> fileQueue, final FileFilter fileFilter, File root) {
            this.fileQueue = fileQueue;
            this.root = root;
            // not filtering using FileFilter. It returns true.
            this.fileFilter = new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || fileFilter.accept(f);
                }
            };
        }

        /**
         * alreadyIndexed - check the file and directory is checked before or not (ArrayList)
         * - adds to ArrayList if not indexed
         * - working with files & directory to make the object unique (only one file can be found with that name in particular directory)
         *
         * @param file      file to compare
         * @param directory directory to compare file is from which directory as a
         * @return true if file is already indexed or false
         */
        private boolean alreadyIndexed(File file, File directory) {
            FileWithDir newFileDir = new FileWithDir(file);
            if (fileWithDirArrayList.contains(newFileDir)) {
                System.out.println("P: Found the same item! <---------");
                return true;
            }
            return false;
        }

        /**
         * Run method calls recursive crawl method with passing root, and adds poison objects
         */
        public void run() {
            try {
                crawl(root);

                // implemented exit logic - poison object with the count of N_CONSUMERS
                for (int i = 0; i < N_CONSUMERS-1; i++) {
                    fileQueue.put(new File("POISON"));
                }
                fileQueue.put(new File("FINALMSG"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * crawl recursive method goes through all directories inside root and adds files to queue
         *
         * @param root root file to start from
         * @throws InterruptedException if thread interrupted, throws exception
         */
        private void crawl(File root) throws InterruptedException {
            File[] entries = root.listFiles(fileFilter);

            if (entries != null) {
                for (File entry : entries) {
                    // if entry is directory - recursion call for files
                    if (entry.isDirectory()) {
                        crawl(entry);
                    }
                    else if (!alreadyIndexed(entry, root)) {
                        fileWithDirArrayList.add(new FileWithDir(entry));
                        fileQueue.put(entry);
                    }
                    else if (alreadyIndexed(entry, root)) {
                        //System.out.println("P: found same item");                        // for debug
                    }
                }

            }
        }
    }

    /**
     * Indexer class - CONSUMER. -> goes through the queue and take files out with keeping the count of total num of removed files
     */
    static class Indexer implements Runnable {
        /**
         * FileQueue adding files into
         */
        private final BlockingQueue<File> queue;
        /**
         * For random generation
         */
        private Random random = new Random();
        /**
         * For count the number of files removed from Queue
         *          AtomicInteger - because without using AtomicInteger, the counter was miss calculating because of multiple Threads.
         */
        public static AtomicInteger counter = new AtomicInteger();

        /**
         * Constructor initializes queue
         *
         * @param queue BlockingQueue of Files
         */
        public Indexer(BlockingQueue<File> queue) {
            this.queue = queue;
        }

        /**
         * Checks file is not poison and indexes it.
         */
        public void run() {
            while (true) {
                try {
                    File currFile = queue.take();
                    if (!currFile.getName().equals("POISON") && !currFile.getName().equals("FINALMSG")) {
                        indexFile();
                    }
                    else if (currFile.getName().equals("POISON")) {
                        //System.out.println("C: Got POISON - exiting thread: " + Thread.currentThread().getName());            //for debug
                        break;
                    }
                    else if (currFile.getName().equals("FINALMSG")) {
                        // producer added "FINALMSG" as a possion for one last thread to print RESULTS
                        System.out.println("\nTOTAL FILES received: " + counter.get());
                        break;
                    }

                    Thread.sleep(random.nextInt(500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }

        /**
         * indexFile increments the counter
         */
        public void indexFile() {
            counter.incrementAndGet();
        }
    }

    /**
     * BOUND for blockingQueue
     */
    private static final int BOUND = 10;
    /**
     * Num of consumers based on the available processor
     */
    private static final int N_CONSUMERS = Runtime.getRuntime().availableProcessors();

    /**
     * startIndexing method starts threads for producer and consumer for crawling through files
     *
     * @param roots toor file to start from
     */
    public static void startIndexing(File[] roots) {
        //using Blocking Queue with BOUND
        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(BOUND);
        // not filtering using FileFilter. It returns true.
        FileFilter filter = new FileFilter() {public boolean accept(File file) {  return true;  }   };


        //starting threads for producer and consumer
        for (File root : roots)
            new Thread(new FileCrawler(queue, filter, root)).start();

        for (int i = 0; i < N_CONSUMERS; i++)
            new Thread(new Indexer(queue)).start();

    }
}
