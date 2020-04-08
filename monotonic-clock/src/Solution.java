import org.jetbrains.annotations.NotNull;

public class Solution implements MonotonicClock {
    private final RegularInt t1c1 = new RegularInt(0);
    private final RegularInt t1c2 = new RegularInt(0);
    private final RegularInt t1c3 = new RegularInt(0);

    private final RegularInt t2c1 = new RegularInt(0);
    private final RegularInt t2c2 = new RegularInt(0);
    private final RegularInt t2c3 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        t2c1.setValue(time.getD1());
        t2c2.setValue(time.getD2());
        t2c3.setValue(time.getD3());

        t1c3.setValue(t2c3.getValue());
        t1c2.setValue(t2c2.getValue());
        t1c1.setValue(t2c1.getValue());
    }

    @NotNull
    @Override
    public Time read() {
        Integer h1 = t1c1.getValue();
        Integer m1 = t1c2.getValue();
        Integer s1 = t1c3.getValue();

        int h2 = t2c1.getValue();
        int m2 = t2c2.getValue();
        int s2 = t2c3.getValue();

        if (!h1.equals(h2)) {
            return new Time(Math.max(h1, h2), 0, 0);
        } else if (!m1.equals(m2)) {
            return new Time(h1, Math.max(m1, m2), 0);
        } else if (!s1.equals(s2)) {
            return new Time(h1, m1, Math.max(s1, s2));
        } else {
            return new Time(h1, m1, s1);
        }
    }
}