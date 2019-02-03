import java.util.*;
import java.io.*;
import java.time.*;
import java.time.temporal.*;

/* 
 * Algorithms based on "EPR 3: Guidelines for the Diagnosis and Management of Asthma"
 * https://www.nhlbi.nih.gov/files/docs/guidelines/asthgdln.pdf
 * 
 * Code is used to assist, not replace, clinical decisionmaking
 */

enum AgeRange {
    FOUR(0), ELEVEN(1), OLDER(2); // Upper bound of age range
    
    private int val;
    private AgeRange(int n) { val = n; }
    public int getVal() { return val; }
}

enum AgeRange2 {
    NINETEEN(0), THIRTY_NINE(1), FIFTY_NINE(2), EIGHTY(3); //Upper bound of age range
    
    private int val;
    private AgeRange2(int n) { val = n; }
    public int getVal() { return val; }
}
enum Interference {
    NONE(0), MINOR(1), SOME(2), EXTREME(3);
    
    private int val;
    private Interference(int n) { val = n; }
    public int getVal() { return val; }
}
enum Severity {
    INTERMITTENT(0), MILD(1), MODERATE(2), SEVERE(3), OVERLAP(4);
    
    private int val;
    private Severity(int n) { val = n; }
    public int getVal() { return val; }
    public static String get(int n) {
        switch(n) {
            case 0: return "intermittent";
            case 1: return "mild";
            case 2: return "moderate";
            default: return "severe";
        }
    }
}
enum Control {
    WELL_CONTROLLED(0), POORLY_CONTROLLED(1), VERY_POORLY_CONTROLLED(2);
    
    private int val;
    private Control(int n) { val = n; }
    public int getVal() { return val; }
    public static String get(int n) {
        switch(n) {
            case 0: return "good";
            case 1: return "poor";
            default: return "very poor";
        }
    }
}
public class User implements Serializable {
    private String name;
    private int initSeverity, step;
    private LocalDate prevVisit = null, dob;
    private HashMap<LocalDate, DayEntry> calendar;
    private HashMap<LocalDate, VisitEntry> visitCalendar;
    
    public User(String n, LocalDate d, int s) {
        name = n;
        dob = d;
        initSeverity = s;
        calendar = new HashMap<LocalDate, DayEntry>();
        visitCalendar = new HashMap<LocalDate, VisitEntry>();
    }
    
    /*
     * Use during initial visit or any follow-up visit
     * Returns Severity enum based on "Impairment" factors in the previous month
     * Returned value will be accurate only if there is a month of dayEntries prior to method call
     */
    public void getSeverity() {
        LocalDate mark1 = LocalDate.now().minusDays(28), mark2 = LocalDate.now().minusDays(365), date;
        DayEntry day;
        int symptoms = 0, nas = 0, sabas = 0, interfs = 0, exacs = 0; //totals
        for(Map.Entry<LocalDate, DayEntry> entry : calendar.entrySet()) {
            date = entry.getKey();
            day = entry.getValue();
            if(date.compareTo(mark2) >= 0 && day.getExacs()) exacs++;
            if(date.compareTo(mark1) >= 0) {
                symptoms += day.getSymptoms();
                nas += day.getNAs();
                sabas += day.getSABAs();
                interfs += day.getInterf();
            }
        }
        
        int[] arr = new int[6];
        arr[0] = (int) Math.round(symptoms/4.0);
        arr[1] = nas;
        arr[2] = (int) Math.round(sabas/4.0);
        arr[3] = (int) Math.round(interfs/28.0);
        arr[4] = visitCalendar.get(prevVisit).getFEV1FVC();
        arr[5] = exacs;
        
        severityMessage(categorizeSeverity(arr));
    }
    
    /*
     * Iterates through Severity Classification ("Impairment" and "Risk") charts
     * Returns array representing severity classification in each factor
     */
    private int[] categorizeSeverity(int[] arr) {
        int[][][][] components = new int[][][][]{ // Values inclusive
            // INT  MILD   MOD   SEV
            // 0-4 yrs (p95)
            {{{0,2},{3,5},{6,8},{9,100}},   // Symptoms
            {{0,0},{1,2},{3,4},{5,100}},    // Nighttime awakenings
            {{0,0},{1,1},{2,2},{3,3}},      // Interference with normal activity
            {{0,2},{3,5},{6,8},{9,100}},    // SABA use/wk
            {{0,0},{0,0},{0,0},{0,0}},      // FEV1/FVC
            {{0,1},{2,10},{2,10},{2,10}}},  // Severe exacerbations/yr
            
            // 5-11 yrs (p96)
            {{{0,2},{3,5},{6,8},{9,100}},
            {{0,2},{3,4},{5,20},{21,100}},
            {{0,0},{1,1},{2,2},{3,3}},
            {{0,2},{3,5},{6,8},{9,100}},
            {{86,100},{81,100},{75,80},{0,74}},
            {{0,1},{2,10},{2,10},{2,10}}},
            
            // 12+ yrs (p97)
            {{{0,2},{3,5},{6,8},{9,100}},
            {{0,2},{3,4},{5,20},{21,100}},
            {{0,0},{1,1},{2,2},{3,3}},
            {{0,2},{3,5},{6,8},{9,100}},
            {{-1,-1},{-1,-1},{-5,-1},{-100,-6}},
            {{0,1},{2,10},{2,10},{2,10}}}};
            
        /* -1: normal FEV1/FVC, -#: #% lower than normal FEV1/FVC
         * 
         * Normal FEV1/FVC values: 
         *    8-19: 85%
         *   20-39: 80%
         *   40-59: 75%
         *   60-80: 70%
         */
        
        int age = getAge(), ageRange = getAgeRange(age), ageRange2 = getAgeRange2(age), normal;
        int[] severities = new int[6];
        for(int i = 0; i < 6; i++) {
            for(int j = 0; j < 4; j++) {
                int a = components[ageRange][i][j][0], b = components[ageRange][i][j][1];
                if(a == -1) continue; //-1: normal FEV1/FVC
                if(a < 0) {
                    if(age < 8) continue;
                    normal = 85 - 5 * ageRange2;
                    if(arr[4] >= normal + a && arr[4] <= normal + b) severities[i] = j;
                }
                else if(arr[i] >= a && arr[i] <= b) {
                    severities[i] = j;
                    break;
                }
            }
        }
        
        return severities;
    }
    
