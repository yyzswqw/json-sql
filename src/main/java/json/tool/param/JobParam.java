package json.tool.param;

import com.beust.jcommander.Parameter;
import lombok.Data;

import java.io.Serializable;

@Data
public class JobParam implements Serializable {

    private static final long serialVersionUID = -8067017372230577552L;

    @Data
    public static class CommonJobParam implements Serializable {

        private static final long serialVersionUID = -1217061168656566524L;

        @Parameter(names = { "-h","--help"}, help = true,description = "显示帮助信息")
        private boolean help;

    }

}
