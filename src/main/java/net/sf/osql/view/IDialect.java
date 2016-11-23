package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;

public interface IDialect<T extends ITableView> {
    T render(Table table, Document document);
}
