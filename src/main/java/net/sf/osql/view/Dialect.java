package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map;

class Dialect {
    private final Transformer definition, constraints, insertions, triggers, itable;

    Dialect(Map<String, Transformer> transformers) {
        definition = transformers.get("definition");
        constraints = transformers.get("constraints");
        insertions = transformers.get("insertions");
        triggers = transformers.get("triggers");
        itable = transformers.get("itable");
    }

    class TableViewImpl implements TableView {
        private final Table table;
        private final Document document;

        TableViewImpl(Table table, Document document) {
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
            return applyTransformer(definition);
        }

        @Override
        public String showConstraints() {
            return applyTransformer(constraints);
        }

        @Override
        public String showInsertions() {
            return applyTransformer(insertions);
        }

        @Override
        public String showTriggers() {
            return applyTransformer(triggers);
        }

        @Override
        public String showITable() {
            return applyTransformer(itable);
        }

        @Override
        public Table getTable() {
            return table;
        }

    }
}
