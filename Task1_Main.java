import java.io.File;
import java.util.Scanner;

public class Task1_Main {

    public static void main(String[] args) {

        System.out.println("********************************  Disk File Crawler  ***************************************");
        System.out.println("-->Task 1<--");

        Task1_ProducerConsumer producerConsumer = new Task1_ProducerConsumer();
        // array of Files to crawl
        File[] files = new File[1];
        files[0] = new File("C:\\test10183");                       // hard coding one directory
        // Calling startIndexing method which starts crawling to each file using search term and directory provided
        producerConsumer.startIndexing(files);

    }
}