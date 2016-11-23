package net.sf.osql.view;

import net.sf.osql.model.Table;

public interface IDbView<T extends ITableView> {
    T render(Table table);
}
