package json.tool.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import json.tool.param.JobParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class JobParamUtil {

    private JobParamUtil() {
    }

    public static boolean parseParam(String[] args, JobParam.CommonJobParam jobParam, Class<?> clazz) {
        JCommander jct = JCommander.newBuilder().addObject(jobParam).acceptUnknownOptions(true).build();
        jct.setProgramName("java -classpath <*.jar> "+clazz.getName());
        try {
            jct.parse(args);
            if (jobParam.isHelp()) {
                jct.usage();
                return true;
            }
        } catch (ParameterException parameterException) {
            log.error("Params Error. info = [ {} ]", parameterException.toString());
            jct.usage();
            return true;
        }
        return false;
    }

}
