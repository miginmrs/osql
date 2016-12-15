package net.sf.osql.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.exceptions.DialectException;
import org.w3c.dom.Document;

import java.util.*;
import java.util.function.Function;

public class Database implements IDatabase {

    private final XmlViewer viewer;
    private final Map<String, Document> tableDocuments = new HashMap<>();
    private final Map<String, Object> tableLocks = new HashMap<>();
    private final List<Table> tables;
    private final Function<Document, String> stringify;

    Database(List<Table> tables, XmlViewer xmlViewer, Function<Document, String> stringify) {
        this.viewer = xmlViewer;
        this.tables = tables;
        this.stringify = stringify;
    }

    @Override
    public IDbView use(String name) throws DialectException {
        throw new IllegalStateException();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public Document getTableDocument(Table table) {
        String name = table.name;
        Document document = tableDocuments.get(name);
        if (document != null)
            return document;
        Object lock;
        synchronized (this) {
            lock = tableLocks.get(name);
            if (lock == null) {
                lock = new Object();
                tableLocks.put(name, lock);
            }
        }
        synchronized (lock) {
            document = tableDocuments.get(name);
            if (document == null) {
                document = viewer.apply(table);
                tableDocuments.put(name, document);
            }
            return document;
        }
    }

    @Override
    public String getTables() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<tables>\n"+tables.stream().map(this::getTableDocument).map(stringify).reduce("", String::concat)+"\n</tables>";
    }
}
