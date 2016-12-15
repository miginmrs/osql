package net.sf.osql.parser;

import net.sf.osql.model.*;

import static net.sf.osql.model.Table.Event;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.osql.parser.exceptions.ParseException;
import net.sf.osql.parser.exceptions.TypeException;

public class TableParser implements Function<Parser, Table> {
    private static final String idType = "int";
    private static final String sqlID = "sql";
    private static final String nameID = "name";
    private static final String valueID = "value";
    private static final String fromID = "from";
    private static final String typeID = "type";
    private static final String nextID = "next";
    private static final String endID = "end";
    private static final String starID = "star";
    private static final String abstractID = "abstract";
    private static final String dependentID = "dependent";
    private static final String interfacesID = "interfaces";
    private static final Pattern sqlPattern = Pattern.compile("(?:\\s*sql\\s+'''(?<" + sqlID + ">.*?)''')?", Pattern.DOTALL);
    private static final Pattern paramsStartPattern = Pattern.compile("\\s*@(?<" + nameID + ">\\w+)(?<" + starID + ">\\*)?\\s*\\(" + "(?<" + endID + ">\\))?", Pattern.DOTALL);
    private static final Pattern paramPattern = Pattern.compile("\\s*(?<" + nameID + ">\\w+)\\s*=\\s*"
            + "(?<" + valueID + ">(?:[^'\"#()]|'(?:[^'\\\\]|\\\\.)*')+?)"
            + "\\s*(?:(?<" + nextID + ">,)|\\))", Pattern.DOTALL);
    private static final Pattern startPattern = Pattern.compile(
            "\\s*(?:(?<" + abstractID + ">abstract)\\s+)?"
                    +"\\s*(?:(?<" + dependentID + ">ref)\\s+)?"
                    + "(?<" + typeID + ">\\S+)\\s+(?<" + nameID + ">\\w+)"
                    + "(?:\\s+from\\s+(?<" + fromID + ">.*?))?"
                    + "(?:\\s+uses\\s+(?<" + interfacesID + ">.*?))?"
                    + "\\s*[{]", Pattern.DOTALL);
    private static final Pattern endPattern = Pattern.compile("\\s*[}]");
    private static final EventParser eventParser = new EventParser();
    private static final CommentParser commentParser = new CommentParser();

    private final String type;
    private final FieldParser fieldParser;
    private final boolean isAbstract;
    private final IDatabaseManager database;

