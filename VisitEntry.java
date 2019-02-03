public class VisitEntry implements java.io.Serializable {
    //fev1fvc: FEV1/FVC ratio, step: Treatment step recommended during visit
    private int fev1fvc, step;
    public VisitEntry(int FEV1FVC, int s) {
        fev1fvc = FEV1FVC;
        step = s;
    }
    public int getFEV1FVC() { return fev1fvc; }
    public int getStep() { return step; }
}