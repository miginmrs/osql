package net.sf.osql.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.exceptions.DialectException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlViewer {
    private final DocumentBuilder builder;
    private final Document transformerDocument;
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final Transformer transformer;
    private final Transformer stringifier;
    public SqlViewer(String mode) {
        System.setProperty("javax.view.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error("Unable to create a document builder for XML usage");
        }
        try {
            transformerDocument = builder.parse(ClassLoader.getSystemResourceAsStream("net/sf/osql/transform.xsl"));
            if(mode != null) {
                transformerDocument.getElementsByTagName("mode").item(0).setTextContent(mode);
            }
            stringifier = transformerFactory.newTransformer();
            stringifier.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            stringifier.setOutputProperty(OutputKeys.METHOD, "xml");
            stringifier.setOutputProperty(OutputKeys.INDENT, "yes");
            stringifier.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer = transformerFactory.newTransformer(new DOMSource(transformerDocument));
        } catch (SAXException | IOException | TransformerConfigurationException e) {
            throw new Error("Unable to load xsl transformer from system class path");
        }
    }
    private final Map<String, Dialect> dialects = new HashMap<>();
    private final Map<String, Object> dialectLocks = new HashMap<>();

    private String stringify(Document document) {
        StringWriter writer = new StringWriter();
        try {
            stringifier.transform(new DOMSource(document), new StreamResult(writer));
        } catch (TransformerException e) {
            return null;
        }
        return writer.toString();
    }

    private Map<String, Transformer> getTransformers(InputStream dialect)
            throws TransformerException {
        Source dialectSource = new StreamSource(dialect);
        Document dialectDocument = builder.newDocument();
        transformer.transform(dialectSource, new DOMResult(dialectDocument));
        NodeList nodes = dialectDocument.getDocumentElement().getChildNodes();
        return new HashMap<String, Transformer>(){{
            NodeList sourceNodes = ((Element)transformerDocument.getElementsByTagName("root").item(0)).getElementsByTagName("xsl:call-template");
            for(int i = 0, n = nodes.getLength(); i < n ; i++) {
                Node node = nodes.item(0);
                Document document = builder.newDocument();
                document.adoptNode(node);
                document.appendChild(node);
                put(((Element)sourceNodes.item(i)).getAttribute("name"),transformerFactory.newTransformer(new DOMSource(document)));
            }
        }};

    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public Database load(List<Table> tables) {
        return new Database(tables, new XmlViewer(builder, tables), (name)->{
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
                        InputStream stream = ClassLoader.getSystemResourceAsStream("net/sf/osql/dialect/" + name + ".xml");
                        if (stream == null)
                            throw new IOException();
                        dialect = new Dialect(getTransformers(stream));
                        dialects.put(name, dialect);
                    } catch (TransformerException | IOException e) {
                        try {
                            Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("net/sf/osql/dialect");
                            Set<String> registeredDialects = new HashSet<>();
                            while (resources.hasMoreElements()) {
                                Stream.of(new File(resources.nextElement().toURI()).list())
                                        .filter(filename -> filename.endsWith(".xml"))
                                        .map(filename -> filename.substring(0, filename.length() - 4) + "\n")
                                        .collect(Collectors.toCollection(() -> registeredDialects));
                            }
                            String message;
                            if (registeredDialects.contains(name))
                                message = "problems with the dialect " + name + ", please choose another one\n";
                            else message = "dialect " + name + " not found\n";
                            message += registeredDialects.stream().reduce(String::concat)
                                    .map(list -> "this is the list of registered dialects:\n" + list)
                                    .orElse("there is no registered dialect");
                            throw new DialectException(message);
                        } catch (IOException | URISyntaxException ignored) {
                            throw new Error("Unable to access dialect folder");
                        }
                    }
                }
                return dialect;
            }
        }, this::stringify);
    }

}
