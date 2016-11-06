package net.sf.osql.model;


public class EventData {
    public final String event;
    public final String code;
    public final boolean isAfter;

    public EventData(String time, String event, String code) {
        this.event = event;
        this.isAfter = "after".equals(time);
        this.code = code;
    }
    
}
