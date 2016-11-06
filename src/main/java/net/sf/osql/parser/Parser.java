package net.sf.osql.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private final String dataString;
    private int next = 0;

    public Parser(String dataString) {
        this.dataString = dataString;
    }

    public Matcher parseWithPattern(Pattern pattern) throws Exception {
        Matcher matcher = pattern.matcher(dataString);
        if (!matcher.find(next) || matcher.start() != next) {
            throwException("Syntax Error");
        }
        next = matcher.end();
        return matcher;
    }

    public void seek(int distance) {
        next += distance;
    }

    public Character getChar() {
        if (next < dataString.length()) {
            return dataString.charAt(next);
        }
        return null;
    }

    public void rewind(int point) {
        next = point;
    }

    public int mark() {
        return next;
    }

    public void throwException(String cause) throws Exception {
        throw getException(cause);
    }

    public Exception getException(String cause) {
        return new Exception(getException(cause, next));
    }

    public String getException(String cause, int next) {
        return cause + " on position " + next + " near: " + dataString.substring(next).trim().split("\n", 2)[0];
    }
    
}
