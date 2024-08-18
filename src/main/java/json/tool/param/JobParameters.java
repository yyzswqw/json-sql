package json.tool.param;

import com.beust.jcommander.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class JobParameters extends JobParam.CommonJobParam implements Serializable {

  private static final long serialVersionUID = -1105273855379234348L;

  @Parameter(names = {"-d","--data"}, help = true, required = false,description = "json数据")
  private String jsonData;

  @Parameter(names = {"-f","--file"}, help = true, required = false,description = "json数据文件，只能有一条json，可以换行")
  private String file;

  @Parameter(names = {"-t","--tableName"}, help = true, required = true,description = "数据注册的表名")
  private String tableName;

  @Parameter(names = {"--sql"}, help = true, required = false,description = "需要执行的sql")
  private String sql;

  @Parameter(names = {"--sqlFile"}, help = true, required = false,description = "需要执行的sql文件")
  private String sqlFile;

  @Parameter(names = {"--format"}, help = true, required = false,description = "输出的格式: non / csv / json")
  private String format;

  @Parameter(names = {"--no_header","--no_outputHeader"}, help = true, required = false,description = "是否输出表头，默认值：true")
  private boolean noOutputHeader = false;

  @Parameter(names = {"--of","--outputFile"}, help = true, required = false,description = "结果输出到目标文件")
  private String outputFile;


}
