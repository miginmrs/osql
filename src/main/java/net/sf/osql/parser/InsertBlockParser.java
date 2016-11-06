package net.sf.osql.parser;

import net.sf.osql.model.DollarValue;
import net.sf.osql.model.ParamsValue;
import net.sf.osql.model.Table;
import net.sf.osql.model.InsertInstruction;
import net.sf.osql.model.InsertBlock;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.osql.parser.exceptions.ParseException;

class InsertBlockParser implements Function<Parser, InsertBlock> {
    private static final String nameID = "name";
    private static final String nextID = "next";
    private static final String valueID = "value";
    private static final String newID = "new";
    private static final Pattern intoPattern = Pattern.compile("\\s*into\\s*\\(");
    private static final Pattern namePattern = Pattern.compile("\\s*(?<" + nameID + ">\\$?\\w+)(?<" + nextID + ">\\s*,)?");
    private static final Pattern insertPattern = Pattern.compile("\\s*\\)\\s*insert");
    private static final Pattern valuePattern = Pattern.compile("\\s*(?<" + valueID + ">(?:[^'\"#()]|'(?:[^'\\\\]|\\\\.)*')+?)" + "\\s*(?:(?<" + nextID + ">,)|\\)\\s*((?<" + newID + ">,)|;))", Pattern.DOTALL);
    private static final Pattern insertionPattern = Pattern.compile("\\s*(?:(?<=\\s|,)\\$(?<" + nameID + ">\\w+)\\s*=\\s*)?\\(");
    private final Table table;
    private final DollarValue.Context context;
    private final int[] auto_inc;

    public InsertBlockParser(Table table, DollarValue.Context context, int[] auto_inc) {
        this.table = table;
        this.context = context;
        this.auto_inc = auto_inc;
    }

    @Override
    public InsertBlock apply(Parser t) {
        int point = t.mark();
        int local = point;
        try {
            t.parseWithPattern(intoPattern);
            boolean next;
            Table paramsTable = null;
            List<String> columns = new LinkedList<>();
            List<InsertInstruction> instructions = new LinkedList<>();
            do {
                Matcher m;
                m = t.parseWithPattern(namePattern);
                String column = m.group(nameID);
                if (table.columns.get(column) == null) {
                    throw new ParseException(column + " undefined for " + table.name);
                }
                columns.add(column);
                next = m.group(nextID) != null;
            } while (next);
            t.parseWithPattern(insertPattern);
            local = t.mark();
            Matcher m = t.parseWithPattern(insertionPattern);
            boolean paramsSet = false;
            boolean multiple = false;
            while (true) {
                String into = m.group(nameID);
                DollarValue dollarValue;
                ++auto_inc[0];
                if (into != null) {
                    dollarValue = DollarValue.get(context, into);
                    if (dollarValue.getValue() != null) {
                        throw new ParseException("multiple definition of dollar value $" + into);
                    }
                    dollarValue.create(table);
                } else {
                    dollarValue = null;
                }
                List<Object> values = new LinkedList<>();
                InsertInstruction instruction = new InsertInstruction(values, dollarValue);
                do {
                    m = t.parseWithPattern(valuePattern);
                    String value = m.group(valueID);
                    Object instructionValue;
                    if (value.startsWith("$")) {
                        instructionValue = DollarValue.get(context, value.substring(1));
                    } else if (value.startsWith("@")) {
                        if (into != null) {
                            throw new ParseException("Unable to store mutiple insertions into one value");
                        }
                        if (paramsSet) {
                            throw new ParseException("Unable to set more than one params list" + " per insertion");
                        }
                        String interfaceName = value.substring(1);
                        Table interf = table.interfaces.get(interfaceName);
                        if (interf == null) {
                            throw new ParseException("Params interface " + interfaceName + " not used for table " + table.name);
                        }
                        instructionValue = new ParamsValue(interfaceName);
                        paramsTable = interf;
                        --auto_inc[0];
                        List<Table> paramsList = context.paramsList.get(table.root.name);
                        if(paramsList == null) {
                            paramsList = new LinkedList<>();
                            context.paramsList.put(table.root.name, paramsList);
                        }
                        paramsList.add(interf);
                        paramsSet = true;
                    } else {
                        instructionValue = value;
                    }
                    values.add(instructionValue);
                    next = m.group(nextID) != null;
                } while (next);
                if (paramsSet) {
                    if (columns.size() < instruction.values.size()) {
                        throw new ParseException("More values than columns");
                    }
                } else {
                    if (columns.size() != instruction.values.size()) {
                        throw new ParseException("Values and columns numbers don't match");
                    }
                }
                instructions.add(instruction);
                boolean hasNext = m.group(newID) != null;
                multiple |= hasNext;
                if (paramsSet && multiple) {
                    throw new ParseException("Unable to use more than one values list per" + " insertion when using params");
                }
                if (!hasNext) {
                    break;
                }
                local = t.mark();
                m = t.parseWithPattern(insertionPattern);
            }
            return new InsertBlock(table, columns, instructions, paramsTable);
        } catch (ParseException ex) {
            t.rewind(point);
            throw new RuntimeException(new ParseException(t.getException(ex.getMessage(), local)));
        } catch (Exception ex) {
            t.rewind(point);
            throw new RuntimeException(ex);
        }
    }
    
}
