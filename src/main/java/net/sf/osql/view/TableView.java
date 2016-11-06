package net.sf.osql.view;

import net.sf.osql.model.Table;

public interface TableView {
    String showDefinition();

    String showConstraints();

    String showInsertions();

    String showTriggers();

    String showITable();

    Table getTable();
}
