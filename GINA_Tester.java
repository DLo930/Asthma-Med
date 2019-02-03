import java.io.*;
import java.util.*;
import java.time.*;

public class GINA_Tester {
    public static void main(String[] args) {
        String file = "C:\\Users\\Dennis\\Desktop\\TestCases\\user2.ser";
        Scanner scan = new Scanner(System.in);
        System.out.print("Write to file (0) or read data (1)? ");
        int io = scan.nextInt();
        
        if(io == 0) {
            System.out.print("Name, DOB (yyyy-mm-dd), Initial severity: ");
            User tmp = new User(scan.next(), LocalDate.parse(scan.next()), scan.nextInt());
            System.out.print("How many dayEntries (format: yyyy-mm-dd a n i s e)? ");
            int n = scan.nextInt();
            while(n-- > 0) tmp.addDayEntry(LocalDate.parse(scan.next()), scan.nextInt(), scan.nextInt(), scan.nextInt(), scan.nextInt(), (scan.next()=="yes") ? true : false);
            System.out.print("How many visitEntries (format: yyyy-mm-dd FEV1/FVC% s)? ");
            int m = scan.nextInt();
            while(m-- > 0) tmp.addVisitEntry(LocalDate.parse(scan.next()), scan.nextInt(), scan.nextInt());
            
            IO.save(tmp, file);
        }
        else {
            User tmp2 = IO.read(file);
            System.out.println("Name: " + tmp2.getName() + "\tAge: " + tmp2.getAge() + "\t\tInitial severity: " + tmp2.getInitSeverity());
            tmp2.getSeverity();
            tmp2.getControl();
            tmp2.getMedication();
        }
    }
}