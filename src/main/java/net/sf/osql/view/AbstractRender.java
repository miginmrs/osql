package net.sf.osql.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.exceptions.DialectException;
import net.sf.xsltiny.TransformersBuilder;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue"})
public abstract class AbstractRender<T extends ITableView, V extends IDbView<T>, D extends IDialect<T>> extends Render<T, V, D>{

    private static final TransformersBuilder.DocumentData context;
    protected final TransformersBuilder transformer;
    private final Map<String, Object> dialectLocks = new HashMap<>();
    private final Map<String, D> dialects = new HashMap<>();

    static {
        try {
            context = TransformersBuilder.getContext(ClassLoader.getSystemResource("net/sf/osql/dialect.xml"));
        } catch (Throwable e) {
            throw new Error("Unable to load context document");
        }
    }

    public AbstractRender(String mode) {
        try {
            context.setProperties(new HashMap<String, String>(){{
                put("mode", mode);
            }});
            transformer = new TransformersBuilder(context);
        } catch (TransformerConfigurationException | XPathExpressionException e) {
            throw new Error("Unable to load xsl transformer from system class path");
        }
        try {
            Render.register(getClass().getName()+"-"+mode, this);
        } catch (NameAlreadyBoundException ignored) {
        }
    }

    protected abstract V provideDbView(Database database, String name);
    protected abstract D provideDialect(String name) throws IOException, TransformerException;

    public IDatabase load(List<Table> tables) {
        Database database = new Database(tables, new XmlViewer(builder, tables), Render::stringify);
        adoptDatabase(database);
        return database;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IDatabase<V> adoptDatabase(Database database) {
        return new DbProxy<>(name -> {
            try{
                return provideDbView(database, name);
            } catch (RuntimeException e) {
                Throwable exception = e.getCause();
                if(exception != null && exception instanceof DialectException) throw (DialectException)exception;
                throw e;
            }
        }, database);
    }

    @Override
    public synchronized void register(String name, D dialect) throws NameAlreadyBoundException {
        if(dialects.putIfAbsent(name, dialect) != null)
            throw new NameAlreadyBoundException(name);
    }

    protected abstract Set<String> getRegistredDialects() throws IOException, URISyntaxException;
    protected Set<String> getRegistredDialectsFromSystem(String path) throws IOException, URISyntaxException {
        Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources(path+"/list");
        Set<String> registeredDialects = new HashSet<>();
        while (resources.hasMoreElements()) {
            String[] list = new java.util.Scanner(resources.nextElement().openStream()).useDelimiter("\\Z").next().split("(\r|\n)+");
            //noinspection ResultOfMethodCallIgnored
            Stream.of(list)
                    .map(filename -> filename + "\n")
                    .collect(Collectors.toCollection(() -> registeredDialects));
        }
        return registeredDialects;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Override
    public D getDialect(String name) throws NameNotFoundException {
        D dialect = dialects.get(name);
        if (dialect != null)
            return dialect;
        Object lock;
        synchronized (this) {
            lock = dialectLocks.computeIfAbsent(name, k -> new Object());
        }
        synchronized (lock) {
            dialect = dialects.get(name);
            if (dialect == null) {
                try {
                    dialect = provideDialect(name);
                    dialects.put(name, dialect);
                } catch (TransformerException | IOException e) {
                    try {
                        Set<String> registeredDialects = getRegistredDialects();
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
