package net.sf.osql.view;

import net.sf.osql.view.exceptions.DialectException;

public interface IDatabase<V extends IDbView> {
    V use(String name) throws DialectException;

    String getTables();
}
