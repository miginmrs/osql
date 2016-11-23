package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map;

class Dialect implements IDialect<TableView> {
    private static final String DEFINITION = "definition", CONSTRAINTS="constraints", INSERTION="insertion",
            TRIGGERS="triggers", ITABLE="itable";
    private final Map<String, Transformer> transformers;

    Dialect(Map<String, Transformer> transformers) {
        this.transformers = transformers;
    }

    @Override
    public TableView render(Table table, Document document) {
        return new TableViewImpl(table, document);
    }

    private class TableViewImpl implements TableView {
        private final Table table;
        private final Document document;

        private TableViewImpl(Table table, Document document) {
            this.table = table;
            this.document = document;
        }

        private String applyTransformer(Transformer transformer) {
            StringWriter writer = new StringWriter();
            try {
                transformer.transform(new DOMSource(document), new StreamResult(writer));
            } catch (TransformerException e) {
                throw new RuntimeException(e.getMessage());
            }
            return writer.getBuffer().toString();
        }

        @Override
        public String showDefinition() { return show(DEFINITION); }

        @Override
        public String showConstraints() {
            return show(CONSTRAINTS);
        }

        @Override
        public String showInsertions() { return show(INSERTION); }

        @Override
        public String showTriggers() {
            return show(TRIGGERS);
        }

        @Override
        public String showITable() {
            return show(ITABLE);
        }

        @Override
        public String show(String using) {
            Transformer transformer = transformers.get(using);
            if(transformer==null) throw new IllegalArgumentException("There is no section with name " + using + " in the dialect");
            return applyTransformer(transformer);
        }

        @Override
        public Table getTable() {
            return table;
        }

    }
}
