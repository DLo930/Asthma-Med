public class DayEntry implements java.io.Serializable {
    // sabas: SABA use frequency, nas: Nighttime awakenings, interf: interference with normal activity
    private int symptoms, nas, interf, sabas;
    private boolean exacs;
    public DayEntry(int a, int n, int i, int s, boolean e) {
        symptoms = a;
        nas = n;
        interf = i;
        sabas = s;
        exacs = e;
    }
    
    public int getSymptoms() { return symptoms; }
    public int getNAs() { return nas; }
    public int getInterf() { return interf; }
    public int getSABAs() { return sabas; }
    public boolean getExacs() { return exacs; }
}