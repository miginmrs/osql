package net.sf.osql.view;

import net.sf.osql.view.exceptions.DialectException;

public class DbProxy<T extends ITableView, V extends IDbView<T>> implements IDatabase<V> {
    private final IDbViewProvider<V> dbViewProvider;
    private final Database db;

    public DbProxy(IDbViewProvider<V> dbViewProvider, Database db) {
        this.dbViewProvider = dbViewProvider;
        this.db = db;
    }

    @Override
    public V use(String name) throws DialectException {
        return dbViewProvider.get(name);
    }

    @Override
    public String getTables() {
        return db.getTables();
    }
}
