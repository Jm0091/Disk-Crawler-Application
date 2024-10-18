import java.io.File;

/**
 * FileWithDir class representing File with having possible future properties related to file
 *          - currently only storing File file
 */
public class FileWithDir {
    public File file;


    public FileWithDir(File file) {
        this.file = file;
    }

    /**
     * Overriding equals method for comparing 2 files (in contains())
     * @param other other file
     * @return true if both files are same or false
     */
    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass() == this.getClass()) {
            if (this.file == ((FileWithDir) other).file) {
                return true;
            }
        }
        return false;
    }

    /**
     * Overriding toString method
     * @return file
     */
    @Override
    public String toString() {
        return "File " + file ;
    }
}
