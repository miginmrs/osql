package net.sf.osql.view;

import net.sf.osql.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

class XmlViewer implements Function<Table, Document> {
    private final DocumentBuilder builder;
    private final Collection<Table> tables;

    XmlViewer(DocumentBuilder builder, Collection<Table> tables) {
        this.builder = builder;
        this.tables = tables;
    }

    @Override
    public Document apply(Table table) {
        final Document document = builder.newDocument();
        Element root = document.createElement("table");
        root.setAttribute("name", table.name);
        if(table.from != null) root.setAttribute("from", table.from.name);
        if(table.isAbstract) root.setAttribute("abstract", "true");
        if(table.dependent) root.setAttribute("dependent", "true");
        Element sql = document.createElement("sql");
        sql.appendChild(document.createCDATASection(table.sqldata));
        root.appendChild(sql);
        Element subtypes = document.createElement("subtypes");
        table.subtypes.forEach(subtype -> {
            Element subtypeElement = document.createElement("subtype");
            subtypeElement.setAttribute("name", subtype.name);
            subtypes.appendChild(subtypeElement);
        });
        root.appendChild(subtypes);
        if(table.from != null) {
            Element siblings = document.createElement("siblings");
            table.from.subtypes.forEach(subtype -> {
                Element siblingElement = document.createElement("sibling");
                siblingElement.setAttribute("name", subtype.name);
                siblings.appendChild(siblingElement);
            });
            root.appendChild(siblings);
        }
        Element columns = document.createElement("columns");
        table.columns.forEach((sname, column) -> {
            Element columnElement = document.createElement("column");
            columnElement.setAttribute("name", column.getName());
            if(column.getTable() != null) columnElement.setAttribute("table", column.getTable().name);
            if(column.isPresent()) columnElement.setAttribute("present", "true");
            columnElement.setAttribute("type", column.getType());
            columnElement.setAttribute("definer", column.definer.name);
            if(column.isPresent()) {
                if(column.nullable) columnElement.setAttribute("null", "true");
                if(column.unique) columnElement.setAttribute("unique", "true");
                if(column.defaultValue != null) columnElement.setAttribute("default", column.defaultValue);
                if(column.comment != null) columnElement.setAttribute("comment", column.comment);
                if(column.isNew()) columnElement.setAttribute("new", "true");
                if(column.isComposition()) columnElement.setAttribute("composition", "true");
                if(!table.subtypes.isEmpty() && column.isInherit()) columnElement.setAttribute("inherit", "true");
            }
            columns.appendChild(columnElement);
        });
        root.appendChild(columns);
        Element links = document.createElement("links");
        table.hardlinks.forEach(column -> {
            Element linkElement = document.createElement("column");
            linkElement.setAttribute("sname", column.sname);
            linkElement.setAttribute("name", column.getName());
            linkElement.setAttribute("table", column.getContainer().name);
            links.appendChild(linkElement);
        });
        root.appendChild(links);
        new HashMap<String, Map<String, List<Column>>>(){{
            put("index", table.keys);
            put("unique", table.uniques);
        }}.forEach((tag, map)->{
            Element index = document.createElement(tag);
            map.forEach((name, keyColumns) -> {
                Element list = document.createElement("list");
                list.setAttribute("name", name);
                keyColumns.forEach(column -> {
                    Element columnElement = document.createElement("column");
                    columnElement.setAttribute("name", column.getName());
                    list.appendChild(columnElement);
                });
                index.appendChild(list);
            });
            root.appendChild(index);
        });

        Function<String, Element> nameEntry = (name) -> {
            Element element = document.createElement("entry");
            element.setAttribute("name", name);
            return element;
        };
        BiFunction<String, String, Element> entry = (name, value) -> {
            Element element = nameEntry.apply(name);
            element.setAttribute("value", value);
            return element;
        };
        BiFunction<String, String, Element> stringEntry = (name, value) -> {
            Element element = nameEntry.apply(name);
            element.setAttribute("string", value);
            return element;
        };

        Element path = document.createElement("path");
        Table parent = table;
        while (parent != null) {
            Element element = stringEntry.apply("_" + (parent.from == null ? "" : parent.from.name), parent.from == null ? table.name : parent.name);
            path.appendChild(element);
            parent = parent.from;
        }
        root.appendChild(path);
        Element insertions = document.createElement("insertions");
        insertions.setAttribute("into", table.root.name);
        table.insertions.forEach(insertBlock -> {
            if (insertBlock.paramsTable != null) {
                String paramsName = insertBlock.paramsTable.name;
                if (insertBlock.instructions.size() != 1) {
                    throw new RuntimeException("Insertion using params must have unique instruction");
                }
                InsertInstruction instruction = insertBlock.instructions.get(0);
                for (Table tab : tables) {
                    Element insertion = document.createElement("insertion");
                    Params params = tab.paramsMap.get(paramsName);
                    if (params == null) {
                        continue;
                    }
                    Iterator<Object> it = instruction.values.iterator();
                    Set<String> defined = new HashSet<>(table.columns.keySet());
                    for (String colName : insertBlock.columns) {
                        Object value = it.next();
                        if (value instanceof ParamsValue) {
                            insertion.appendChild(stringEntry.apply(colName, tab.name));
                            for (Map.Entry<String, Object> param : params.params.entrySet()) {
                                Column col = insertBlock.paramsTable.columns.get(param.getKey());
                                insertion.appendChild(entry.apply(col.getName(), param.getValue().toString()));
                                defined.remove(param.getKey());
                            }
                        } else {
                            insertion.appendChild(entry.apply(table.columns.get(colName).getName(), value.toString()));
                        }
                        defined.remove(colName);
                    }
                    for (String colName : defined) {
                        Column col = table.columns.get(colName);
                        if (col.defaultValue != null) {
                            continue;
                        }
                        insertion.appendChild(nameEntry.apply(table.columns.get(colName).getName()));
                    }
                    insertions.appendChild(insertion);
                }
            } else {
                for (InsertInstruction instruction : insertBlock.instructions) {
                    Element insertion = document.createElement("insertion");
                    Iterator<Object> it = instruction.values.iterator();
                    Set<String> defined = new HashSet<>(table.columns.keySet());
                    for (String colName : insertBlock.columns) {
                        insertion.appendChild(entry.apply(table.columns.get(colName).getName(), it.next().toString()));
                        defined.remove(colName);
                    }
                    for (String colName : defined) {
                        insertion.appendChild(nameEntry.apply(table.columns.get(colName).getName()));
                    }
                    insertions.appendChild(insertion);
                }
            }

        });
        root.appendChild(insertions);
        Element constraints = document.createElement("constraints");
        table.constraints.forEach(constraint -> {
            Element subtypeElement = document.createElement("constraint");
            subtypeElement.setAttribute("number", Integer.toString(constraint.ibfk));
            subtypeElement.setAttribute("reference", constraint.column.getName());
            subtypeElement.setAttribute("target", constraint.references.name);
            if(constraint.column.isComposition()) subtypeElement.setAttribute("composition", "true");
            constraints.appendChild(subtypeElement);
        });
        root.appendChild(constraints);
        Element triggers = document.createElement("triggers");
        new HashMap<String, String>(){{
            put("before:insert", table.beforeInsert());
            put("after:insert", table.afterInsert());
            put("before:update", table.beforeUpdate());
            put("after:update", table.afterUpdate());
            put("before:delete", table.beforeDelete());
            put("after:delete", table.afterDelete());
        }}.forEach((event, action) -> {
            if(action == null) return;
            Element trigger = document.createElement("trigger");
            trigger.setAttribute("event", event);
            trigger.appendChild(document.createCDATASection(action));
            triggers.appendChild(trigger);
        });
        root.appendChild(triggers);
        document.appendChild(root);
        return document;
    }
}
