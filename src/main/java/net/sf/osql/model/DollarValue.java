package net.sf.osql.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DollarValue {
    public static class Context {
        // Fly Weight Pattern
        public final Map<String, DollarValue> dollarValues = new HashMap<>();
        // Insertions to count when computing the current id per root table
        public final Map<String, List<Table>> paramsList = new HashMap<>();
    }

    private Context context;
    private List<Table> previousParamsList;

    private Integer value;
    public final String name;

    public static DollarValue get(Context context, String name) {
        DollarValue dollarValue = context.dollarValues.get(name);
        if (dollarValue == null) {
            dollarValue = new DollarValue(context, name);
            context.dollarValues.put(name, dollarValue);
        }
        return dollarValue;
    }

    /**
     * Initialise this variable by the current table auto_increment value
     * @param table the root table to calculate auto_increment value
     */
    public void create(Table table){
        if (value != null) {
            throw new IllegalStateException("Dollar value $" + name + " already defined");
        }
        value = table.getAutoInc();
        List<Table> list = context.paramsList.get(table.root.name);
        previousParamsList = list==null ? new LinkedList<>() : new LinkedList<>(list);
    }

    public Integer getValue() {
        return value;
    }

    private DollarValue(Context context, String name) {
        this.name = name;
        this.context = context;
    }

    @Override
    public String toString() {
        if (value == null) {
            throw new RuntimeException("variable $" + name + " not initialized");
        }
        int val = this.value;
        for (Table params : previousParamsList) {
            val += params.getUsers();
        }
        return "" + val;
    }
    
}
