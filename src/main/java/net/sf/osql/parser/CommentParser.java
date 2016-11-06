package net.sf.osql.parser;

import java.util.function.Consumer;
import java.util.regex.Pattern;

class CommentParser implements Consumer<Parser> {
    private static final Pattern sqlPattern = Pattern.compile("\\s*#.*");

    @Override
    public void accept(Parser t) {
        try {
            while (true) {
                t.parseWithPattern(sqlPattern);
            }
        } catch (Exception ex) {
        }
    }
    
}
