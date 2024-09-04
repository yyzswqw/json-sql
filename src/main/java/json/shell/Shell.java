package json.shell;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import json.shell.annotation.CommandClass;
import json.shell.annotation.CommandParser;
import json.shell.command.SqlCommand;
import json.shell.entity.CommandDescInfo;
import json.shell.utils.CommandCallUtil;
import json.shell.utils.ConsoleLog;
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

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class Shell {

    private Map<String, CommandDescInfo> commandDescInfoMap = new HashMap<>();

    private Terminal terminal = null;

    private Parser parser = new DefaultParser();

    private ConsoleLog consoleLog;

    private OutputStream logOutput = System.out;

    private final ShellContext shellContext = new ShellContext();

    private String prompt = "json-sql-shell:> ";

    private LineReader lineReader = null;

    private List<CommandDescInfo> commandDescInfoList = new ArrayList<>();

    private JsonSqlContext jsonSqlContext = JsonSqlContext.builder().build();

    private Boolean init = false;
    private Boolean initFinish = false;

    private boolean terminalOutPut = true;

    public static void main(String[] args) {
//        // 内嵌模式
//        Shell shell = new Shell();
//        // 关闭终端输出模式，通过接收返回值，自行打印输出结果
//        shell.setTerminalOutPut(false);
//        shell.init();
//        shell.initFinish();
//        String next = ConsoleLog.input();
//        Object result = shell.startCommand(next);
//        if(ObjectUtil.isNotEmpty(result)){
//            System.out.println(result.toString());
//        }
//        shell.close();

        // 终端模式
        try {
            Shell shell = new Shell();
            shell.init();
            shell.initFinish();
            shell.startTerminal();
            shell.close();
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }

    /**
     * 初始化
     */
    public void init(){
        if(init){
            return ;
        }
        init = true;
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(CommandClass.class);
        parseCommand(classes);
    }

    /**
     * 批量解析命令
     * @param urls urls
     */
    public void addCommand(Collection<URL> urls){
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInUrls(CommandClass.class,urls);
        parseCommand(classes);
    }

    /**
     * 解析命令
     * @param classes 命令classes
     */
    public void parseCommand(Set<Class<?>> classes){
        if(ObjectUtil.isEmpty(classes)){
            return;
        }
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

        if(ObjectUtil.isNotEmpty(commandDescInfoList)){
            commandDescInfoMap.putAll(commandDescInfoList.stream()
                    .collect(Collectors.toMap(CommandDescInfo::getName, commandDescInfo -> commandDescInfo)));
        }
    }

    /**
     * 完成最后初始化
     */
    public void initFinish(){
        if(initFinish){
            return ;
        }
        initFinish = true;
        shellContext.setJsonSqlContext(jsonSqlContext);
        shellContext.setCommandDescInfoMap(commandDescInfoMap);
        shellContext.setDefaultSavePath("."+ FileUtil.FILE_SEPARATOR+"data");
        shellContext.setDefaultTempDataPath("."+ FileUtil.FILE_SEPARATOR+"temp");
        consoleLog = new ConsoleLog(logOutput,terminalOutPut);
        shellContext.setConsoleLog(consoleLog);
    }

    /**
     * 如果terminal为空，则设置默认值
     */
    public void checkAndSetTerminal(){
        if(ObjectUtil.isEmpty(terminal)){
            try {
                terminal = TerminalBuilder.builder().system(true).build();
            } catch (IOException e) {
                consoleLog.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 执行一行命令或sql
     * @param command 一行命令或sql
     * @return 命令结果
     */
    public Object startCommand(String command){
        ParsedLine parsedLine = parser.parse(command, 0);
        return this.process(parsedLine,command.trim());
    }

    /**
     * 开启终端
     */
    public void startTerminal(){
        init();
        this.setTerminalOutPut(true);
        // 创建终端行读取器
        LineReaderBuilder terminalTemp = LineReaderBuilder.builder().terminal(terminal);
        if(ObjectUtil.isNotEmpty(commandDescInfoList)){
            terminalTemp.completer(this.getHelpCompleter(commandDescInfoList));
        }
        lineReader = terminalTemp.parser(parser).build();

        checkAndSetTerminal();
        String line;
        while ((line = lineReader.readLine(this.getPrompt())) != null) {
            ParsedLine parsedLine = lineReader.getParsedLine();
            String command = line.trim();
            this.process(parsedLine,command);
        }
    }

    /**
     * 执行命令或者sql
     * @param parsedLine 一行命令或sql
     * @param command 一行命令或sql的字符串形式
     * @return 命令结果
     */
    public Object process(ParsedLine parsedLine,String command){
        if (ObjectUtil.isEmpty(command)) {
            return null;
        } else if(SqlCommand.isSql(this.getShellContext(),command)) {
            String result = this.getJsonSqlContext().sql(command);
            this.consoleLog.log(result);
            return result;
        }else {
            try {
                return this.processCommand(parsedLine, command);
            }catch (Exception e){
                this.consoleLog.error(e);
                return "process command error : "+e.getMessage();
            }
        }
    }

    public void close(){
        if(ObjectUtil.isNotEmpty(terminal)){
            try {
                terminal.close();
            } catch (IOException e) {
                this.consoleLog.log("close terminal error!",e);
            }
        }
        if(ObjectUtil.isNotEmpty(consoleLog)){
            consoleLog.close();
        }

    }

    /**
     * 解析一行命令，并执行
     * @param parsedLine 一行命令
     * @param command 命令字符串
     * @return 命令结果
     */
    private Object processCommand(ParsedLine parsedLine,String command) {
        List<String> commandAndArgsList = parseCommand(parsedLine);
        if(ObjectUtil.isEmpty(commandAndArgsList) || commandAndArgsList.isEmpty()){
            String res = "command is error. command : " + commandAndArgsList.get(0);
            this.consoleLog.error(res);
            return res;
        }
        CommandDescInfo commandDescInfo = this.getCommandDescInfoMap().get(commandAndArgsList.get(0));
        if(ObjectUtil.isEmpty(commandDescInfo)){
            List<String> errors = SqlCommand.sqlHasError(this.getShellContext(),command);
            if(ObjectUtil.isNotEmpty(errors)){
                this.consoleLog.log("errors: {}",errors);
            }
            String res = "command not exist. command : " + commandAndArgsList.get(0);
            this.consoleLog.error(res);
            if(ObjectUtil.isNotEmpty(errors)){
                return res + "\n" + String.join("\n", errors);
            }else{
                return res;
            }
        }
        Object[] args = new Object[commandAndArgsList.size() - 1];
        for (int i = 1; i < commandAndArgsList.size(); i++) {
            args[i-1] = commandAndArgsList.get(i);
        }
        Object result = CommandCallUtil.callCommand(this.getShellContext(),commandDescInfo, args);
        if(ObjectUtil.isNotEmpty(result)){
            this.consoleLog.log(result);
            return result;
        }
        return null;
    }

    /**
     * 解析一行命令
     * @param parsedLine 一行命令
     * @return 命令以及参数列表
     */
    public static List<String> parseCommand(ParsedLine parsedLine) {
        List<String> result = new ArrayList<>();
        List<String> words = sanitizeInput(parsedLine.words());
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

    /**
     * 删除换行符
     * @param words words
     * @return new words
     */
    public static List<String> sanitizeInput(List<String> words) {
        words = words.stream()
                .map(s -> s.replaceAll("^\\n+|\\n+$", "")) // CR at beginning/end of line introduced by backslash continuation
                .map(s -> s.replaceAll("\\n+", " ")) // CR in middle of word introduced by return inside a quoted string
                .collect(Collectors.toList());
        return words;
    }


    /**
     * 生成命令列表
     * @param commandDescInfoList 命令描述信息
     * @return Completer
     */
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
