package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Map;

public abstract class AbstractTable implements ITableView{
    private final Table table;
    private final Document document;

    protected AbstractTable(Table table, Document document) {
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

    protected abstract Map<String, Transformer> getTransformers();

    @Override
    public String show(String using) {
        Transformer transformer = getTransformers().get(using);
        if(transformer==null) throw new IllegalArgumentException("There is no section with name " + using + " in the dialect");
        return applyTransformer(transformer);
    }

    @Override
    public Table getTable() {
        return table;
    }

}
