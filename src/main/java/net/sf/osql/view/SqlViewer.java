package net.sf.osql.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.exceptions.DialectException;
import net.sf.xsltiny.TransformersBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlViewer extends Render<TableView, DbView, Dialect>{

    private static final TransformersBuilder.DocumentData context;
    private final TransformersBuilder transformer;

    static {
        try {
            context = TransformersBuilder.getContext(ClassLoader.getSystemResource("net/sf/osql/dialect.xml"));
        } catch (IOException e) {
            throw new Error("Unable to load context document");
        }
    }

    public SqlViewer(String mode) {
        try {
            context.setProperties(new HashMap<String, String>(){{
                put("mode", mode);
            }});
            transformer = new TransformersBuilder(context);
        } catch (TransformerConfigurationException | XPathExpressionException e) {
            throw new Error("Unable to load xsl transformer from system class path");
        }
        try {
            Render.register("sqlviewer-"+mode, this);
        } catch (NameAlreadyBoundException ignored) {
        }
    }

    private final Map<String, Dialect> dialects = new HashMap<>();
    private final Map<String, Object> dialectLocks = new HashMap<>();

    public IDatabase load(List<Table> tables) {
        Function<Document, String> stringify = Render::stringify;
        Database database = new Database(tables, new XmlViewer(builder, tables), stringify);
        adoptDatabase(database);
        return database;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IDatabase<DbView> adoptDatabase(Database database) {
        return new DbProxy<>(name -> { try {
            return new DbView(database, getDialect(name));
        } catch (NameNotFoundException e) {
            throw new DialectException(e.getMessage());
        }}, database);
    }

    @Override
    public synchronized void register(String name, Dialect dialect) throws NameAlreadyBoundException {
        if(dialects.putIfAbsent(name, dialect) != null)
            throw new NameAlreadyBoundException(name);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Override
    public Dialect getDialect(String name) throws NameNotFoundException {
        Dialect dialect = dialects.get(name);
        if (dialect != null)
            return dialect;
        Object lock;
        synchronized (this) {
            lock = dialectLocks.get(name);
            if (lock == null) {
                lock = new Object();
                dialectLocks.put(name, lock);
            }
        }
        synchronized (lock) {
            dialect = dialects.get(name);
            if (dialect == null) {
                try {
                    URL url = ClassLoader.getSystemResource("net/sf/osql/dialect/" + name + ".xml");
                    if(url == null)
                        throw new IOException();
                    dialect = new Dialect(transformer.getTransformers(TransformersBuilder.loadDocument(url)));
                    dialects.put(name, dialect);
                } catch (TransformerException | IOException e) {
                    try {
                        Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("net/sf/osql/dialect");
                        Set<String> registeredDialects = new HashSet<>();
                        while (resources.hasMoreElements()) {
                            String[] list = new File(resources.nextElement().toURI()).list();
                            assert list != null;
                            Stream.of(list)
                                    .filter(filename -> filename.endsWith(".xml"))
                                    .map(filename -> filename.substring(0, filename.length() - 4) + "\n")
                                    .collect(Collectors.toCollection(() -> registeredDialects));
                        }
                        String message;
                        if (registeredDialects.contains(name+"\n"))
                            message = "Problems with the dialect " + name + ", please choose another one\n" + e.getMessage() +"\n";
                        else message = "Dialect " + name + " not found\n";
                        message += registeredDialects.stream().reduce(String::concat)
                                .map(list -> "This is the list of registered dialects:\n" + list)
                                .orElse("There is no registered dialect");
                        throw new NameNotFoundException(message);
                    } catch (IOException | URISyntaxException ignored) {
                        throw new Error("Unable to access dialect folder");
                    }
                }
            }
            return dialect;
        }
    }
}
