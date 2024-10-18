import com.sun.security.jgss.GSSUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static javafx.application.Platform.exit;

/**
 * ProducerConsumer
 * <p/>
 * Producer and consumer tasks in a desktop search application
 *
 * @author Brian Goetz and Tim Peierls
 */
public class Task2_ProducerConsumer {
    /**
     * FileCrawler class: producer class
     * Adding/putting files to Blocking Queue if it's not directory
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
         * Number of Consumer tasks - to add poison objects
         */
        private final int nCTasks;

        /**
         * Constructor initializing fileQueue, fileFilter, root
         *
         * @param fileQueue  Getting queue with specific bound
         * @param fileFilter filter to accept file's pathname
         * @param root       root file from file structure
         * @param numCTasks  num of consumer tasks
         */
        public FileCrawler(BlockingQueue<File> fileQueue, final FileFilter fileFilter, File root, int numCTasks) {
            this.fileQueue = fileQueue;
            this.root = root;
            this.nCTasks = numCTasks;
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
         *
         * @param file file to compare
         * @return true if file is already indexed or false
         */
        private boolean alreadyIndexed(File file) {
            FileWithDir newFileDir = new FileWithDir(file);
            if (fileWithDirArrayList.contains(newFileDir)) {
                System.out.println("P: Found the same item! <---------");
                return true;
            }
            return false;
        }

        /**
         * Run method calls recursive crawl method with passing root, and adds poison objects
         * Poison objects used are : ">" and "<"  (these characters can not be found in file name or path, will not affect filtering)
         */
        public void run() {
            try {
                crawl(root);
                for (int i = 0; i < nCTasks - 1; i++) {
                    fileQueue.put(new File("<"));
                    //System.out.println("P: Added POISON items name: \"<\"");         // for debug
                }
                fileQueue.put(new File(">"));
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
                    } else if (!alreadyIndexed(entry)) {
                        fileWithDirArrayList.add(new FileWithDir(entry));
                        fileQueue.put(entry);
                        //System.out.println("P: Added items name: " + entry.getName());         // for debug
                    } else if (alreadyIndexed(entry)) {
                        // System.out.println("P: found same item!");                              // for debug
                    }
                }
            }
        }
    }

    /**
     * Indexer class - CONSUMER. -> goes through the queue and take files out. And
     * -> keeping the count of total num of files found based on user's input director and search term
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
         * AtomicInteger - because without using AtomicInteger, the counter was miss calculating because of multiple Threads.
         */
        public static AtomicInteger counter = new AtomicInteger();
        /**
         * File we are searching
         */
        private final File SearchFile;
        /**
         * Directory File into which we will search files
         */
        private final File Directory;
        /**
         * fileWithDirArrayList to check the file is already indexed or not
         */
        private static final ArrayList<FileWithDir> fileWithPathArrayList = new ArrayList<>();

        /**
         * Constructor initializes values
         *
         * @param queue      queue of files
         * @param searchFile File to search
         * @param directory  Directory File to search into
         */
        public Indexer(BlockingQueue<File> queue, File searchFile, File directory) {
            this.queue = queue;
            this.SearchFile = searchFile;
            this.Directory = directory;
        }

        /**
         * Run() Checks: if file matches giving searchTerm, indexes it
         *               if file is poison object - breaks
         *               if file is last poison object - shows results
         */
        public void run() {
            while (true) {
                try {
                    File currFile = queue.take();
                    if (currFile.getName().toLowerCase().matches("(.*)" + SearchFile.getName().toLowerCase() + "(.*)")) {
                        indexFile(currFile);
                    }
                    else if (currFile.getName().equals("<")) {
                        //System.out.println("C: Got POISON - exiting thread: " + Thread.currentThread().getName());            //for debug
                        break;
                    }
                    else if (currFile.getName().equals(">")) {
                        // producer added ">" as a poison for one last thread to print RESULTS
                        System.out.println("\nTOTAL FILES received for search term = \"" + SearchFile.getName() + "\" in directory = " + Directory.getAbsolutePath().toString() + "  : " + counter.get());
                        if (counter.get() == 0) {
                            System.out.println("Please try again with different values! Thank you :)");
                        }
                        break;
                    }
                    Thread.sleep(random.nextInt(500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * indexFile - Adds matched file to ArrayList
         *           - Increments the counter
         *           - Prints the file found
         */
        public void indexFile(File currFile) {
            fileWithPathArrayList.add(new FileWithDir(currFile));
            System.out.println(counter.incrementAndGet() + ". Found " + currFile.toString());
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
     * number of consumer tasks
     *
     */
    private static int N_CTASKS = 30;

    /**
     * startIndexing method starts threads for producer and consumer for crawling through files
     */
    public static void startIndexing(File directory, File searchFile) {
        //using Blocking Queue with BOUND
        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(BOUND);
        // not filtering using FileFilter. It returns true.
        FileFilter filter = new FileFilter() {public boolean accept(File file) {  return true;  }   };
        //starting threads for producer and consumer

        File[] roots = new File[1];
        roots[0] = directory;

        //ExecutorService - ThreadPool
        ExecutorService pool = Executors.newFixedThreadPool(N_CONSUMERS);

        // Here we have two options
        //   1. Use FileFilter and crawl files and start(submit) tasks based on that count.  (If I use this concept, there will be no major part left for consumer class for assignment requirements)
        //   2. Hard coded value based on possible average number of result. (Average of Max and Min possible outcome to make that number of accurate tasks which helps to use threadPool)

        pool.submit(new FileCrawler(queue, filter, roots[0], N_CTASKS - 1));
        //pool.submit(new FileCrawler(queue, filter2, roots[0], N_TASKS-2));
        for (int i = 0; i < N_CTASKS - 1; i++) {
            pool.submit(new Indexer(queue, searchFile, directory));
        }
        pool.shutdown();
        try {
            pool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
