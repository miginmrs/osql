package net.sf.osql.parser;

import net.sf.osql.model.Field;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldParser implements Function<Parser, Field> {
    private static final String typeID = "type";
    private static final String nameID = "name";
    private static final String contentID = "content";
    private static final String keyID = "key";
    private static final String keynameID = "keyname";
    private static final String nullableID = "nullable";
    private static final String refID = "ref";
    private static final String defaultID = "default";
    private static final String commentID = "comment";
    private static final String inheritedID = "inherited";
    private static final Pattern typedFieldPattern = Pattern.compile(""
            + "(?<" + nullableID + ">\\s*null\\s)?" 
            + "(?<" + refID + ">\\s*ref\\s)?" 
            + "\\s*(?<" + typeID + ">\\w+(?:\\((?<" + contentID + ">([^'\"#]|'(?:[^'\\\\]|\\\\.)*')*?)\\))?)" 
            + "\\s+(?<" + nameID + ">\\w+)(?<" + inheritedID + ">\\*)?" 
            + "(?:\\s+(?<" + keyID + ">key|unique)(?:\\s+(?<" + keynameID + ">\\w+))?)?" 
            + "(?:\\s*=\\s*(?<" + defaultID + ">(?:[^'\"#]|'(?:[^'\\\\]|\\\\.)*')+?))?" 
            + "(?:\\s*\"(?<" + commentID + ">(?:[^\"\\\\]|\\\\\")*)\")?\\s*;", 
            Pattern.DOTALL);
    private static final Pattern untypedFieldPattern = Pattern.compile("" 
            + "(?<" + nullableID + ">\\s*null)?" 
            + "\\s*(?<" + nameID + ">\\w+)(?<" + inheritedID + ">\\*)?" 
            + "\\s*(?:(?<" + keyID + ">key|unique)(?:\\s+(?<" + keynameID + ">\\w+))?)?" 
            + "(?:\\s*=\\s*(?<" + defaultID + ">(?:[^'\"#]|'(?:[^'\\\\]|\\\\.)*')+?))?"
            + "(?:\\s*\"(?<" + commentID + ">(?:[^\"\\\\]|\\\\\")*)\")?\\s*;", 
            Pattern.DOTALL);
    private static final Pattern exitPattern = Pattern.compile("\\s*(?:on\\s|\\}|into\\s)", Pattern.DOTALL);
    private final boolean isTyped;

    public FieldParser(boolean isTyped) {
        this.isTyped = isTyped;
    }

    @Override
    public Field apply(Parser t) {
        int point = t.mark();
        boolean exit = false;
        try {
            try {
                t.parseWithPattern(exitPattern);
                exit = true;
            } catch (Exception e) {
            }
            if (exit) {
                throw new RuntimeException("End of fields block");
            }
            Matcher m = t.parseWithPattern(isTyped ? typedFieldPattern : untypedFieldPattern);
            String key = m.group(keyID);
            String name = m.group(nameID);
            String keyname = m.group(keynameID);
            if ("key".equals(key) && keyname == null) {
                keyname = name;
            }
            boolean nullable = m.group(nullableID) != null;
            boolean composition = isTyped && m.group(refID) != null;
            String defaultValue = m.group(defaultID);
            if (nullable && defaultValue != null && !"null".equals(defaultValue)) {
                throw new RuntimeException("default value '" + defaultValue + "' of nullable field must be null not");
            }
            return new Field(isTyped ? m.group(typeID) : null, nullable, composition, isTyped ? m.group(contentID) : null, name, "unique".equals(key), keyname, defaultValue, m.group(commentID), m.group(inheritedID) == null);
        } catch (Exception ex) {
            t.rewind(point);
            throw new RuntimeException(ex);
        }
    }
    
}
