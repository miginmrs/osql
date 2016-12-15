package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map;

public class Dialect implements IDialect<TableView> {
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

    private class TableViewImpl extends AbstractTable implements TableView {
        private TableViewImpl(Table table, Document document) {
            super(table, document);
        }

        @Override
        protected Map<String, Transformer> getTransformers() { return transformers; }

        @Override
        public String showDefinition() { return show(DEFINITION); }

        @Override
        public String showConstraints() { return show(CONSTRAINTS); }

        @Override
        public String showInsertions() { return show(INSERTION); }

        @Override
        public String showTriggers() { return show(TRIGGERS); }

        @Override
        public String showITable() { return show(ITABLE); }

    }
}
