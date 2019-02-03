import java.io.*;

public class IO
{
    static User read(String fileName) {
        try {
             FileInputStream fileIn = new FileInputStream(fileName);
             ObjectInputStream in = new ObjectInputStream(fileIn);
             return (User) in.readObject();
        }
        catch(IOException i) {
             System.out.println("Error");
             return null;
        }
        catch(ClassNotFoundException c) {
             System.out.println("User class not found");
             return null;
        }
    }
    static void save(User tmp, String fileName) {
        try {
             FileOutputStream fileOut = new FileOutputStream(fileName);
             ObjectOutputStream out = new ObjectOutputStream(fileOut);
             out.writeObject(tmp);
             out.close();
             fileOut.close();
             System.out.println("Serialized data is saved in " + fileName);
        }
        catch(IOException i) { System.out.println("Error"); }
    }
}