    public TableParser(String type, FieldParser fieldParser, boolean isAbstract, IDatabaseManager database) {
        this.type = type;
        this.fieldParser = fieldParser;
        this.isAbstract = isAbstract;
        this.database = database;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public Table apply(Parser t) {
        int point = t.mark();
        int local = point;
        Exception fieldException = null;
        Exception eventException = null;
        Exception insertionException = null;
        try {
            commentParser.accept(t);
            Matcher m = t.parseWithPattern(sqlPattern);
            String sql = m.group(sqlID);
            Map<String, Params> paramsMap = new HashMap<>();
            Map<String, Map<String, Object>> paramsValues = new HashMap<>();
            while (true) {
                try {
                    commentParser.accept(t);
                    m = t.parseWithPattern(paramsStartPattern);
                } catch (Exception e) {
                    break;
                }
                local = t.mark();
                String paramsName = m.group(nameID);
                Table paramsTable = database.getInterfaces().get(paramsName);
                if (paramsTable == null) {
                    throw new ParseException("Params table " + paramsName + " not fount");
                }
                Map<String, Object> params = new HashMap<>();
                if (paramsMap.put(paramsTable.name, new Params(paramsTable, params, m.group(starID) == null)) != null) {
                    throw new ParseException("duplicated @" + paramsTable + " declaration");
                }
                paramsValues.put(paramsTable.name, params);
                if (m.group(endID) == null) {
                    do {
                        commentParser.accept(t);
                        m = t.parseWithPattern(paramPattern);
                        String value = m.group(valueID);
                        Object instructionValue;
                        if (value.startsWith("$")) {
                            instructionValue = DollarValue.get(database.getDollarValueContext(), value.substring(1));
                        } else {
                            instructionValue = value;
                        }
                        params.put(m.group(nameID), instructionValue);
                    } while (m.group(nextID) != null);
                }
            }
            commentParser.accept(t);
            local = t.mark();
            m = t.parseWithPattern(startPattern);
            if (!type.equals(m.group(typeID))) {
                try {
                    t.throwException(type + " expected but " + m.group(typeID) + " found");
                } catch (Exception e) {
                    throw new TypeException(e);
                }
            }
            String name = m.group(nameID);
            String from = m.group(fromID);
            if (database.getTableManagers().containsKey(name)) {
                throw new Exception("table " + name + " already defined");
            }
            Table fromTable = null;
            if (from != null) {
                fromTable = (isAbstract ? database.getInterfaces() : database.getTables()).get(from);
            }
            if (from != null && fromTable == null) {
                throw new Exception(from + " not found");
            }
            // inherit params for table
            if (fromTable != null) {
                for (Map.Entry<String, Params> inherited : fromTable.paramsMap.entrySet()) {
                    if (!inherited.getValue().inherit) {
                        continue;
                    }
                    Params params = paramsMap.get(inherited.getKey());
                    if (params == null) {
                        HashMap<String, Object> map = new HashMap<>();
                        params = new Params(inherited.getValue().type, map, inherited.getValue().inherit);
                        paramsMap.put(inherited.getKey(), params);
                        paramsValues.put(inherited.getKey(), map);
                    }
                    for (Map.Entry<String, Object> param : inherited.getValue().params.entrySet()) {
                        if (params.type.columns.get(param.getKey()).isInherit()) {
                            paramsValues.get(inherited.getKey()).putIfAbsent(param.getKey(), param.getValue());
                        }
                    }
                }
            }
            for (Params params : paramsMap.values()) {
                List<String> toBePresent = params.type.columns.values().stream()
                        .filter(col -> !col.nullable && col.defaultValue == null)
                        .map(col -> col.sname)
                        .collect(Collectors.toCollection(LinkedList::new));
                for (String param : toBePresent) {
                    if (!params.params.containsKey(param)) {
                        throw new ParseException("Inherited params field " + param + " defined for " + params.type.name + " is not initialised");
                    }
                }
                database.getTableManagers().get(params.type.name).getUsers()[0]++;
            }
            Map<String, Column> columns = new HashMap<>();
            Map<String, Table> interfaces = new HashMap<>();
            List<InsertBlock> insertions = new LinkedList<>();
            Event insert = new Event();
            Event update = new Event();
            Event delete = new Event();
            ITableManager tableManager = new TableManager(fromTable != null, fromTable == null ? new int[]{0} : database.getTableManagers().get(fromTable.name).getAuto_inc());
            database.getTableManagers().put(name, tableManager);
            Map<String, List<Column>> keys = tableManager.getKeys();
            Map<String, List<Column>> uniques = tableManager.getUniques();
            Table table = new Table(
                    sql,
                    name,
                    isAbstract || m.group(abstractID) != null,
                    m.group(dependentID) != null,
                    fromTable,
                    insert,
                    update,
                    delete,
                    tableManager.getIbfk(),
                    tableManager.getAuto_inc(),
                    tableManager.getUsers(),
                    insertions,
                    tableManager.getConstraints(),
                    tableManager.getExterns(),
                    tableManager.getSubtypes(),
                    tableManager.getHardlinks(),
                    interfaces,
                    columns,
                    paramsMap,
                    tableManager.getKernel()
            );
            String interfaceNames = m.group(interfacesID);
            if (interfaceNames != null) {
                for (String interfaceName : interfaceNames.split("\\s*,\\s*")) {
                    Table interf = database.getInterfaces().get(interfaceName);
                    if (interf == null) {
                        throw new ParseException("Interface " + interfaceName + " used for " + name + " is not found");
                    }
                    interfaces.put(interfaceName, interf);
                }
            }
            if (fromTable != null) {
                fromTable.columns.values().forEach(column -> {
                    columns.put(column.sname, column.clone(table));
                    tableManager.getKernels().put(column.sname, database.getTableManagers().get(from).getKernels().get(column.sname));
                });
                fromTable.constraints.stream().filter(c->c.column.isComposition() && c.column.isInherit()).forEach(constraint->
                    tableManager.getConstraints().add(new Constraint(constraint.column, constraint.references, true, ++tableManager.getIbfk()[0]))
                );
            }
            InsertBlockParser blockParser = new InsertBlockParser(table, database.getDollarValueContext(), tableManager.getAuto_inc());
            //fields
            if (fromTable != null) {
                Map<String, List<Column>> in, out;
                // add inherited indexes
                for(in = fromTable.keys, out = keys; out != uniques; in = fromTable.uniques, out = uniques)
                for(Map.Entry<String, List<Column>> key : in.entrySet()){
                    List<Column> keyColumns = new LinkedList<>();
                    for(Column col:key.getValue()) {
                        Column cloned = columns.get(col.sname);
                        if(!cloned.isPresent()) {
                            keyColumns=null;
                            break;
                        }
                        keyColumns.add(cloned);
                    }
                    if(keyColumns!=null) out.put(key.getKey(), keyColumns);
                }
            }
            try {
                while (true) {
                    Field field;
                    try {
                        commentParser.accept(t);
                        field = fieldParser.apply(t);
                    } catch (Exception e) {
                        String msg = "\tat " + String.join("\n\tat ", (CharSequence[]) Arrays.stream(e.getCause().getStackTrace()).map(Object::toString).toArray(String[]::new));
                        throw new ParseException(e.getCause().getMessage() + "\n" + msg);
                    }
                    Column.Kernel kernel = new Column.Kernel(field.name, field.type);
                    Column cloned = table.columns.get(field.name);
                    Column col = new Column(kernel, field.nullable, field.composition, field.isunique && field.keyname == null, table, field.defaultValue, field.comment, field.inherited, cloned != null, true, this.isAbstract);
                    Table typeTable = database.getTables().get(field.type);
                    if (typeTable != null) {
                        kernel.name = kernel.type + "." + col.sname;
                        kernel.type = idType;
                        kernel.table = typeTable;
                        if(col.isComposition())
                            if(typeTable.dependent) {
                                Table referenced = typeTable;
                                do  {
                                    database.getTableManagers().get(referenced.name).getHardlinks().add(col);
                                    referenced = referenced.from;
                                } while(referenced!=null && referenced.dependent);
                            }
                            else throw new ParseException("Table '"+typeTable.name+"' is not dependent");
                        tableManager.getConstraints().add(new Constraint(col, typeTable, true, ++tableManager.getIbfk()[0]));
                    }
                    if(cloned != null) {
                        if (!cloned.isPresent()) {
                            throw new Exception("Column " + col.sname + " cannot be redifined");
                        }
                        if (cloned.isNew()) {
                            throw new Exception("Column " + col.sname + " is already defined in this scope");
                        }
                    }
                    columns.put(col.sname, col);
                    tableManager.getKernels().put(col.sname, kernel);
                    if (field.keyname != null) {
                        Map<String, List<Column>> map = field.isunique ? uniques : keys;
                        map.computeIfAbsent(field.keyname, k -> new LinkedList<>()).add(col);
                    }
                }
            } catch (ParseException e) {
                fieldException = e;
            }
            if(table.dependent) {
                Column.Kernel $ref = new Column.Kernel("$ref", "text");
                Column.Kernel $rid = new Column.Kernel("$rid", idType);
                columns.put("$ref", new Column($ref, false, false, false, table, null, null, false, false, false, isAbstract));
                columns.put("$rid", new Column($rid, false, false, false, table, null, null, false, false, false, isAbstract));
                tableManager.getKernels().put("$ref", $ref);
                tableManager.getKernels().put("$rid", $rid);
            }
            for (Table interf : interfaces.values()) {
                for (Map.Entry<String, Column> entry : interf.columns.entrySet()) {
                    String colName = entry.getKey();
                    Column col = entry.getValue().implement(table);
                    Column defined = columns.putIfAbsent(colName, col);
                    if(defined == null) {
                        tableManager.getKernels().put(col.sname, database.getTableManagers().get(interf.name).getKernels().get(col.sname));
                    } else if(!defined.isPresent()) {
                        throw new ParseException("Declared field " + colName + " in interface " + interf.name + " is disallowed in table" + name);
                    }
                }
            }
            //insertions
            try {
                while (true) {
                    commentParser.accept(t);
                    InsertBlock insertBlock = blockParser.apply(t);
                    fieldException = null;
                    eventException = null;
                    insertions.add(insertBlock);
                }
            } catch (Exception e) {
                try {
                    throw e.getCause();
                } catch (ParseException ex) {
                    throw e;
                } catch (Exception ex) {
                    insertionException = ex;
                } catch (Throwable ignored) {
                }
            }
            //events
            try {
                while (true) {
                    commentParser.accept(t);
                    EventData eventData = eventParser.apply(t);
                    fieldException = null;
                    switch (eventData.event) {
                        case "insert":
                            if (eventData.isAfter) {
                                insert.after = eventData.code;
                            } else {
                                insert.before = eventData.code;
                            }
                            break;
                        case "update":
                            if (eventData.isAfter) {
                                update.after = eventData.code;
                            } else {
                                update.before = eventData.code;
                            }
                            break;
                        case "delete":
                            if (eventData.isAfter) {
                                delete.after = eventData.code;
                            } else {
                                delete.before = eventData.code;
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                eventException = e;
            }
            //end
            t.parseWithPattern(endPattern);
            //created
            if (fromTable != null) {
                database.getTableManagers().get(from).getSubtypes().add(table);
            }
            for (Map.Entry<String, Column> entry : columns.entrySet()) {
                Column col = entry.getValue();
                String colName = entry.getKey();
                Column.Kernel kernel = tableManager.getKernels().get(colName);
                if((col.isComposition() || col.isNew()) && kernel.table == null) {
                    database.getWaiting()
                            .computeIfAbsent(kernel.type, k -> new LinkedList<>())
                            .add(col);
                }
            }
            List<Column> waitingForMe = database.getWaiting().remove(name);
            if (waitingForMe != null) {
                for (Column col : waitingForMe) {
                    assert col.isNew() || col.isComposition();
                    Column.Kernel kernel = database.getTableManagers().get(col.getContainer().name).getKernels().get(col.sname);
                    if(kernel.table == null) {
                        kernel.name = kernel.type + "." + col.sname;
                        kernel.type = idType;
                        kernel.table = table;
                    }
                    if(col.isComposition())
                        if(table.dependent)
                            tableManager.getHardlinks().add(col);
                        else throw new ParseException("Table '"+table.name+"' is not dependent");
                    ITableManager manager = database.getTableManagers().get(col.getContainer().name);
                    Constraint constraint = new Constraint(col, table, false, ++manager.getIbfk()[0]);
                    manager.getConstraints().add(constraint);
                    manager.getExterns().add(constraint);
                }
            }
            (isAbstract ? database.getInterfaces() : database.getTables()).put(name, table);
            return table;
        } catch (RuntimeException ex) {
            t.rewind(point);
            throw ex;
        } catch (ParseException ex) {
            t.rewind(point);
            throw new RuntimeException(t.getException(ex.getMessage(), local), ex);
        } catch (Exception ex) {
            t.rewind(point);
            String err = "";
            if (fieldException != null) {
                err += "\nfield : " + fieldException.getMessage();
            }
            if (eventException != null) {
                err += "\nevent : " + eventException.getMessage();
            }
            if (insertionException != null) {
                err += "\ninsertion : " + insertionException.getMessage();
            }
			if(err.length()==0) err = ex.getMessage();
            throw new RuntimeException(err, ex);
        }
    }

}
