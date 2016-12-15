package net.sf.osql.model;

public class Column implements Cloneable {


    public static class Kernel {
        public String name;
        public String type;
        public Table table;

        public Kernel(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    public final String sname;
    public final boolean nullable;
    public final boolean unique;
    public final String defaultValue;
    public final String comment;
    public final Table definer;
    public final boolean isAbstract;

    private final Kernel k;
    private boolean _present;
    private boolean _new;
    private boolean _composition;
    private boolean _inherit;
    private Table _container;

    public Column(
            Kernel k,
            boolean nullable,
            boolean composition,
            boolean unique,
            Table containerTable,
            String defaultValue,
            String comment,
            boolean inherit,
            boolean isAbstract
    ) {
		this(k, nullable, composition, unique, containerTable, defaultValue, comment, inherit, false, true, isAbstract);
	}
	public Column(
            Kernel k,
            boolean nullable,
            boolean composition,
            boolean unique,
            Table containerTable,
            String defaultValue,
            String comment,
            boolean inherit,
            boolean redefined,
            boolean present,
            boolean isAbstract
    ) {
        this.sname = k.name;
        this.nullable = nullable;
        this._composition = composition;
        this.unique = unique;
        this._container = containerTable;
        this.definer = containerTable;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this._inherit = inherit;
        this.k = k;
        this._present = present;
        this._new = !redefined;
        this.isAbstract = isAbstract;
    }

    public boolean isPresent() {
        return _present;
    }

    public boolean isNew() {
        return _new;
    }

    public boolean isComposition() {
        return _composition;
    }

    public boolean isInherit() {
        return _inherit;
    }

    public Table getContainer() {
        return _container;
    }

    public String getName() {
        return k.name;
    }

    public String getType() {
        return k.type;
    }

    public Table getTable() { return k.table; }

    public Column implement(Table container) {
        return new Column(k,nullable,_composition,unique,container,defaultValue,comment,_inherit,!_new,_present,false);
    }

    public Column clone(Table container) {
        try {
            Column child = (Column) super.clone();
            child._new = false;
            child._container = container;
            if(!isAbstract && !_inherit) {
                child._present = false;
                child._inherit = false;
                child._composition = false;
            }
            return child;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
