package net.sf.osql.view;

import net.sf.osql.view.exceptions.DialectException;

@FunctionalInterface
public interface IDbViewProvider<V extends IDbView>{
    V get(String name) throws DialectException;
}