    // Displays user's severity and each factor's severity
    private void severityMessage(int[] arr) {
        System.out.println("Your asthma classifies as " + Severity.get(max(arr)) + ".\n");
        System.out.println("\tSymptoms: " + Severity.get(arr[0]));
        System.out.println("\tNighttime awakenings: " + Severity.get(arr[1]));
        System.out.println("\tInterference with normal activity: " + Severity.get(arr[2]));
        System.out.println("\tFrequency of SABA use: " + Severity.get(arr[3]));
        System.out.println("\tFEV1/FVC: " + Severity.get(arr[4]));
        System.out.println("\tSevere exacerbations: " + Severity.get(arr[5]) + "\n");
    }
    
    /*
     * Use after instantiation of dayEntry and visitEntry
     * Returns Control enum based on "Impairment" factors in the previous month
     * Returned value will be accurate only if there is a month of dayEntries prior to method call
     */
    public void getControl() {
        LocalDate mark1 = LocalDate.now().minusDays(28), mark2 = LocalDate.now().minusDays(365), date;
        DayEntry day;
        int symptoms = 0, nas = 0, sabas = 0, interfs = 0, exacs = 0; //totals
        for(Map.Entry<LocalDate, DayEntry> entry : calendar.entrySet()) {
            date = entry.getKey();
            day = entry.getValue();
            if(date.compareTo(mark2) >= 0 && day.getExacs()) exacs++;
            if(date.compareTo(mark1) >= 0) {
                symptoms += day.getSymptoms();
                nas += day.getNAs();
                sabas += day.getSABAs();
                interfs += day.getInterf();
            }
        }
        
        int[] arr = new int[6];
        arr[0] = (int) Math.round(symptoms/4.0);
        arr[1] = nas;
        arr[2] = (int) Math.round(sabas/4.0);
        arr[3] = (int) Math.round(interfs/28.0);
        arr[4] = visitCalendar.get(prevVisit).getFEV1FVC();
        arr[5] = exacs;
        
        controlMessage(categorizeSeverity(arr));
    }
    
    /*
     * Iterates through Control Classification ("Impairment" and "Risk") charts
     * Returns array representing control classification in each factor
     */
    private int[] categorizeControl(int[] arr) {
        int[][][][] components = new int[][][][]{ // Values inclusive
            // INT  MILD   MOD   SEV
            // 0-4 yrs (p98)
            {{{0,2},{3,8},{9,100}},     // Symptoms
            {{0,1},{2,4},{5,100}},      // Nighttime awakenings
            {{0,0},{1,2},{3,3}},        // Interference with normal activity
            {{0,2},{3,8},{9,100}},      // SABA use/wk
            {{0,0},{0,0},{0,0}},        // FEV1/FVC
            {{0,1},{2,10},{2,10}}},     // Severe exacerbations
            
            // 5-11 yrs (p99)
            {{{0,2},{3,8},{9,100}},
            {{0,1},{2,7},{8,100}},
            {{0,0},{1,2},{3,3}},
            {{0,2},{3,8},{9,100}},
            {{81,100},{75,80},{0,74}},
            {{0,1},{2,10},{2,10}}},
            
            // 12+ yrs (p100)
            {{{0,2},{3,5},{9,100}},
            {{0,2},{3,15},{16,100}},
            {{0,0},{1,2},{3,3}},
            {{0,2},{3,5},{6,8},{9,100}},
            {{0,0},{0,0},{0,0}},
            {{0,1},{2,10},{2,10}}}};
            
        /* -1: NA, -#: #% lower than normal FEV1/FVC
         * 
         * Normal FEV1/FVC values: 
         *    8-19: 85%
         *   20-39: 80%
         *   40-59: 75%
         *   60-80: 70%
         */
        int age = getAge(), ageRange = getAgeRange(age), ageRange2 = getAgeRange2(age), normal;
        int[] controls = new int[6];
        for(int i = 0; i < 6; i++) {
            for(int j = 0; j < 3; j++) {
                int a = components[ageRange][i][j][0], b = components[ageRange][i][j][1];
                if(a == -1) continue; //-1: normal FEV1/FVC
                if(a < 0) {
                    if(age < 8) continue;
                    normal = 85 - 5 * ageRange2;
                    if(arr[4] >= normal + a && arr[4] <= normal + b) controls[i] = j;
                }
                else if(arr[i] >= a && arr[i] <= b) {
                    controls[i] = j;
                    break;
                }
            }
        }
        
        return controls;
    }
    
