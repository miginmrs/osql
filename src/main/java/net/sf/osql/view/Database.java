package net.sf.osql.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.exceptions.DialectException;
import org.w3c.dom.Document;

import java.util.*;
import java.util.function.Function;

public class Database {
    private final DialectProvider dialectProvider;

    @FunctionalInterface
    interface DialectProvider {
        Dialect get(String name) throws DialectException;
    }

    private final XmlViewer viewer;
    private final Map<String, Document> tableDocuments = new HashMap<>();
    private final Map<String, Object> tableLocks = new HashMap<>();
    private final List<Table> tables;
    private final Function<Document, String> stringify;

    Database(List<Table> tables, XmlViewer xmlViewer, DialectProvider dialectProvider, Function<Document, String> stringify) {
        this.viewer = xmlViewer;
        this.dialectProvider = dialectProvider;
        this.tables = tables;
        this.stringify = stringify;
    }

    public DbView use(String name) throws DialectException {
        return new DbView(this, dialectProvider.get(name));
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    Document getTableDocument(Table table) {
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

    public String getTables() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<tables>\n"+tables.stream().map(this::getTableDocument).map(stringify).reduce("", String::concat)+"\n</tables>";
    }
}
