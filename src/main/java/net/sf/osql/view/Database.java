package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class Database {
    private final Dialect dialect;
    private final XmlViewer viewer;

    private class TableViewImpl implements TableView {
        private final Table table;
        private final Document document;

        public TableViewImpl(Table table, Document document) {
            this.table = table;
            this.document = document;
        }

        private String applyTransformer(Transformer transformer) {
            StringWriter writer = new StringWriter();
            try {
                transformer.transform(new DOMSource(document), new StreamResult(writer));
            } catch (TransformerException e) {
                return null;
            }
            return writer.getBuffer().toString();
        }

        @Override
        public String showDefinition() {
            return applyTransformer(dialect.definition);
        }

        @Override
        public String showConstraints() {
            return applyTransformer(dialect.constraints);
        }

        @Override
        public String showInsertions() {
            return applyTransformer(dialect.insertions);
        }

        @Override
        public String showTriggers() {
            return applyTransformer(dialect.triggers);
        }

        @Override
        public String showITable() {
            return applyTransformer(dialect.itable);
        }

        @Override
        public Table getTable() { return table; }

    }

    public Database(Dialect dialect, XmlViewer viewer) {
        this.dialect = dialect;
        this.viewer = viewer;
    }

    public TableView render(Table table) {
        return new TableViewImpl(table, viewer.apply(table));
    }
}
