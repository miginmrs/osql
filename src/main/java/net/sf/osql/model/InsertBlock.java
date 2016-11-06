package net.sf.osql.model;

import java.util.Collections;
import java.util.List;

public class InsertBlock {
    public final Table table;
    public final List<String> columns;
    public final List<InsertInstruction> instructions;
    public final Table paramsTable;

    public InsertBlock(Table table, List<String> columns, List<InsertInstruction> instructions, Table paramsTable) {
        this.table = table;
        this.columns = Collections.unmodifiableList(columns);
        this.instructions = Collections.unmodifiableList(instructions);
        this.paramsTable = paramsTable;
    }
}
