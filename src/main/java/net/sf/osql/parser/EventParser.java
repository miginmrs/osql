package net.sf.osql.parser;

import net.sf.osql.model.EventData;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EventParser implements Function<Parser, EventData> {
    private static final String timeID = "time";
    private static final String eventID = "event";
    private static final String codeID = "code";
    private static final Pattern eventPattern = Pattern.compile("" + "\\s*on\\s+(?<" + timeID + ">before|after)\\s+(?<" + eventID + ">insert|update|delete)" + "\\s*'''(?<" + codeID + ">.*?)'''", Pattern.DOTALL);

    @Override
    public EventData apply(Parser t) {
        int point = t.mark();
        try {
            Matcher m = t.parseWithPattern(eventPattern);
            return new EventData(m.group(timeID), m.group(eventID), "\tBEGIN"+m.group(codeID)+"\n\tEND;");
        } catch (Exception ex) {
            t.rewind(point);
            throw new RuntimeException(ex);
        }
    }
    
}
