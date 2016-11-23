package net.sf.osql;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.osql.model.Table;
import net.sf.osql.parser.FieldParser;
import net.sf.osql.parser.Parser;
import net.sf.osql.parser.TableParser;
import net.sf.osql.parser.exceptions.TypeException;
import net.sf.osql.view.*;
import net.sf.osql.view.exceptions.DialectException;
import org.apache.commons.cli.*;

public class Main {

    private static final Pattern END_PATTERN = Pattern.compile("\\s*$");
    private static List<Table> getTables(String content) {
        Parser t = new Parser(content);
        TableParser.Database database = new TableParser.Database();
        TableParser tableParser = new TableParser("class", new FieldParser(true), false, database);
        TableParser interfaceParser = new TableParser("@interface", new FieldParser(true), true, database);
        List<Table> tabs = new LinkedList<>();
        while(true) {
            try{
                try {
                    tabs.add(tableParser.apply(t));
                } catch(TypeException te) {
                    interfaceParser.apply(t);
                }
            } catch(RuntimeException re) {
                try {
                    t.parseWithPattern(END_PATTERN);
                    break;
                } catch(Exception e) {
                    throw re;
                }
            }
        }
        return tabs;
    }

    private static class Argument {
        final InputStream in;
        final PrintStream out;
        final String mode;
        final String dialect;
        final boolean xml;

        Argument(InputStream in, PrintStream out, String mode, String dialect, boolean xml) {
            this.in = in;
            this.out = out;
            this.mode = mode;
            this.dialect = dialect;
            this.xml = xml;
        }
    }

    private static Argument getArgument(String[] args){
        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter(){
            @Override
            public void printHelp(String cmdLineSyntax, Options options) {
                PrintWriter pw = new PrintWriter(System.err);
                this.printHelp(pw, 80, cmdLineSyntax, null, options, 1, 3, null, false);
                pw.flush();
            }
        };
        Options options = new Options();
        options.addOption(new Option("i", "input", true, "input file path"));
        options.addOption(new Option("o", "output", true, "output file path"));
        options.addOption(new Option("m", "mode", true, "database integrity mode (soft|hard)"));
        options.addOption(new Option("h", "help", false, "show this help"));
        options.addOption(new Option("x", "xml", false, "output xml rather than sql"));
        options.addOption(new Option("d", "dialect", true, "sql dialect used for output"));
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            if(!cmd.hasOption('h')) {
                if (!cmd.hasOption('m'))
                    throw new ParseException("Mode option required");
                if (!Arrays.asList("soft", "hard").contains(cmd.getOptionValue('m')))
                    throw new ParseException("Bad mode option value");
                if (cmd.hasOption('d') == cmd.hasOption('x')) {
                    if(!cmd.hasOption('x'))
                        throw new ParseException("Must either specify the dialect or choose to output xml");
                    else if(!cmd.hasOption('o'))
                        throw new ParseException("When xml and dialect are the two present the output option is required");
                }
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("osql", options);
            System.exit(1);
            return null;
        }

        if(cmd.hasOption('h')) {
            formatter.printHelp("osql", options, false);
            System.exit(0);
        }

        try {
            InputStream in = cmd.hasOption('i') ? new FileInputStream(cmd.getOptionValue('i')) : System.in;
            PrintStream out = cmd.hasOption('o') ? new PrintStream(new FileOutputStream(cmd.getOptionValue('o'))) : System.out;
            return new Argument(in, out, cmd.getOptionValue('m'), cmd.getOptionValue('d'), cmd.hasOption('x'));
        } catch (FileNotFoundException e) {
            System.err.println("File not found "+e.getMessage());
            formatter.printHelp("osql", options);
            System.exit(1);
            return null;
        }
    }

    public static void main(String[] args) {
        Argument argument = getArgument(args);
        assert argument != null;
        PrintStream out = argument.out;
        List<Table> tabs = getTables(new Scanner(argument.in, "UTF-8").useDelimiter("\\Z").next());
        Database db = Render.loadDatabase(tabs);
        if(argument.xml) {
            (argument.dialect == null ? out : System.out).println(db.getTables());
            if(argument.dialect == null)
                return;
        }
        IDatabase<DbView> database = new SqlViewer(argument.mode).adoptDatabase(db);
        DbView view;
        try {
            view = database.use(argument.dialect);
        } catch (DialectException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        List<TableView> tableViews = tabs.stream().map(view::render).collect(Collectors.toList());
        for(TableView table:tableViews) {
            out.println(table.showDefinition());
            out.println(table.showTriggers());
            out.println(table.showInsertions());
        }
        tableViews.stream().filter(table -> table.getTable().from != null).map(TableView::showConstraints).forEach(out::println);
        tableViews.stream().filter(table -> table.getTable().from != null).map(TableView::showITable).forEach(out::println);
    }

}
