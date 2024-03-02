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
STRING : ('"' (~('\\' | '"') | '\\' .)* '"') | ('\'' (~('\\' | '\'') | '\\' .)* '\'');
INT : DIGIT+;
DOUBLE : ([0-9]+ '.' [0-9]* | '.' [0-9]+);

WS : [ \t\r\n]+ -> skip;

update: ('UPDATE') | ('update') ;
delete: ('DELETE') | ('delete') ;
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
existsLable: 'EXISTS' | 'exists';
betweenLable: 'BETWEEN' | 'between';
likeLable: 'LIKE' | 'like';
ifLable: 'IF' | 'if';
createLable: 'CREATE' | 'create';
dropLable: 'DROP' | 'drop';
tableLable: 'TABLE' | 'table';

selectLable: 'SELECT' | 'select';
fromLable: 'FROM' | 'from';
asLable: 'AS' | 'as';
starLable: '*';

//自定义比较符
customCompareFunction: 'compareSymbol(' STRING ')';
//自定义高优先级运算符，与乘除同优先级
highOperatorFunction: 'highOpSymbol(' STRING ')';
//自定义低优先级运算符，与加减同优先级
lowOperatorFunction: 'lowOpSymbol(' STRING ')';
toJsonFunction: 'toJson(' STRING ')';
toJsonByPathFunction: 'toJsonByPath(' STRING ')';
jsonPathFunction: 'jsonPath(' STRING ')';

ifFunction: ifLable '(' ifTrueAndResultBranch (',' ifTrueAndResultBranch)* ifFuncElseBranch? ')';
ifTrueAndResultBranch: expression ',' relationalExpr;
ifFuncElseBranch: ',' relationalExpr;

// 语法规则
sql: sqlStatement (';' sqlStatement)* ';'?;
sqlStatement: updateStatement | selectStatement | deleteStatement | createTableStatement | dropTableStatement;
deleteStatement : delete (tableName)? delClause (where expression)?;
delClause : columnName (',' columnName)* ;
updateSql: updateStatement;
updateStatement : (update tableName)? set setClause (where expression)?;
setClause : setExpression (',' setExpression)* ;
setExpression : (columnName '=' (relationalExpr | caseExpr)) | customFunction;

tableName : ID;
columnName : ID | jsonPathFunction;
literalValue : STRING | doubleValue | ('-') doubleValue | intValue | ('-') intValue | nullLable | boolLable;
intValue : INT;
doubleValue : DOUBLE;


caseExpr : caseLable whenBranch+ elseBranch? endLable;
whenBranch : whenLable condition thenLable relationalExpr;
elseBranch : elseLable relationalExpr;
condition : expression;

expression : orExpr | '(' expression ')';
orExpr : andExpr (orLable andExpr)* | '(' expression ')';
andExpr : equalityExpr (andLable equalityExpr)* | '(' expression ')' ;
equalityExpr : (relationalExpr comparisonOperator relationalExpr) | boolLable | isNullExpression | inSubqueryExpression | existsSubqueryExpression | betweenExpression | likeExpression | '(' expression ')';
comparisonOperator : '=' | '<>' | '!=' | '<' | '<=' | '>' | '>=' | customCompareFunction;

relationalExpr: relationalExpr (op=('*'|'/'|'%') | highOperatorFunction) relationalExpr           # MulDiv
            | relationalExpr (op=('+'|'-') | lowOperatorFunction ) relationalExpr                 # AddSub
            | primaryExpr                                                                         # id
            | toJsonFunction                                                                      # toJsonFunc
            | ifFunction                                                                          # ifFunc
            | toJsonByPathFunction                                                                # toJsonByPathFunc
            | customFunction                                                                      # customFunc
            | jsonPathFunction                                                                    # jsonPathFunc
            | '(' relationalExpr ')'                                                              # parens
            ;

primaryExpr : '(' expression ')' | literalValue | columnName;

// IN子句 eg: ab in (select * from table where 1=1 as '$.a[0].book')
inSubqueryExpression : (columnName | literalValue) (notLable)? inLable '('  ((selectStatement asLable columnName ) | (literalValue (',' literalValue)*) )')';

// EXISTS子句
existsSubqueryExpression : (notLable)? existsLable '(' selectStatement asLable columnName ')';

// BETWEEN子句
betweenExpression : (columnName | literalValue) (notLable)? betweenLable relationalExpr andLable relationalExpr;

// LIKE子句
likeExpression : (columnName | literalValue) (notLable)? likeLable STRING;

// IS NULL子句
isNullExpression : columnName isLable (notLable)? nullLable;

createTableStatement: createLable tableLable tableName selectStatement;

dropTableStatement: dropLable tableName;

selectStatement : selectLable selectList fromLable tableName (where expression)?;
selectList : selectItem (',' selectItem)*;
selectItem : (relationalExpr | caseExpr) (asLable ID)?  | starLable ;
