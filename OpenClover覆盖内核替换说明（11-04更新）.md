OpenClover覆盖内核替换说明：

# 1. 配置
配置pom文件，添加插件

```xml
<plugin>
    <groupId>org.openclover</groupId>
    <artifactId>clover-maven-plugin</artifactId>
    <version>4.4.1</version>    
    <configuration>      
        <generateXml>true</generateXml>
        <generatePdf>false</generatePdf>
        <generateHtml>false</generateHtml>
        <generateJson>false</generateJson>
    </configuration> 
</plugin>
```
其中`configuration`确保仅生成xml格式的覆盖率报告

# 2. 运行
运行maven命令，生成覆盖率信息xml文件
```
mvn clean clover:setup test clover:aggregate clover:clover
```
其中
- `clean`：清除已经编译的字节码文件，确保所有代码被重新编译；
- `clover:setup`：初始化插件以及对源码进行插桩
- `test`： 编译代码，运行用例以及记录覆盖率信息-
- `clover:aggregate`：整合覆盖率信息
- `clover:clover`：生成覆盖率报告（XML格式）

# 3. 转换
将xml文件转换为node文件

首先解析xml文件，将信息提取，
```java
ParseXML parseXML = new ParseXML();
        List<List<List<String>>> xmlResult = parseXML.parseXML(inputXMLFilePath);
        List<List<String>> nodesList = xmlResult.get(0);
        List<List<String>> colorsList = xmlResult.get(1);
        List<List<String>> edgesList = xmlResult.get(2);
```
其中**参数**`inputXMLFilePath`是xml文件的地址
之后生成三个node文件，分别是catchNode， nodeJson和colorNode，以第一个文件为例，传入上个步骤获得的列表，生成json文件信息
```java
JSONArray catchNodeJsonArray = parseXML.getCatchNode(nodesList);
```
最后将信息流写入json文件，
```java
parseXML.writeJSONString(catchNodeJsonArray.toString(), outputCatchNodeFilePath);
```
其中**参数**`outputCatchNodeFilePath`为输出的json文件路径



# 4. 打包

上诉程序打包后生成`ParseXml.jar`,传入四个参数生成需要的node文件及覆盖率文件。
```powershell
java -jar jar_path  xml_path catch_node_path node_path color_node path
```
其中：

- `jar_path`：jar包的路径
- `xml_path`：插件生成的xml覆盖率文件
- `catch_node_path`：生成的catch_node文件存放路径
- `node_path`：生成的node文件存放路径
- `color_node_path`：生成的color着色文件存放路径

示例：

```powershell
java -jar C:\\ParseXml.jar C:\\clover.xml C:\\coverage.json C:\\project.json C:\\color.json
```