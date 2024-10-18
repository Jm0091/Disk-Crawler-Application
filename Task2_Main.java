import java.io.File;
import java.util.Scanner;

public class Task2_Main {
    public static void main(String[] args) {
        System.out.println("********************************  Disk File Crawler  ***************************************");
        Task2_ProducerConsumer producerConsumer = new Task2_ProducerConsumer();
        System.out.println("-->Task 2<--");

        // Taking User input
        File directory = takeInput("directory");                   //C:\test10183             // for testing
        File searchFile = takeInput("file");

        // Calling startIndexing method which starts crawling to each file using search term and directory provided
        producerConsumer.startIndexing(directory,searchFile);
    }

    /**
     * takeInput method to take a input with applying any validation conditions
     * @param value using value to identify for what we are taking this input to have apply specific validation conditions
     * @return File from user's inputted value
     */
    private static File takeInput(String value) {

        Scanner scanner = new Scanner(System.in);                                           // for user input
        boolean validInput = false;                                                         // used in while loop
        String userInput = "";

        while(!validInput){

            System.out.print("Please enter " + value +" : ");
            userInput = scanner.next();

            //if we are taking input for directory - checking for valid directory
            if(value.equals("directory")) {
                File directory = new File(userInput);
                if (!(directory.exists() && directory.isDirectory()))
                {
                    System.out.println("Directory "+ userInput +" does not exist!");
                }
                else if (userInput.equals("")) {
                    System.out.println("** Invalid " + userInput + " **");
                } else {
                    validInput = true;
                }
            }
            else{               // if we are not taking input for directory - which is file, we do not have any validations for it. updates variable to true.
                validInput = true;
            }

        }
        return new File(userInput);
    }
}