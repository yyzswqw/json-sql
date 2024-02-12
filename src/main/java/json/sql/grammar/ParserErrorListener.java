package json.sql.grammar;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class ParserErrorListener extends BaseErrorListener {

    private final List<String> errorMsgList = new ArrayList<>();
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        errorMsgList.add(String.format("line %s:%s %s",line,charPositionInLine,msg));
    }

    public boolean hasError(){
        return !errorMsgList.isEmpty();
    }

    public List<String> errors(){
        return errorMsgList;
    }

    public void clearError(){
        errorMsgList.clear();
    }

}
