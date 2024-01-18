grammar Sql;

@header { package json.sql.parse; }

customFunction : functionCall;

functionCall : CUSTOMID '(' args? ')' ;

args : argsExpression (',' argsExpression)* ;

argsExpression : functionCall | relationalExpr ;



// 词法规则
//fragment 关键字用于定义只能在语法规则内部使用的片段规则
fragment DIGIT : [0-9];
fragment LETTER : [a-zA-Z];
fragment UNDERSCORE : '_';

CUSTOMID : '$' (LETTER | UNDERSCORE) (LETTER | DIGIT | UNDERSCORE)* ;
ID : (LETTER | UNDERSCORE) (LETTER | DIGIT | UNDERSCORE)* | ((LETTER | UNDERSCORE) (LETTER | DIGIT | UNDERSCORE)*) ('.' ((LETTER | UNDERSCORE) (LETTER | DIGIT | UNDERSCORE)*))*;
//ONEID : (LETTER | UNDERSCORE) (LETTER | DIGIT | UNDERSCORE)* ;
STRING : ('"' (~('\\' | '"') | '\\' .)* '"') | ('\'' (~('\\' | '\'') | '\\' .)* '\'');
INT : '-'? DIGIT+;
DOUBLE : '-'? ([0-9]+ '.' [0-9]* | '.' [0-9]+);

WS : [ \t\r\n]+ -> skip;

update: ('UPDATE') | ('update') ;
set: 'SET' | 'set';
where: 'WHERE' | 'where';
nullLable: 'NULL' | 'null';
notLable: 'NOT' | 'not';
inLable: 'IN' | 'in';
andLable: 'AND' | 'and';
orLable: 'OR' | 'or';
isLable: 'IS' | 'is';
boolLable: 'TRUE' | 'true' | 'FALSE' | 'false';
caseLable: 'CASE' | 'case';
endLable: 'END' | 'end';
whenLable: 'WHEN' | 'when';
thenLable: 'THEN' | 'then';
elseLable: 'ELSE' | 'else';

selectLable: 'SELECT' | 'select';
fromLable: 'FROM' | 'from';
asLable: 'AS' | 'as';
starLable: '*';

toJsonFunction: 'toJson(' STRING ')';
toJsonByPathFunction: 'toJsonByPath(' STRING ')';
jsonPathFunction: 'jsonPath(' STRING ')';


// 语法规则
updateSql: updateStatement;
updateStatement : (update tableName)? set setClause (where expression)?;
setClause : setExpression (',' setExpression)* ;
setExpression : columnName '=' (relationalExpr | caseExpr | delColumnExpr);

tableName : ID;
columnName : ID | jsonPathFunction;
literalValue : STRING | doubleValue | intValue | nullLable | boolLable;
stringValue: ('"' (~('\\' | '"') | '\\' .)* '"') | ('\'' (~('\\' | '\'') | '\\' .)* '\'');
intValue : INT;
doubleValue : DOUBLE;

delColumnExpr : 'del' '(' ')';

caseExpr : caseLable whenBranch+ elseBranch? endLable;
whenBranch : whenLable condition thenLable relationalExpr;
elseBranch : elseLable relationalExpr;
condition : expression;

expression : orExpr | '(' expression ')';
orExpr : andExpr (orLable andExpr)* | '(' expression ')';
andExpr : equalityExpr (andLable equalityExpr)* | '(' expression ')' ;
equalityExpr : (relationalExpr comparisonOperator relationalExpr) | boolLable | isNullExpression | '(' expression ')';
comparisonOperator : '=' | '<>' | '!=' | '<' | '<=' | '>' | '>=';

relationalExpr: relationalExpr op=('*'|'/'|'%') relationalExpr           # MulDiv
            | relationalExpr op=('+'|'-') relationalExpr                 # AddSub
            | primaryExpr                                                # id
            | toJsonFunction                                             # toJsonFunc
            | toJsonByPathFunction                                       # toJsonByPathFunc
            | customFunction                                             # customFunc
            | jsonPathFunction                                           # jsonPathFunc
            | '(' relationalExpr ')'                                     # parens
            ;

primaryExpr : '(' expression ')' | literalValue | columnName;

// 以下是新增的规则

// IN子句
inSubqueryExpression : columnName (notLable)? inLable '(' selectStatement ')';

// EXISTS子句
existsSubqueryExpression : 'EXISTS' '(' selectStatement ')';

// BETWEEN子句
//betweenExpression : columnName ('NOT')? 'BETWEEN' additiveExpr 'AND' additiveExpr;

// LIKE子句
likeExpression : columnName (notLable)? 'LIKE' stringValue;

// IS NULL子句
isNullExpression : columnName isLable (notLable)? nullLable;

selectStatement : selectLable selectList fromLable tableName (where expression)?;
selectList : selectItem (',' selectItem)*;
selectItem : relationalExpr (asLable ID)?  | starLable ;
//selectItem : relationalExpr (asLable tableName)? | starLable ;












