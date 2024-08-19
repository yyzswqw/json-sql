package json.shell;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import json.shell.annotation.CommandClass;
import json.shell.annotation.CommandParser;
import json.shell.command.SqlCommand;
import json.shell.entity.CommandDescInfo;
import json.shell.utils.CommandCallUtil;
import json.sql.JsonSqlContext;
import json.sql.annotation.PackageAnnotationScanner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jline.builtins.Completers;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class Shell {

    private Map<String, CommandDescInfo> commandDescInfoMap = new HashMap<>();

    private Terminal terminal = null;

    private String prompt = "json-sql-shell:> ";

    private LineReader lineReader = null;;

    private List<CommandDescInfo> commandDescInfoList = new ArrayList<>();

    private JsonSqlContext jsonSqlContext = JsonSqlContext.builder().build();

    public Shell(){
        initCommand();
    }

    public void initCommand(){
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(CommandClass.class);
        Set<String> commandNameSet = new HashSet<>();
        for (Class<?> aClass : classes) {
            List<CommandDescInfo> commandDescInfos = CommandParser.classParser(aClass, true, (Method[]) null);
            if(ObjectUtil.isNotEmpty(commandDescInfos)){
                for (CommandDescInfo commandDescInfo : commandDescInfos) {
                    String name = commandDescInfo.getName();
                    if (commandNameSet.contains(name)) {
                        throw new RuntimeException("存在同名 commandName : " + name);
                    }
                    commandNameSet.add(name);
                    commandDescInfoList.add(commandDescInfo);
                }
            }
        }
//        创建行读取器
        Parser parser = new DefaultParser();
        LineReaderBuilder terminalTemp = LineReaderBuilder.builder().terminal(terminal);
        if(ObjectUtil.isNotEmpty(commandDescInfoList)){
            terminalTemp.completer(this.getHelpCompleter(commandDescInfoList));
            commandDescInfoMap = commandDescInfoList.stream()
                    .collect(Collectors.toMap(CommandDescInfo::getName, commandDescInfo -> commandDescInfo));
        }
        lineReader = terminalTemp.parser(parser).build();
    }

    public void start(){
        String line;
        while ((line = lineReader.readLine(this.getPrompt())) != null) {
            String command = line.trim();
            if (command.isEmpty()) {
                continue;
            } else if(SqlCommand.isSql(command)) {
                String result = this.getJsonSqlContext().sql(command);
                Console.log(result);
            }else {
                try {
                    this.processCommand(lineReader,line);
                }catch (Exception e){
                    Console.log(e.getMessage());
                }
            }
        }
    }

    public void close(){
        if(ObjectUtil.isNotEmpty(terminal)){
            try {
                terminal.close();
            } catch (IOException e) {
                log.error("close terminal error!",e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Shell shell = new Shell();
            // 创建终端
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            shell.setTerminal(terminal);

            ShellContext.cur().setJsonSqlContext(shell.getJsonSqlContext());
            ShellContext.cur().setTerminal(terminal);
            ShellContext.cur().setCommandDescInfoMap(shell.getCommandDescInfoMap());
            ShellContext.cur().setDefaultSavePath("."+ FileUtil.FILE_SEPARATOR+"data");
            ShellContext.cur().setDefaultTempDataPath("."+ FileUtil.FILE_SEPARATOR+"temp");
            shell.start();
            shell.close();
        } catch (IOException e) {
            log.error("Error: ", e);
        }
    }

    private void processCommand(LineReader lineReader,String command) {
        List<String> commandAndArgsList = parseCommand(lineReader);
        if(ObjectUtil.isEmpty(commandAndArgsList) || commandAndArgsList.size() < 1){
            throw new RuntimeException("命令不正确. command : "+command);
        }
        CommandDescInfo commandDescInfo = this.getCommandDescInfoMap().get(commandAndArgsList.get(0));
        if(ObjectUtil.isEmpty(commandDescInfo)){
            List<String> errors = SqlCommand.sqlHasError(command);
            if(ObjectUtil.isNotEmpty(errors)){
                log.info("errors: {}",errors);
            }
            throw new RuntimeException("命令不存在. command : "+commandAndArgsList.get(0));
        }
        Object[] args = new Object[commandAndArgsList.size() - 1];
        for (int i = 1; i < commandAndArgsList.size(); i++) {
            args[i-1] = commandAndArgsList.get(i);
        }
        Object result = CommandCallUtil.callCommand(commandDescInfo, args);
        if(ObjectUtil.isNotEmpty(result)){
            Console.log(result);
        }
    }


    public static List<String> parseCommand(LineReader lineReader) {
        List<String> result = new ArrayList<>();
        ParsedLine parsedLine1 = lineReader.getParsedLine();
        List<String> words = sanitizeInput(parsedLine1.words());
//        拼接用于打印
//        String line = words.stream().collect(Collectors.joining(" ")).trim();
        if(ObjectUtil.isNotEmpty(words)){
            for (String word : words) {
                if(ObjectUtil.isNull(word)){
                    result.add(word);
                }else {
                    result.add(word.trim());
                }
            }
        }
        return result;
    }

    public static List<String> sanitizeInput(List<String> words) {
        words = words.stream()
                .map(s -> s.replaceAll("^\\n+|\\n+$", "")) // CR at beginning/end of line introduced by backslash continuation
                .map(s -> s.replaceAll("\\n+", " ")) // CR in middle of word introduced by return inside a quoted string
                .collect(Collectors.toList());
        return words;
    }


    private Completer getHelpCompleter(List<CommandDescInfo> commandDescInfoList) {
        if(ObjectUtil.isEmpty(commandDescInfoList)){
            return null;
        }

        List<Completer> list = new ArrayList<>();
        for (CommandDescInfo commandDescInfo : commandDescInfoList) {
//            Completer schemaCompleter = new Completers.TreeCompleter(
//                    Completers.TreeCompleter.node("schema",
//                            Completers.TreeCompleter.node("select", Completers.TreeCompleter.node(NullCompleter.INSTANCE)),
//                            Completers.TreeCompleter.node("update", Completers.TreeCompleter.node(NullCompleter.INSTANCE))
//                    )
//            );
            Completer completerTemp = new Completers.TreeCompleter(
                    Completers.TreeCompleter.node(commandDescInfo.getName(),Completers.TreeCompleter.node(NullCompleter.INSTANCE)
                    )
            );
            list.add(completerTemp);
        }
        return new AggregateCompleter(list.toArray(new Completer[0]));
    }

}