    // Displays user's asthma control and each factor's level of control
    private void controlMessage(int[] arr) {
        System.out.println("Your asthma control is " + Control.get(max(arr)) + ".\n");
        System.out.println("\tSymptoms: " + Control.get(arr[0]));
        System.out.println("\tNighttime awakenings: " + Control.get(arr[1]));
        System.out.println("\tInterference with normal activity: " + Control.get(arr[2]));
        System.out.println("\tFrequency of SABA use: " + Control.get(arr[3]));
        System.out.println("\tFEV1/FVC: " + Control.get(arr[4]));
        System.out.println("\tSevere exacerbations: " + Control.get(arr[5]) + "\n");
    }
    
    public void getMedication() {
        String[][] medications = new String[][]{
            // 0-4 yrs (p328)
            {"S","1;C|M","2","2+L|M","I3+L|M","3+L|M+O"},
            
            // 5-11 yrs (p329)
            {"S","1;C|K|N|T","(1+L|K|T)|I2","2+L;2+K|T","3+L;3+K|T","3+L+O;3+K|T+O"},
            
            // 12+ yrs (p366)
            {"S","1;C|K|N|T","(1+L)|2;1+K|T|Z","2+L;2+K|T|Z","3+L&A","3+L+O&A"}};
        
        /* S: SABA, #: ICS, C: Cromolyn, M: Montelukast, T: Theophylline, L: LABA, K: LTRA, O: OCS, A: Omalizumab, Z: Zileuton
         * ";": Alternative, "|": Or, "+": And, "&": Consider
         */
        int ageRange = getAgeRange(getAge());
        stepMessage();
        medicationMessage(medications[ageRange][step-1]);
    }
    
    // Displays recommended treatment based on step
    private void medicationMessage(String s) {
        System.out.print("Recommended treatment:\n\n\t");
        for(int i=0;i<s.length();i++) {
            switch(s.charAt(i)) {
                case 'A': System.out.print("Omalizumab ");
                          break;
                case 'C': System.out.print("Cromolyn ");
                          break;
                case 'K': System.out.print("LTRA ");
                          break;
                case 'L': System.out.print("LABA ");
                          break;
                case 'M': System.out.print("Montelukast ");
                          break;
                case 'O': System.out.print("OCS ");
                          break;
                case 'S': System.out.print("SABA PRN ");
                          break;
                case 'T': System.out.print("Theophylline ");
                          break;
                case 'Z': System.out.print("Zileuton ");
                          break;
                case '1': System.out.print("Low-dose ICS ");
                          break;
                case '2': System.out.print("Medium-dose ICS ");
                          break;
                case '3': System.out.print("High-dose ICS ");
                          break;
                case ';': System.out.print("\n\nAlternative:\n\n\t");
                          break;
                case '|': System.out.print("or ");
                          break;
                case '+': System.out.print("and ");
                          break;
                case ')': System.out.print("\n\t\tOR\n\t");
                          i++;
                          break;
                default: System.out.print("");
            }
        }
        
    }
    
    public void stepMessage() { System.out.println("Your current Asthma Management Step: " + getStep()); };
    
    private int getAgeRange(int age) {
        if(age <= 4) return AgeRange.FOUR.getVal();
        return (age <= 11) ? AgeRange.ELEVEN.getVal() : AgeRange.OLDER.getVal();
    }
    
    private int getAgeRange2(int age) {
        if(age <= 19) return AgeRange2.NINETEEN.getVal();
        if(age <= 39) return AgeRange2.THIRTY_NINE.getVal();
        return (age <= 59) ? AgeRange2.FIFTY_NINE.getVal() : AgeRange2.EIGHTY.getVal();
    }
    
    public String getName() { return name; }
    
    public int getAge() {
        LocalDate today = LocalDate.now();
        Period p = Period.between(dob, today);
        return p.getYears();
    }
    
    public int getInitSeverity() { return initSeverity; }
    
    private int getStep() { return step; }
    
    public void addDayEntry(LocalDate d, int a, int n, int i, int s, boolean e) { calendar.put(d, new DayEntry(a, n, i, s, e)); }
    
    public void addVisitEntry(LocalDate d, int fvc, int s) {
        visitCalendar.put(d, new VisitEntry(fvc, s));
        step = s;
        prevVisit = d;
    }
    
    private static int max(int[] arr) {
        int max = 0;
        for(int i : arr) max = Math.max(max, i);
        return max;
    }
}