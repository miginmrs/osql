package net.sf.osql.view;

import net.sf.osql.model.Table;

public class DbView {
    private final Database database;
    private final Dialect dialect;


    DbView(Database database, Dialect dialect) {
        this.database = database;
        this.dialect = dialect;
    }

    public TableView render(Table table) {
        return dialect.new TableViewImpl(table, database.getTableDocument(table));
    }
}
