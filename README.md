# json-sql
这是一个写SQL操作JSON数据的项目。旨在简化JSON的增删改查。

# 提供的功能

- 表概念，一条JSON即为一张表
- 支持SQL statement
  - select ，查询字段
  - update ，更新字段(增加、删除、修改、重命名）
  - delete，删除字段
  - drop tableName，删除注册的表
  - create table tableName select ，根据select 结果，注册表
- 支持JSON Path语法，支持简化的jsonPath,如name.firstName：表示name下有一个属性是firstName，复杂jsonPath使用函数jsonPath('path')引用
- JSON字段的增加
- JSON字段的删除
- JSON字段的修改
- 支持加减乘除取余四则运算
- 支持逻辑比较符
- 支持SQL函数
- 支持子查询关联查询其他表
  - select * from t1 where a in (select b from t2 where 1=1 as b)，select子句返回的是json串，需要后面使用as表达式说明使用的字段名称
- 内置语法函数
- 自定义UDF函数，注册后，使用时需要加上$前缀
- 自定义UDF函数
  - 支持宏变量,宏变量需要在所有参数的最前面定义。
  - 支持可变参数，数组、集合（Collection类及其子类）或者Map类型（Map类及其子类,需要参数两两匹配，即key,value对）。
  - 一个UDF函数有且仅有一个可变参数（除了宏参数变量），位置可以是参数列表中任意位置，但必须在宏参数变量之后。
  - 可变参数中List、Map类型支持注册参数泛型。
  - 注意：使用时，如果传递了可变参数，如果可变参数后面还有参数，则必传，如果没有传可变参数，则后面的参数可以不传。



# 支持的逻辑比较符

- =  ，等于
- <> ，不等于
- != ，不等于
- < ，小于
- <=，小于等于
- \> ，大于
- \>= ，大于等于
- and
- or

# 支持的SQL statement

- select ，查询字段
- update tableName set，更新字段(增加、删除、修改、重命名）
- delete，删除字段
- drop tableName，删除注册的表
- create table tableName select ，根据select 结果，注册表

# 支持的SQL函数

- *CASE WHEN*
- *IN*
- *NOT IN*
-  *EXISTS*
- *NOT EXISTS*
-  *BETWEEN*
- *NOT BETWEEN*
- *LIKE*(正则表达式)
- *NOT LIKE*(正则表达式)
- *IS NULL*
- *IS NOT NULL*
- IF (expr1,trueResult1 [, expr2,trueResult2] [, elseResult] )

# 内置语法函数

- toJson('json')，将一个json字符串，转换为json对象
- toJsonByPath(‘path')，将一个json path值的json字符串，转换为json对象
- jsonPath('path')，获取一个json path值

# 支持的宏

- `ORIGINAL_JSON`，原始的json
- `READ_DOCUMENT`，读取值的json document
- `ORIGINAL_WRITE_DOCUMENT`，原始的写操作的json document
- `COPY_WRITE_WRITE_DOCUMENT`，copy写方式的写操作的json document,同`copy on write`
- `CUR_WRITE_DOCUMENT`，当前写操作的json document，根据写模式的不同，获取到的是当前写，操作的那个json document，可能值为：`ORIGINAL_WRITE_DOCUMENT`、`COPY_WRITE_WRITE_DOCUMENT`
- `ALL_UDF_DESC_INFO`， 所有的注册的udf函数描述信息
- `ALL_TABLE_NAME`，所有注册的表名
- `TABLE_NOT_OPERABLE_DATA`，获取指定表不可操作的数据，不可操作意味着对该对象的修改不会影响现有的表中的数据，需搭配宏参数`CUR_CONTEXT_PROXY`使用
- `TABLE_OPERABLE_DATA`，获取指定表可操作的数据，可操作意味着对该对象的修改会影响现有的表中的数据，需搭配宏参数`CUR_CONTEXT_PROXY`使用
- `CUR_CONTEXT_PROXY`，当前上下文代理

## 宏参数支持传递参数

1、注册的UDF函数首先获取到`CUR_CONTEXT_PROXY`宏变量

2、UDF函数中通过`MacroParamArgsContext`设置参数列表

3、由`CUR_CONTEXT_PROXY`参数调用`getMacro(MacroEnum macroEnum)`方法

4、调用`MacroParamArgsContext`的`remove()`方法清除参数列表，防止后续UDF函数获取到错误的参数

# 注册UDF函数

1、UDF函数只能是公用的静态（public static）方法

2、使用`@UdfMethod`注解标识该方法需要注册为UDF函数。`@UdfMethodIgnore`注解可以忽略注册

3、使用`@MacroParam`注解标识该参数为宏参数，并设置`type`：宏参数的类型

4、使用`@UdfParam`注解标识非宏参数的参数的描述信息

5、如果存在可变参数，可使用`@VariableArgsGenericityType`注解标识可变参数的泛型，也可以不标注，会自动推断泛型类型

6、使用`@UdfMethod`注解标识的方法会默认被注册进UDF函数列表中，如果不想自动注册，可以使用`@UdfMethodIgnore`忽略该函数

7、使用`@UdfClass`注解标识的类，类中的所有方法也会被自动注册为UDF函数

8、非`@UdfMethod`注解标识的方法如果也想被注册为UDF函数，可以使用`UdfParser`类中`classParser`方法注册，需要注意的是，如果宏参数没有被`@MacroParam`注解标识，则不会被当做宏参数。此时，若还想被当做宏参数则需要手动注册，可参考下方示例中`注册自定义UDF函数`模块

9、注册时，如果已存在同名UDF函数，该函数会注册失败

# 内置UDF函数

```shell
jsonSize
	desc: 返回数组的 size,如果是对象，返回的是一级 key 的数量(默认不开启)，jsonPath为空则默认为根路径
	Returns: Long
	args:
		jsonPath
			String         	jsonPath
		objReturnSize
			Boolean        	如果是json object对象，是否返回一级 key 的数量，默认false

del
	desc: 删除指定的jsonPath，jsonPath为空则默认删除全部
	Returns: Integer
	args:
		jsonPaths
			String[]       	jsonPath列表

formatAllLevel
	desc: 格式化json中的字符串json,将字符串的json转换为一个正常的json，直到递归到最大层级，jsonPath为空则默认为根路径
	Returns: Object
	args:
		jsonPath
			String         	jsonPath
		ignoreKeys
			String[]       	需要忽略key的列表

getTable
	desc: 获取指定表的数据
	Returns: Map<String,String>
	args:
		tableNameList
			List<String>   	表名列表

explode3
	desc: 将json 对象打平展开
	Returns: Map<Object,Object>
	args:
		jsonPath
			String         	jsonPath
		ignoreKeys
			String[]       	需要忽略key的列表

showTableNames
	desc: 获取满足正则表达式的表名,没有条件则获取所有表名
	Returns: Set<String>
	args:
		namePatternList
			List<String>   	表名正则表达式列表

size
	desc: 返回集合的 size,如果是对象，并且不为空，则返回1
	Returns: Long
	args:
		obj
			Object         	待判断数据

values
	desc: 获取jsonPath下的所有的value，直到递归到最大层级，jsonPath为空则默认为根路径
	Returns: Set<Object>
	args:
		jsonPath
			String         	jsonPath
		ignoreKeys
			String[]       	需要忽略key的列表

keysByLevel
	desc: 获取jsonPath下的所有的key，jsonPath为空则默认为根路径
	Returns: Set<Object>
	args:
		jsonPath
			String         	jsonPath
		level
			Long           	往下递归的最大层级

keys
	desc: 获取jsonPath下的所有的key，直到递归到最大层级，jsonPath为空则默认为根路径
	Returns: Set<Object>
	args:
		jsonPath
			String         	jsonPath

format
	desc: 格式化json中的字符串json,将字符串的json转换为一个正常的json，jsonPath为空则默认为根路径
	Returns: Object
	args:
		jsonPath
			String         	jsonPath
		level
			Long           	往下递归的最大层级
		replace
			Boolean        	是否替换原始的值，默认会替换，不替换时仅返回格式化后的jsonPath的值
		ignoreKeys
			String[]       	需要忽略key的列表

explodeAllLevel2
	desc: 将json 对象打平展开
	Returns: Map<Object,Object>
	args:
		putNewValue2TarJsonPath
			Boolean        	是否将打平展开的值放入根路径下
		jsonPath
			String         	jsonPath
		delOldJsonPath
			Boolean        	是否删除打平展开前jsonPath的值
		arrayExplode
			Boolean        	如果遇到数组是否需要打平展开
		arrayExplodeConcatIndex
			Boolean        	如果遇到数组需要打平展开，key是否拼接上位置下标，不拼接会导致相同key的value被替换
		ignoreKeys
			String[]       	需要忽略key的列表

explode2
	desc: 将json 对象打平展开
	Returns: Map<Object,Object>
	args:
		jsonPath
			String         	jsonPath
		level
			Long           	往下递归的最大层级
		ignoreKeys
			String[]       	需要忽略key的列表

formatAllLevel2
	desc: 格式化json中的字符串json,将字符串的json转换为一个正常的json，直到递归到最大层级，jsonPath为空则默认为根路径
	Returns: Object
	args:
		jsonPath
			String         	jsonPath
		replace
			Boolean        	是否替换原始的值，默认会替换，不替换时仅返回格式化后的jsonPath的值
		ignoreKeys
			String[]       	需要忽略key的列表

explode
	desc: 将json 对象打平展开
	Returns: Map<Object,Object>
	args:
		putNewValue2TarJsonPath
			Boolean        	是否将打平展开的值放入根路径下
		jsonPath
			String         	jsonPath
		delOldJsonPath
			Boolean        	是否删除打平展开前jsonPath的值
		arrayExplode
			Boolean        	如果遇到数组是否需要打平展开
		arrayExplodeConcatIndex
			Boolean        	如果遇到数组需要打平展开，key是否拼接上位置下标，不拼接会导致相同key的value被替换
		level
			Long           	往下递归的最大层级
		ignoreKeys
			String[]       	需要忽略key的列表

rename
	desc: 重命名key
	Returns: Integer
	args:
		jsonPath
			String         	需要改名的jsonPath路径
		oldName
			String         	旧的名称
		newName
			String         	新的名称

delIfNull
	desc: 如果指定的jsonPath的值为空，则删除指定的jsonPath，jsonPath为空则默认删除全部
	Returns: Integer
	args:
		jsonPaths
			String[]       	jsonPath列表

showUdf
	desc: 获取满足正则表达式的udf描述信息,没有条件则获取所有udf
	Returns: Collection<UdfFunctionDescInfo>
	args:
		patternList
			List<String>   	正则表达式列表

explodeAllLevel
	desc: 将json 对象打平展开
	Returns: Map<Object,Object>
	args:
		tarJsonPath
			String         	将打平展开的值放入指定路径下
		putNewValue2TarJsonPath
			Boolean        	是否将打平展开的值放入tarJsonPath路径下
		jsonPath
			String         	jsonPath
		delOldJsonPath
			Boolean        	是否删除打平展开前jsonPath的值
		arrayExplode
			Boolean        	如果遇到数组是否需要打平展开
		arrayExplodeConcatIndex
			Boolean        	如果遇到数组需要打平展开，key是否拼接上位置下标，不拼接会导致相同key的value被替换
		ignoreKeys
			String[]       	需要忽略key的列表

valuesByLevel
	desc: 获取jsonPath下的所有的value，jsonPath为空则默认为根路径
	Returns: Set<Object>
	args:
		jsonPath
			String         	jsonPath
		level
			Long           	往下递归的最大层级
		ignoreKeys
			String[]       	需要忽略key的列表
```



# 示例

## 原始数据

假设有下面一条JSON数据，在java中将其给变量json:

```JSON
{
	"p3": "reference",
	"p4": 5,
	"p1": "aa",
	"store": {
		"book": [{
				"category": "reference",
				"author": "Nigel Rees",
				"title": "Sayings of the Century",
				"price": 8.95
			}, {
				"category": "fiction",
				"author": "Evelyn Waugh",
				"title": "Sword of Honour",
				"price": 12.99
			}, {
				"category": "fiction",
				"author": "Herman Melville",
				"title": "Moby Dick",
				"isbn": "0-553-21311-3",
				"price": 8.99
			}, {
				"category": "fiction",
				"author": "J. R. R. Tolkien",
				"title": "The Lord of the Rings",
				"isbn": "0-395-19395-8",
				"price": 22.99
			}
		],
		"bicycle": {
			"color": "red",
			"price": 19.95
		}
	}
}
```



## 注册自定义UDF函数

```java
// 自定义UDF函数方法和宏变量
public class CustomMethod {

    public static int a(Number a,Object b){
        return a.intValue();
    }

    public static int b(BigDecimal a, BigDecimal b){
        return a.add(b).intValue();
    }

    public static int c(Object a1,Object a2,Object a3,Object a4,Object a5,BigDecimal a, BigDecimal b,List<String> a6){
        return a.add(b).intValue();
    }

    public static int d(Object a1, Object a2, Object a3, Object a4, DocumentContext a5){
        return 2;
    }

}

// 手动注册自定义UDF函数
    public static void registerDemo(JsonSqlContext jsonSqlContext){
        try {
            Method a = UdfDemo.class.getMethod("a", Number.class, Object.class);
            Method b = UdfDemo.class.getMethod("b", BigDecimal.class, BigDecimal.class);
            Method c = UdfDemo.class.getMethod("c", Object.class,Object.class,Object.class,Object.class,Object.class,BigDecimal.class, BigDecimal.class, List.class);
            Method d = UdfDemo.class.getMethod("d", Object.class,Object.class,Object.class,Object.class, DocumentContext.class);

            // 注册 udf 函数
            jsonSqlContext.registerFunction("a", a,Number.class, Object.class);
            jsonSqlContext.registerFunction("b", b,BigDecimal.class, BigDecimal.class);
            jsonSqlContext.registerFunction("c", c,BigDecimal.class, BigDecimal.class, List.class);
            jsonSqlContext.registerFunction("d", d);

            // 注册宏参数变量
            jsonSqlContext.registerMacro("c", MacroEnum.ORIGINAL_JSON,MacroEnum.READ_DOCUMENT,MacroEnum.ORIGINAL_WRITE_DOCUMENT,MacroEnum.COPY_WRITE_WRITE_DOCUMENT,MacroEnum.CUR_WRITE_DOCUMENT);
            jsonSqlContext.registerMacro("d", MacroEnum.ORIGINAL_JSON,MacroEnum.READ_DOCUMENT,MacroEnum.ORIGINAL_WRITE_DOCUMENT,MacroEnum.COPY_WRITE_WRITE_DOCUMENT, MacroEnum.CUR_WRITE_DOCUMENT);

            UdfFunctionDescInfo udfFunctionDescInfo = new UdfFunctionDescInfo();
            udfFunctionDescInfo.setFunctionName("c");
            udfFunctionDescInfo.setFunctionDesc("c");
            udfFunctionDescInfo.setReturnType("int");

            UdfParamDescInfo paramDescInfo1 = new UdfParamDescInfo();
            paramDescInfo1.setParamName("a");
            paramDescInfo1.setParamDesc("a");
            paramDescInfo1.setParamType("BigDecimal");
            udfFunctionDescInfo.getUdfParamDescInfoList().add(paramDescInfo1);

            UdfParamDescInfo paramDescInfo2 = new UdfParamDescInfo();
            paramDescInfo2.setParamName("b");
            paramDescInfo2.setParamDesc("b");
            paramDescInfo2.setParamType("BigDecimal");
            udfFunctionDescInfo.getUdfParamDescInfoList().add(paramDescInfo2);

            UdfParamDescInfo paramDescInfo3 = new UdfParamDescInfo();
            paramDescInfo3.setParamName("a6");
            paramDescInfo3.setParamDesc("a6");
            paramDescInfo3.setParamType("List<String>");
            udfFunctionDescInfo.getUdfParamDescInfoList().add(paramDescInfo3);
            // 注册函数描述信息
            jsonSqlContext.registerFunctionDescInfo("c", udfFunctionDescInfo);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
```

## Select示例

### 示例1

查询所有字段

```java


String sql = "select * from a1";

JsonSqlContext.registerTable("a1", json);
JsonSqlContext.registerTable("b1", json);
JsonSqlContext.setTableConfig("a1", TableConfig.WRITE_MODEL,true);
String exec = JsonSqlContext.sql(sql);

System.out.println("最终结果:"+exec);
System.out.println("最终结果 table a1 :"+JsonSqlContext.getTable("a1"));
System.out.println("最终结果 table b1 :"+JsonSqlContext.getTable("b1"));
```

结果为：

```json
最终结果:{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}
```

### 示例2

查询字段进行四则运算或函数运算

```java
String sql = "select p1,p2,p3,p4 + 1 as aA1,p5,toJson('{\"a\":1}') from b1";
```

结果为：

```json
最终结果:{"p1":"aa","p3":"reference","aA1":6,"_c0":{"a":1}}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例3

查询使用case when语句

```java
String sql = "select case when 1>2 then 1 end as c from a1 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1";
```

结果为：

```json
最终结果:{}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例4

like中写正则表达式

```java
String sql = "select case when 1>2 then 1 end as c,1 as b ,jsonPath('$..book[0][\"category\"]') as c from a1 where p3 like '^refer'";
```

结果为：

```json
最终结果:{"b":1,"c":["reference"]}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例5

条件中进行四则运算

```java
String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where 3 between 1+1 and p4*2";
```

结果为：

```json
最终结果:{"b":1}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例6

使用exists

```java
String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where not exists (select 1 from b1 where 1!=1 as _c0)";
```

结果为：

```json
最终结果:{"b":1}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例7

使用in

```java
String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where 22.99 not in (1,2,3,22.99)";
```

结果为：

```json
最终结果:null

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例8

使用in，关联子查询

```java
String sql = "select case when 1>2 then 1 end as c,1 as b from a1 where 22.99 in (select 1 as abc from b1 where 1=1 as abc)";
```

结果为：

```json
最终结果:null

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例9

进行自定义UDF函数调用

```java
String sql = "select *,jsonPath('$.store.book[0].category') from a1 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1";
```

结果为：

```json
最终结果:{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"_c0":"reference"}

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

## Delete示例

### 示例1

条件删除字段

```java
String sql = "delete a1 p1,p2,p3,jsonPath('$..book[0][\"category\"]') where 51 in (select p4 from b1 as p4)";
```

结果为：

```json
最终结果:0

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例2

条件删除字段

```java
String sql = "delete a1 p1,p2,p3,jsonPath('$..book[0][\"category\"]') where p4 != 5";
```

结果为：

```json
最终结果:0

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

## Update示例

### 示例1

删除字段，使用del()函数

```java
String sql = "update a1 SET $del('$.store.book[0].category'),age = 31.32 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"age":31.32}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例2

使用and和子查询in

```java
String sql = "update a1 SET $del('$.store.book[0].category'),age = 31.32 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1 and 22.99  in (select jsonPath('$.store.book[*].price') as abc from b1 where 1=1 as abc)";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"age":31.32}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例3

使用and和非子查询in

```java
String sql = "update a1 SET $del('$.store.book[0].category'),age = 31.32 where $a($c(jsonPath('$.p4'),$d()),'b') >= 1.1 and 22.99  in (1,2,3,22.99)";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"age":31.32}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例4

使用case when

```java
        String sql = "update a1 SET name = 'a',age = 1,a1 = " +
                "case when age = 1 then 'a' " +
                "when age < 10 then true " +
                "when age <20 and name = 'a' then 5.9 " +
                "when dd is not null then null " +
                "else toJsonByPath('$..book[:3][\"category\"]') end " +
                " where true = true";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":"a","age":1,"a1":["reference","fiction","fiction"]}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例5

使用逻辑比较

```java
String sql = "update a1 SET name = 'a',age = 31.32,name=null where 1.1 >= 1.1";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":null,"age":31.32}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例6

使用 is null，设置null

```java
String sql = "update a1 SET name = 'a',age = 31.32,name=null where aa IS NULL";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":null,"age":31.32}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例7

使用is null，设置boolean

```java
String sql = "update a1 SET name = 'a',age = 31,name=false where aa IS NULL";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":false,"age":31}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例8

使用is null，设置动态值

```java
String sql = "update a1 SET name = 'a',age = 31,ab=jsonPath('$..book[?(@.price<10)]') where aa IS NULL";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":"a","age":31,"ab":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99}]}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例9

使用is null，设置动态对象值

```java
String sql = "update a1 SET name = 'a',age = 31,ab=jsonPath('$..*') where aa IS NULL";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":"a","age":31,"ab":["reference",5,"aa",{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],{"color":"red","price":19.95},{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99},"reference","Nigel Rees","Sayings of the Century",8.95,"fiction","Evelyn Waugh","Sword of Honour",12.99,"fiction","Herman Melville","Moby Dick","0-553-21311-3",8.99,"fiction","J. R. R. Tolkien","The Lord of the Rings","0-395-19395-8",22.99,"red",19.95]}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例10

使用is null，和复杂json path

```java
String sql = "update a1 SET name = 'a',age = 31,ab=jsonPath('$..book[?(@.isbn)]') where aa IS NULL";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":"a","age":31,"ab":[{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}]}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例11

使用is not null，设置字面量值

```java
String sql = "update a1 SET name = 'a',age = 31 where aa IS NOT NULL";
```

结果为：

```json
最终结果:0

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例12

使用boolean条件，设置字面量值

```java
String sql = "update a1 SET name = 'a',age = 31 where true";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":"a","age":31}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例13

使用逻辑比较符，设置字面量值

```java
String sql = "update a1 SET name = 'a',age = 31 where name = 'a1'";
```

结果为：

```json
最终结果:0

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例14

使用逻辑比较符，设置动态值

```java
String sql = "update a1 SET age = jsonPath('$.age')%4 + age,name = 1";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"age":null,"name":1}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例15

设置动态值

```java
String sql = "update a1 SET jsonPath(\"name\") = jsonPath(\"$..book[:3]['category']\"),age = jsonPath(\"age\")%4 + age";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":["reference","fiction","fiction"],"age":null}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例16

使用逻辑比较符，设置动态值

```java
String sql = "update a1 SET jsonPath('name') = jsonPath('$..book[-1:][\"category\"]'),age = jsonPath('age')%4 + age,p1=123 where p2 = 'aa'";
```

结果为：

```json
最终结果:0

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例17

使用逻辑比较符，设置动态运算后的值

```java
String sql = "update a1 set jsonPath('name') = jsonPath('$..book[-1:][\"category\"]'),age = jsonPath('age')%4 + age,p1=123 where p2 is not null or (p3 = p1 and (p4 is null))";
```

结果为：

```json
最终结果:0

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

### 示例18

设置动态运算后的值

```java
String sql = "update a1 SET jsonPath('name') = jsonPath('$.store.book[*].author'),age = jsonPath('age')%4 + age";
```

结果为：

```json
最终结果:1

最终结果 table a1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}},"name":["Nigel Rees","Evelyn Waugh","Herman Melville","J. R. R. Tolkien"],"age":null}

最终结果 table b1 :{"p3":"reference","p4":5,"p1":"aa","store":{"book":[{"category":"reference","author":"Nigel Rees","title":"Sayings of the Century","price":8.95},{"category":"fiction","author":"Evelyn Waugh","title":"Sword of Honour","price":12.99},{"category":"fiction","author":"Herman Melville","title":"Moby Dick","isbn":"0-553-21311-3","price":8.99},{"category":"fiction","author":"J. R. R. Tolkien","title":"The Lord of the Rings","isbn":"0-395-19395-8","price":22.99}],"bicycle":{"color":"red","price":19.95}}}

```

