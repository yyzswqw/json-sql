# json-sql
这是一个写SQL操作JSON数据的项目。旨在简化JSON的增删改查。

# 提供的功能

- 表概念，一条JSON即为一张表
- 支持SQL statment
  - select ，查询字段
  - update ，更新字段(增加、删除、修改）
  - delete，删除字段
- 支持JSON Path语法，支持简化的jsonPath,如name.firstName：表示name下有一个属性是firstName，复杂jsonPath使用函数jsonPath('path')引用
- JSON字段的增加
- JSON字段的删除
- JSON字段的修改
- 支持加减乘除取余四则运算
- 支持逻辑比较符
- 支持SQL函数
- 支持关联查询其他表
  - select * from t1 where a in (select b from t2 where 1=1 as b)，select子句返回的是json串，需要后面使用as表达式说明使用的字段名称
- 内置函数
- 自定义UDF函数，注册后，使用时需要加上$前缀
- 自定义UDF函数，
  - 支持宏变量,宏变量需要在所有参数的前面定义。
  - 支持可变参数，数组、集合（Collection类及其子类）或者Map类型（Map类及其子类,需要参数两两匹配，即key,value）。
  - 一个UDF函数有且仅有一个可变参数，位置可以是参数列表中任意位置，但必须在宏变量之后。
  - 可变参数中List、Map类型支持注册参数泛型
  - 注意：使用时，如果传递了可变参数，如果可变参数后面还有参数，则必传，如果没有传可变参数，则后面的参数可以不传。

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



# 支持的逻辑比较符

- =  ，等于
-  <> ，不等于
-  != ，不等于
-  < ，小于
-  <=，小于等于
- \> ，大于
- \>= ，大于等于
- and
- or

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

# 内置函数

- toJson('json')，将一个json字符串，转换为json对象
- toJsonByPath(‘path')，将一个json path值的json字符串，转换为json对象
- jsonPath('path')，获取一个json path值
- $del('path', ... ,'path')，删除一个或多个字段

# 支持的宏

- *ORIGINAL_JSON*，原始的json
- *READ_DOCUMENT*，读取值的json document
- *ORIGINAL_WRITE_DOCUMENT*，原始的写操作的json document
- *COPY_WRITE_WRITE_DOCUMENT，copy写方式的写操作的json document*
- *CUR_WRITE_DOCUMENT*，当前写操作的json document，根据写模式的不同，获取到的是当前写，操作的那个json document

# 示例

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

    public static int c(Object a1,Object a2,Object a3,Object a4,Object a5,BigDecimal a, BigDecimal b){
        return a.add(b).intValue();
    }

    public static int d(Object a1, Object a2, Object a3, Object a4, DocumentContext a5){
        return 2;
    }

}


// 注册自定义UDF函数
try {
            Method a = CustomMethod.class.getMethod("a", Number.class, Object.class);
            Method b = CustomMethod.class.getMethod("b", BigDecimal.class, BigDecimal.class);
            Method c = CustomMethod.class.getMethod("c", Object.class,Object.class,Object.class,Object.class,Object.class,BigDecimal.class, BigDecimal.class);
            Method d = CustomMethod.class.getMethod("d", Object.class,Object.class,Object.class,Object.class, DocumentContext.class);

            JsonSqlContext.registerFunction("a", a,Number.class, Object.class);
            JsonSqlContext.registerFunction("b", b,BigDecimal.class, BigDecimal.class);
            JsonSqlContext.registerFunction("c", c,BigDecimal.class, BigDecimal.class);
            JsonSqlContext.registerFunction("d", d);

            JsonSqlContext.registerMacro("c", MacroEnum.ORIGINAL_JSON,MacroEnum.READ_DOCUMENT,MacroEnum.ORIGINAL_WRITE_DOCUMENT,MacroEnum.COPY_WRITE_WRITE_DOCUMENT,MacroEnum.CUR_WRITE_DOCUMENT);
            JsonSqlContext.registerMacro("d", MacroEnum.ORIGINAL_JSON,MacroEnum.READ_DOCUMENT,MacroEnum.ORIGINAL_WRITE_DOCUMENT,MacroEnum.COPY_WRITE_WRITE_DOCUMENT,MacroEnum.CUR_WRITE_DOCUMENT);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
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

