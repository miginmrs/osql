package net.sf.osql.view;

import net.sf.osql.model.Table;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class Render<T extends ITableView, V extends IDbView<T>, D extends IDialect<T>> {
    private static Map<String, Render> registredRenders = new HashMap<>();
    protected static final TransformerFactory transformerFactory;
    protected static final Transformer stringifier;
    protected static final DocumentBuilder builder;
    protected static final SchemaFactory schemaFactory;
    protected static class SimpleErrorHandler implements ErrorHandler{
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            System.err.println(exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
    static {
        System.setProperty("javax.view.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        transformerFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error("Unable to create a document builder for XML usage");
        }
        try {
            stringifier = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new Error("Unable to configure xsl transformer");
        }
        stringifier.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        stringifier.setOutputProperty(OutputKeys.METHOD, "xml");
        stringifier.setOutputProperty(OutputKeys.INDENT, "yes");
        stringifier.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    }
    public static synchronized void register(String name, Render render) throws NameAlreadyBoundException {
        if(registredRenders.putIfAbsent(name, render) != null)
            throw new NameAlreadyBoundException(name);
    }

    public static String stringify(Document document) {
        StringWriter writer = new StringWriter();
        try {
            stringifier.transform(new DOMSource(document), new StreamResult(writer));
        } catch (TransformerException e) {
            return null;
        }
        return writer.toString();

    }

    public abstract void register(String name, D dialect) throws NameAlreadyBoundException;

    public abstract D getDialect(String name) throws NameNotFoundException;

    public static Database loadDatabase(List<Table> tables) {
        Function<Document, String> stringifier = Render::stringify;
        return new Database(tables, new XmlViewer(builder, tables), stringifier);
    }

    public abstract IDatabase<V> adoptDatabase(Database database);
}
