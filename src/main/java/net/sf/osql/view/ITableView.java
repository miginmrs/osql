package net.sf.osql.view;

import net.sf.osql.model.Table;

public interface ITableView {
    String show(String view);
    Table getTable();
}
