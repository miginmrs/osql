package net.sf.osql.view;

import net.sf.osql.model.Table;

public class DbView implements IDbView<TableView> {
    private final Database database;
    private final Dialect dialect;


    DbView(Database database, Dialect dialect) {
        this.database = database;
        this.dialect = dialect;
    }

    @Override
    public TableView render(Table table) {
        return dialect.render(table, database.getTableDocument(table));
    }
}
