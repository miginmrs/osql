package net.sf.osql.view;

import net.sf.osql.model.Table;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import java.util.List;
import java.util.Map;

public class Dialect {
    public final Transformer definition, constraints, insertions, triggers, itable;
    public final DocumentBuilder builder;

    public Dialect(DocumentBuilder builder, Map<String, Transformer> transformers) {
        definition = transformers.get("definition");
        constraints = transformers.get("constraints");
        insertions = transformers.get("insertions");
        triggers = transformers.get("triggers");
        itable = transformers.get("itable");
        this.builder = builder;
    }

    public Database load(List<Table> tables) {
        return new Database(this, new XmlViewer(builder, tables));
    }
}
