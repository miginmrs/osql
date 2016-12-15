package net.sf.osql.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager implements IDatabaseManager {
    private final Map<String, ITableManager> tableManagers = new HashMap<>();
    private final DollarValue.Context dollarValueContext = new DollarValue.Context();
    private final Map<String, Table> tables = new HashMap<>();
    private final Map<String, List<Column>> waiting = new HashMap<>();
    private final Map<String, Table> interfaces = new HashMap<>();

    @Override
    public Map<String, ITableManager> getTableManagers() {
        return tableManagers;
    }

    @Override
    public DollarValue.Context getDollarValueContext() {
        return dollarValueContext;
    }

    @Override
    public Map<String, Table> getTables() {
        return tables;
    }

    @Override
    public Map<String, List<Column>> getWaiting() {
        return waiting;
    }

    @Override
    public Map<String, Table> getInterfaces() {
        return interfaces;
    }
}
