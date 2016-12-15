package net.sf.osql.model;

import java.util.List;
import java.util.Map;

public interface ITableManager {
    int[] getIbfk();

    int[] getAuto_inc();

    int[] getUsers();

    Map<String, Column.Kernel> getKernels();

    Map<String, List<Column>> getKeys();

    Map<String, List<Column>> getUniques();

    List<Constraint> getConstraints();

    List<Constraint> getExterns();

    List<Table> getSubtypes();

    List<Column> getHardlinks();

    Table.Kernel getKernel();
}
