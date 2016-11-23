package net.sf.osql.view;

public interface TableView extends ITableView {

    String showDefinition();

    String showConstraints();

    String showInsertions();

    String showTriggers();

    String showITable();

}
