package net.sf.osql.model;

public class Field {

    public Field(String type, boolean nullable, boolean composition, String content, String name, boolean isunique, String keyname, String defaultValue, String comment, boolean inherited) {
        this.type = type;
        this.nullable = nullable || composition;
        this.composition = composition;
        this.content = content;
        this.name = name;
        this.isunique = isunique;
        this.keyname = keyname;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.inherited = inherited;
    }
    public final String type;
    public final boolean nullable;
    public final boolean composition;
    public final String content;
    public final String name;
    public final boolean isunique;
    public final String keyname;
    public final String defaultValue;
    public final String comment;
    public final boolean inherited;
    
}
