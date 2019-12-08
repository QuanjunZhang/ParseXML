import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParseXML {

    /**
     * 将node列表转成Node，，以写入json文件
     * @param nodesList 列表：包含每个node的具体信息，名称，位置，覆盖次数等
     * @param edgesList 列表：包含每个有向边的信息，出发点，结束点，边的值
     * @return JSONObject，包含三个JSONArray，分别是覆盖粒度类型，node具体信息，有向边的具体信息
     */
    public JSONObject getNode(List<List<String>> nodesList, List<List<String>> edgesList){
        String[] categories = {"statement", "branch", "method"};
        JSONObject result = new JSONObject(true);

        JSONArray categoriesJson = new JSONArray();
        JSONArray nodesJson = new JSONArray();
        JSONArray edgesJson = new JSONArray();

        //Category
        for (String category : categories) {
            JSONObject tmp = new JSONObject(true);
            tmp.put("name", category);
            categoriesJson.add(tmp);
        }

        //Node
        for (List<String> nodeList : nodesList){
            JSONObject node = new JSONObject(true);
            node.put("name", nodeList.get(0));
            node.put("category", nodeList.get(1));
            node.put("location", nodeList.get(2));
            nodesJson.add(node);
        }

        //Edge
        for (List<String> edgeList : edgesList){
            JSONObject node = new JSONObject(true);
            node.put("source", edgeList.get(0));
            node.put("target", edgeList.get(1));
            node.put("value", "branch");
            edgesJson.add(node);
        }

        result.put("categories", categoriesJson);
        result.put("nodes", nodesJson);
        result.put("edges", edgesJson);
        return result;

    }

    /**
     * 将node列表转成catchNode，，以写入json文件
     * @param nodesList 列表：包含每个node的具体信息，名称，位置，覆盖次数等
     * @return 转换后的JSONArray，包含node名称，类型以及是否被覆盖
     */
    public JSONArray getCatchNode(List<List<String>> nodesList){

        JSONArray result = new JSONArray();
        for (List<String> nodeList : nodesList) {
            JSONObject catchNode = new JSONObject(true);
            catchNode.put("nodeName", nodeList.get(0));
            catchNode.put("category", nodeList.get(1));
            catchNode.put("ifCatch", nodeList.get(3).equals("0") ? false : true);

            result.add(catchNode);
        }

        return result;
    }

    /**
     * 将color列表转换成color node信息，以写入json文件
     * @param colorsList 列表：包含每个node的颜色信息
     * @return 转换后的JSONArray，包含node位置和其颜色
     */
    public JSONArray getColorNode(List<List<String>> colorsList){

        JSONArray result = new JSONArray();

        for (List<String> colorList : colorsList) {
            JSONObject catchNode = new JSONObject(true);
            catchNode.put("location", colorList.get(0));
            catchNode.put("color", colorList.get(1));

            result.add(catchNode);
        }
        return result;
    }

    /**
     * 覆盖信息文件解析，xml格式
     * @param fileName 需要解析的xml文档
     * @return 返回三个列表，分别是node，edge 和 color列表
     */
    public List<List<List<String>>>  parseXML(String fileName){

        File inputXml = new File(fileName);
        SAXReader saxReader = new SAXReader();
        List<List<String>> nodesList = new ArrayList<>(); // element: name, type, location, count
        List<List<String>> colorsList = new ArrayList<>(); // element: location, color(0,1,2)
        List<List<String>> edgesList = new ArrayList<>(); // element: branch-true, branch-false
        int numStatement = 0, numBranch = 0, numMethod = 0;

        try {
            Document document = saxReader.read(inputXml);
            Element nodeRoot = document.getRootElement();
            Element nodeProject = nodeRoot.element("project");
            for (Iterator iteratorPackage = nodeProject.elementIterator("package"); iteratorPackage.hasNext();) {
                Element nodePackage = (Element) iteratorPackage.next();
                String namePackage = nodePackage.attributeValue("name").replace(".", File.separator);
                for (Iterator iteratorFile = nodePackage.elementIterator("file"); iteratorFile.hasNext();) {
                    Element nodeFile = (Element) iteratorFile.next();
                    String nameFile = nodeFile.attributeValue("name");
                    String methodInfo = null;
                    for (Iterator iteratorLine = nodeFile.elementIterator("line"); iteratorLine.hasNext();) {
                        Element nodeLine = (Element) iteratorLine.next();

                        String lineType = nodeLine.attributeValue("type");
                        String lineIndex = namePackage + File.separator + nameFile + File.separator + nodeLine.attributeValue("num");
                        List nodeList;
                        List colorList;
                        List edgeList;
                        switch (lineType){
                            case "stmt":
                                String lineCount = nodeLine.attributeValue("count");
                                String lineName = "statement-" + ++numStatement + "-" + methodInfo;

                                //node
                                nodeList= new ArrayList();
                                nodeList.add(lineName);
                                nodeList.add("statement");
                                nodeList.add(lineIndex);
                                nodeList.add(lineCount);
                                nodesList.add(nodeList);

                                //color
                                colorList= new ArrayList();
                                colorList.add(lineIndex);
                                colorList.add(lineCount.equals("0") ? "0" : "2");
                                colorsList.add(colorList);
                                break;
                            case "cond":
                                String branchFalseCount = nodeLine.attributeValue("falsecount");
                                String branchTrueCount = nodeLine.attributeValue("truecount");

                                String branchTrueName = "branch-" + ++numBranch + "-" + methodInfo;
                                String branchFalseName = "branch-" + ++numBranch + "-" + methodInfo;

                                //node
                                nodeList = new ArrayList();
                                nodeList.add(branchTrueName);
                                nodeList.add("branch");
                                nodeList.add(lineIndex + "-true");
                                nodeList.add(branchTrueCount);
                                nodesList.add(nodeList);

                                nodeList = new ArrayList();
                                nodeList.add(branchFalseName);
                                nodeList.add("branch");
                                nodeList.add(lineIndex + "-false");
                                nodeList.add(branchFalseCount);
                                nodesList.add(nodeList);

                                //color
                                colorList= new ArrayList();
                                colorList.add(lineIndex);
                                colorList.add(branchTrueCount.equals("0") && branchFalseCount.equals("0") ? "0" : !branchTrueCount.equals("0") && !branchFalseCount.equals("0") ? 2 :1);
                                colorsList.add(colorList);

                                //edge
                                edgeList = new ArrayList();
                                edgeList.add(branchTrueName);
                                edgeList.add(branchFalseName);
                                edgesList.add(edgeList);
                                break;
                            case "method":
                                String methodCount = nodeLine.attributeValue("count");
                                methodInfo = nodeLine.attributeValue("signature");
                                String methodName = "method-" + ++numMethod + "-" + methodInfo;

                                //node
                                nodeList= new ArrayList();
                                nodeList.add(methodName);
                                nodeList.add("method");
                                nodeList.add(lineIndex);
                                nodeList.add(methodCount);
                                nodesList.add(nodeList);

                                //color
                                colorList= new ArrayList();
                                colorList.add(lineIndex);
                                colorList.add(methodCount.equals("0") ? "0" : "2");
                                colorsList.add(colorList);

                                break;
                            default:
                                break;
                        }

                    }
                }
            }

        } catch (DocumentException e) {
            System.out.println(e.getMessage());
        }

        List<List<List<String>>> xmlResult = new ArrayList<>();
        xmlResult.add(nodesList);
        xmlResult.add(colorsList);
        xmlResult.add(edgesList);
        return xmlResult;

    }

    /**
     * 写入json字符串
     * @param jsonString 需要写入的node信息
     * @param outputFileName 需要写入的文件
     */
    public void writeJSONString(String jsonString, String outputFileName){
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(outputFileName));
            output.write(jsonString);
            output.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void main(String[] args){

        //定义文件路径
//        String inputXMLFilePath = args[0];//"clover.xml"
//        String outputCatchNodeFilePath = args[1];//"coverage.json"
//        String outputNodeFilePath = args[2];//"project.json"
//        String outputColorNodeFilePath = args[3];//"color.json"

        String inputXMLFilePath = "clover.xml";
        String outputCatchNodeFilePath = "coverage.json";
        String outputNodeFilePath = "project.json";
        String outputColorNodeFilePath = "color.json";

        //解析xml覆盖文档
        ParseXML parseXML = new ParseXML();
        List<List<List<String>>> xmlResult = parseXML.parseXML(inputXMLFilePath);
        List<List<String>> nodesList = xmlResult.get(0);
        List<List<String>> colorsList = xmlResult.get(1);
        List<List<String>> edgesList = xmlResult.get(2);

        //获得catchNode
        JSONArray catchNodeJsonArray = parseXML.getCatchNode(nodesList);
        parseXML.writeJSONString(catchNodeJsonArray.toString(), outputCatchNodeFilePath);

        //获得node
        JSONObject nodeJsonObject = parseXML.getNode(nodesList, edgesList);
        parseXML.writeJSONString(nodeJsonObject.toString(), outputNodeFilePath);

        //获得colorNode
        JSONArray colorNodeJsonArray = parseXML.getColorNode(colorsList);
        parseXML.writeJSONString(colorNodeJsonArray.toString(), outputColorNodeFilePath);

    }
}
