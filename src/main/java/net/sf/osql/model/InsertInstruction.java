package net.sf.osql.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class InsertInstruction {
    public final List<Object> values;
    public final DollarValue into;

    public InsertInstruction(List<Object> values, DollarValue into) {
        this.values = Collections.unmodifiableList(values);
        this.into = into;
    }
}
