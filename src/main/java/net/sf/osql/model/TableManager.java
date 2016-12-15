package net.sf.osql.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TableManager implements ITableManager {
    private final int[] ibfk;
    private final int[] auto_inc;
    private final int[] users;
    private final Map<String, Column.Kernel> kernels = new HashMap<>();
    private final Table.Kernel kernel;
    private final List<Constraint> constraints = new LinkedList<>();
    private final List<Constraint> externs = new LinkedList<>();
    private final List<Table> subtypes = new LinkedList<>();
    private final List<Column> hardlinks = new LinkedList<>();

    public TableManager(boolean child, int[] auto_inc) {
        this.ibfk = new int[]{child ? 1 : 0};
        this.auto_inc = auto_inc;
        this.users = new int[1];
        this.kernel = new Table.Kernel();
    }

    @Override
    public int[] getIbfk() {
        return ibfk;
    }

    @Override
    public int[] getAuto_inc() {
        return auto_inc;
    }

    @Override
    public int[] getUsers() {
        return users;
    }

    @Override
    public Map<String, Column.Kernel> getKernels() {
        return kernels;
    }

    @Override
    public Map<String, List<Column>> getKeys() {
        return kernel.keysManager;
    }

    @Override
    public Map<String, List<Column>> getUniques() {
        return kernel.uniquesManager;
    }

    @Override
    public List<Constraint> getConstraints() {
        return constraints;
    }

    @Override
    public List<Constraint> getExterns() {
        return externs;
    }

    @Override
    public List<Table> getSubtypes() {
        return subtypes;
    }

    @Override
    public List<Column> getHardlinks() {
        return hardlinks;
    }

    @Override
    public Table.Kernel getKernel() {
        return kernel;
    }
}
