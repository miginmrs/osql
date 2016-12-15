package net.sf.osql.model;

import java.util.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Table {

    public static class Event {
        public String before;
        public String after;

        public Event() {
        }
    }

    public final String sqldata;
    public final String name;
    public final boolean isAbstract;
    /**
     * A dependent table is a table that can't exist in the database without a hard link to it
     */
    public final boolean dependent;
    public final Table from;
    public final Table root;
    private final Event insert;
    private final Event update;
    private final Event delete;

    public static class Kernel {
        private class UnmodifiableListHashMap<K, V> extends HashMap<K, List<V>> {
            private Map<K, List<V>> unmodifiableListHashMap = new HashMap<>();
            @Override
            public List<V> put(K key, List<V> value) {
                unmodifiableListHashMap.put(key, Collections.unmodifiableList(value));
                return super.put(key, value);
            }
        }
        private final Map<String, List<Column>> keys;
        private final Map<String, List<Column>> uniques;
        public final Map<String, List<Column>> keysManager;
        public final Map<String, List<Column>> uniquesManager;

        public Kernel() {
            UnmodifiableListHashMap<String, Column> keys = new UnmodifiableListHashMap<>();
            UnmodifiableListHashMap<String, Column> uniques = new UnmodifiableListHashMap<>();
            this.keysManager = keys;
            this.uniquesManager = uniques;
            this.keys = Collections.unmodifiableMap(keys.unmodifiableListHashMap);
            this.uniques = Collections.unmodifiableMap(uniques.unmodifiableListHashMap);
        }
    }

    private final int[] ibfk;
    private final int[] auto_inc;
    private final int[] users;

    public final List<InsertBlock> insertions;
    public final List<Constraint> constraints;
    public final List<Constraint> externs;
    public final List<Table> subtypes;
    /**
     * A hard link is a link to a dependent table that can't be changed rather than set to null by delete of the linked table
     */
    public final List<Column> hardlinks;

    public final Map<String, Table> interfaces;
    public final Map<String, Column> columns;
    public final Map<String, Params> paramsMap;

    public final Map<String, List<Column>> keys;
    public final Map<String, List<Column>> uniques;

    public Table(
            String sqldata,
            String name,
            boolean isAbstract,
            boolean dependent,
            Table from,
            Event insert,
            Event update,
            Event delete,
            int[] ibfk,
            int[] auto_inc,
            int[] users,
            List<InsertBlock> insertions,
            List<Constraint> constraints,
            List<Constraint> externs,
            List<Table> subtypes,
            List<Column> hardlinks,
            Map<String, Table> interfaces,
            Map<String, Column> columns,
            Map<String, Params> paramsMap,
            Kernel kernel
    ) {
        this.sqldata = sqldata == null ? "" : sqldata + "\n";
        this.name = name;
        this.isAbstract = isAbstract;
        this.dependent = dependent;
        this.from = from;
        this.insert = insert;
        this.update = update;
        this.delete = delete;
        this.ibfk = ibfk;
        this.auto_inc = auto_inc;
        this.users = users;
        this.insertions = Collections.unmodifiableList(insertions);
        this.constraints = Collections.unmodifiableList(constraints);
        this.externs = Collections.unmodifiableList(externs);
        this.subtypes = Collections.unmodifiableList(subtypes);
        this.hardlinks = Collections.unmodifiableList(hardlinks);
        this.interfaces = Collections.unmodifiableMap(interfaces);
        this.columns = Collections.unmodifiableMap(columns);
        this.paramsMap = Collections.unmodifiableMap(paramsMap);
        this.keys = kernel.keys;
        this.uniques = kernel.uniques;
        this.root = from == null ? this : from.root;
    }

    public int getIbfk(){ return ibfk[0];}

    public int getAutoInc(){ return auto_inc[0];}

    public int getUsers(){ return users[0];}

    public String beforeInsert(){ return insert.before; }

    public String afterInsert(){ return insert.after; }

    public String beforeUpdate(){ return update.before; }

    public String afterUpdate(){ return update.after; }

    public String beforeDelete(){ return delete.before; }

    public String afterDelete(){ return delete.after; }

}
