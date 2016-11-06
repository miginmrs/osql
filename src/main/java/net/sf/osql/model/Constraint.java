package net.sf.osql.model;


public class Constraint {

    public Constraint(Column column, Table references, boolean inner, int ibfk) {
        this.column = column;
        this.references = references;
        this.inner = inner;
        this.ibfk = ibfk;
    }
    public final Column column;
    public final Table references;
    public final boolean inner;
    public final int ibfk;
}
