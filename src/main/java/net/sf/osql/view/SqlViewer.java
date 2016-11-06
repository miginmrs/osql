package net.sf.osql.view;

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
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SqlViewer {
    private final DocumentBuilder builder;
    private final Document transformerDocument;
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final Transformer transformer;
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
            transformer = transformerFactory.newTransformer(new DOMSource(transformerDocument));
        } catch (SAXException | IOException | TransformerConfigurationException e) {
            throw new Error("Unable to load xsl transformer from system class path");
        }
    }
    private final Map<String, Dialect> dialects = new HashMap<>();

    private Map<String, Transformer> getTransformers(InputStream dialect)
            throws TransformerException, IOException, SAXException {
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

    public Dialect use(String name) {
        Dialect dialect = dialects.get(name);
        if(dialect != null)
            return dialect;
        synchronized (SqlViewer.class) {
            dialect = dialects.get(name);
            if(dialect == null) {
                try {
                    dialect = new Dialect(builder, getTransformers(ClassLoader.getSystemResourceAsStream("net/sf/osql/dialect/"+name+".xml")));
                    dialects.put(name, dialect);
                } catch (TransformerException | IOException | SAXException e) {
                }
            }
            return dialect;
        }
    }
}
