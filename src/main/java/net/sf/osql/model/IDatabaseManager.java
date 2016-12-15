package net.sf.osql.model;

import java.util.List;
import java.util.Map;

public interface IDatabaseManager {
    Map<String, ITableManager> getTableManagers();

    DollarValue.Context getDollarValueContext();

    Map<String, Table> getTables();

    Map<String, List<Column>> getWaiting();

    Map<String, Table> getInterfaces();
}
