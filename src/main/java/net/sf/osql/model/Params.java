package net.sf.osql.model;

import java.util.Collections;
import java.util.Map;

public class Params {
    public final Table type;
    public final Map<String, Object> params;
    public final boolean inherit;

    public Params(Table type, Map<String, Object> params, boolean inherit) {
        this.type = type;
        this.params = Collections.unmodifiableMap(params);
        this.inherit = inherit;
    }
    
}
