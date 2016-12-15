package net.sf.osql.view;

import net.sf.xsltiny.TransformersBuilder;

import javax.naming.NameNotFoundException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

public class SqlViewer extends AbstractRender<TableView, DbView, Dialect> {

    public SqlViewer(String mode) {
        super(mode);
    }

    @Override
    protected Dialect provideDialect(String name) throws IOException, TransformerException{
		URL url = ClassLoader.getSystemResource("net/sf/osql/dialect/" + name + ".xml");
		if(url == null)
			throw new IOException();
        return new Dialect(transformer.getTransformers(TransformersBuilder.loadDocument(url)));
    }

    @Override
    protected Set<String> getRegistredDialects() throws IOException, URISyntaxException {
        return getRegistredDialectsFromSystem("net/sf/osql/dialect");
    }

    @Override
    protected DbView provideDbView(Database database, String name) {
        try {
            return new DbView(database, getDialect(name));
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
