package hello.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import hello.domain.Main;
import hello.model.Problem;
import hello.topcoder.TypeRef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static hello.domain.Main.l;

/**
 * Created by sharath on 4/30/15.
 */
public class Types {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        Types types = new Types();
        try {
            types.go();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    public List<TestCase> parseTestCasesDirectory(Path testCaseDir) throws IOException {
        List<TestCase> list = new ArrayList<>();
        int testNumber = 0;
        while(true) {
            if(!Files.exists(testCaseDir.resolve(testNumber+".in"))) break;
            String input = new String(Files.readAllBytes(testCaseDir.resolve(testNumber+".in")));
            String output = new String(Files.readAllBytes(testCaseDir.resolve(testNumber+".out")));
            list.add(new TestCase(""+testNumber, input, output));
            testNumber++;
        }
        log.debug("processed {} testcases", testNumber);
        return list;
    }

    private void go() throws IOException {
        //fullParser();
        ObjectMapper mapper = new ObjectMapper();
        List<ProblemData> problemDataList = mapper.readValue(Paths.get("/Users/sharath/Documents/problemDataList").toFile(), new TypeReference<List<ProblemData>>(){});
        checkProblemList(problemDataList);
        //saveInDatabase(problemDataList);
    }

    public void checkProblemList(List<ProblemData> problemDataList) {
        List<String> failedClasses = new ArrayList<>();
        outer:for (ProblemData problemData : problemDataList) {
            String className = problemData.className;
            //processTestCases(problemData.sys, problemData.inputParamsCommaSeparated, className, failedClasses, SysOrDemo.SYS);
            processTestCases(problemData.demo, problemData.inputParams, className, failedClasses, (SysOrDemo.DEMO));
        }
        log.debug("{}", failedClasses);
    }

    public void saveInDatabase(List<ProblemData> problemDataList) {
        ObjectMapper mapper = new ObjectMapper();
        new Main.DbJob<Void>(session -> {
            Query query = session.createQuery("from Problem p where p.className = :className");
            int index = 0;
            for (ProblemData problemData : problemDataList) {
                query.setParameter("className", problemData.className);
                Problem problem = (Problem)query.uniqueResult();
                problem.methodName = problemData.methodName;
                problem.inputParamsCommaSeparated = Joiner.on(",").join(Arrays.asList(problemData.inputParams));
                problem.returnType = problemData.returnParams;
                try {
                    problem.demoTestJson = mapper.writeValueAsString(problemData.demo).getBytes();
                    problem.sysTestsJson = mapper.writeValueAsString(problemData.sys).getBytes();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                session.update(problem);
                index++;
                if ( index % 20 == 0 ) { //20, same as the JDBC batch size
                    //flush a batch of inserts and release memory:
                    session.flush();
                    session.clear();
                    session.getTransaction().commit();
                    session.beginTransaction();
                }
                log.debug("processed "+index+" of "+problemDataList.size());
            }
            return null;
        }).doWork();
    }

    public enum SysOrDemo {
        SYS, DEMO
    }

    private void processTestCases(List<TestCase> testCases, String[]inputParams, String className, List<String> failedClasses, SysOrDemo sysOrDemo) {
        int index = 0;
        for (TestCase testCase : testCases) {
            processSingleTestcase(index++, inputParams, className, failedClasses, sysOrDemo, testCase);
        }
    }

    private void processSingleTestcase(int index, String[] inputParams, String className, List<String> failedClasses, SysOrDemo sysOrDemo, TestCase testCase) {
        SharathSysReader reader;
        if(sysOrDemo.equals(SysOrDemo.SYS)) {
            reader = new SharathSysReader(new StringReader(testCase.input));
        } else {
            reader = new SharathDemoReader(new StringReader(testCase.input));
        }
        try {
            getTypesFRomSharathReader(inputParams, reader);
        }catch (Exception e) {
            failedClasses.add(className);
            log.debug("/Users/sharath/projects/tcproblems/{}/data/demo/{}.in {}, {}, {}, {}, {}", className, testCase.id, inputParams, testCase.input);
            throw e;
        }
    }

    public void fullParser() throws IOException {
        List<ProblemData> problemDataList = new ArrayList<>();

        Files.walkFileTree(Paths.get("/Users/sharath/projects/tcproblems"), ImmutableSet.of(), 1, new Main.LambdaFileVisitor((dir, blah) -> {
            log.debug("{}", dir);
            if(!Files.exists(dir.resolve("prob"))) return;
            String className = dir.getFileName().toString();
            log.debug("processing {}", className);
            Path data = dir.resolve("data");
            ProblemData problemData = new ProblemData();
            problemData.className = className;

            try {
                parse(dir.resolve("prob").resolve(className + ".html"), problemData);
                problemData.demo = parseTestCasesDirectory(data.resolve("demo"));
                problemData.sys = parseTestCasesDirectory(data.resolve("sys"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            problemDataList.add(problemData);
        }));

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get("/Users/sharath/Documents/problemDataList").toFile(), problemDataList);
    }

    public static class ProblemData {
        public String className;
        public String methodName;
        public String[] inputParams;
        public String returnParams;
        public List<TestCase> demo = new ArrayList<>();
        public List<TestCase> sys = new ArrayList<>();
    }

    public static class TestCase {
        public String id;
        public String input;
        public String output;

        public TestCase(String id, String input, String output) {
            this.id = id;
            this.input = input;
            this.output = output;
        }

        public TestCase() {
        }
    }


    public void parse(Path p, ProblemData problemData) throws IOException {
        Document doc = Jsoup.parse(p.toFile(), "UTF-8");
        Element ul = doc.select("h2:containsOwn(Definitions)").get(0).nextElementSibling();
        ul.child(2);
        Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
        problemData.methodName = ul.child(1).child(1).text();
        problemData.inputParams = Iterables.toArray(splitter.split(ul.child(2).child(1).text()), String.class);
        problemData.returnParams = ul.child(3).child(1).text();
    }
/*
    public Object[] getTypesSharathStyle(String s, String[] classesStr) throws IOException {
        Class[] types = getClasses(classesStr);
        ObjectMapper mapper = new ObjectMapper();
        Object[] parsed = new Object[types.length];
        String[] split = s.split("\n");
        assert types.length == split.length;
        for (int i = 0; i < types.length; i++) {
            if(i<types.length-1) split[i] = split[i].substring(0, split[i].length()-1);
            Class type = types[i];
            parsed[i] = mapper.readValue(new StringReader(split[i]), type);
        }
        return parsed;
    }
*/

    public Object[] getTypesFRomSharathReader(String[] classesStr, SharathSysReader reader) {
        log.debug("{}, {}", Arrays.toString(classesStr), classesStr.length);
        Class[] types = getClasses(classesStr);
        Object[]parsed = new Object[types.length];

        for(int i=0; i<parsed.length; i++) {
            Type type = types[i];
            log.debug(l("trying to parse", type.getTypeName()));
            if(type.equals(int.class)) {
                parsed[i] = reader.nextInt();
            } else if(type.equals(double[].class)) {
                parsed[i] = reader.nextDoubleArray();
            } else if(type.equals(String[].class)) {
                parsed[i] = reader.nextStringArray();
            }else if(type.equals(long[].class)) {
                parsed[i] = reader.nextLongArray();
            }else if(type.equals(int[].class)) {
                parsed[i] = reader.nextIntArray();
            }else if(type.equals(double.class)) {
                parsed[i] = reader.nextDouble();
            }else if(type.equals(boolean.class)) {
                parsed[i] = reader.nextBoolean();
            }else if(type.equals(char.class)) {
                parsed[i] = reader.nextChar();
            }else if(type.equals(String.class)) {
                parsed[i] = reader.nextString();
            }else if(type.equals(long.class)) {
                parsed[i] = reader.nextLong();
            } else {
                log.debug("{}", Arrays.toString(types));
                throw new RuntimeException("Unknown type" +type.getTypeName());
            }
        }
        return parsed;
    }
    public Object[] getTypesFromParameterReader(String s, String[] classesStr, ParameterReader reader) throws IOException {
        Type[] types = getTypes(classesStr);
        Object[]parsed = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            parsed[i] = reader.next(type);
            if(i<types.length-1) {
                reader.next();
            }
            if (type instanceof ParameterizedType) {
                ParameterizedType temp = (ParameterizedType) type;
                if (temp.getRawType().equals(List.class)) {
                    Type innerType = temp.getActualTypeArguments()[0];
                    if(innerType.equals(String.class)) {
                        parsed[i] = ((List<String>)parsed[i]).toArray(new String[0]);
                    } else if(innerType.equals(Integer.class)) {
                        List<Integer> list = (List<Integer>)parsed[i];
                        int[] rawList = new int[list.size()];
                        for(int j=0; j<rawList.length; j++) {
                            rawList[j] = list.get(j);
                        }
                        parsed[i] = rawList;
                    } else if(innerType.equals(Double.class)) {
                        List<Double> list = (List<Double>)parsed[i];
                        double[] rawList = new double[list.size()];
                        for(int j=0; j<rawList.length; j++) {
                            rawList[j] = list.get(j);
                        }
                        parsed[i] = rawList;

                    }
                }
            }
        }
        return parsed;
    }
//
//    public Object[] getTypesWithObjectMapper(String s, String[] classesStr) throws IOException {
//        s = s.replaceAll("\\n", "");
//        Type[] types = getClasses(classesStr);
//        ObjectMapper mapper = new ObjectMapper();
//        String regex = "";
//        Object[]parsed = new Object[types.length];
//        for (int i = 0; i < types.length; i++) {
//            Type type = types[i];
//            if(ImmutableList.of(
//                    boolean.class,
//                    double.class,
//                    int.class,
//                    char.class,
//                    long.class).indexOf(aClass) >=0 ) {
//                regex+="(.*),";
//            } else if (aClass==String.class) {
//                regex+="(\".*\"),";
//
//            }   else {
//                regex+="(\\[.*\\]),";
//            }
//        }
//        regex = regex.substring(0, regex.length()-1);
//        regex+="$";
//        Pattern p = Pattern.compile(regex);
//        Matcher matcher = p.matcher(s);
//        if(matcher.find()) {
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                // Jackson cannot handle single quotes in json for char.
//                if(types[i]==char.class) {
//                    try {
//                        parsed[i] = matcher.group(i + 1).charAt(0);
//                    } catch(StringIndexOutOfBoundsException e) {
//                        log.debug("{}, {}", s, types);
//                    }
//                } else {
//                    parsed[i] = mapper.readValue(matcher.group(i+1), types[i]);
//                }
//            }
//        } else {
//            throw new RuntimeException("The regex did not match the inputParamsCommaSeparated string: regex:"+regex+" string:"+s);
//        }
//        return parsed;
//    }


    private Type[] getTypes(String[] classesStr) {
        Type[] classes = new Type[classesStr.length];
        for (int i = 0; i < classesStr.length; i++) {
            String s = classesStr[i];
            switch (s) {
                case "boolean":
                    classes[i] = Boolean.class;
                    break;
                case "double":
                    classes[i] = Double.class;
                    break;
                case "char":
                    classes[i] = Character.class;
                    break;
                case "String":
                    classes[i] = String.class;
                    break;
                case "int":
                    classes[i] = Integer.class;
                    break;
                case "long":
                    classes[i] = Long.class;
                    break;
                case "double[]":
                    classes[i] = new TypeRef<List<Double>>() {}.getType();
                    break;
                case "long[]":
                    classes[i] =  new TypeRef<List<Long>>() {}.getType();
                    break;
                case "int[]":
                    classes[i] =  new TypeRef<List<Integer>>() {}.getType();
                    break;
                case "String[]":
                    classes[i] =  new TypeRef<List<String>>() {}.getType();
                    break;
            }
        }
        return classes;
    }


    private Class[] getClasses(String[] classesStr) {
        Class[] classes = new Class[classesStr.length];
        for (int i = 0; i < classesStr.length; i++) {
            String s = classesStr[i];
            switch (s) {
                case "boolean":
                    classes[i] = boolean.class;
                    break;
                case "double":
                    classes[i] = double.class;
                    break;
                case "char":
                    classes[i] = char.class;
                    break;
                case "String":
                    classes[i] = String.class;
                    break;
                case "int":
                    classes[i] = int.class;
                    break;
                case "long":
                    classes[i] = long.class;
                    break;
                case "double[]":
                    classes[i] =double[].class;
                    break;
                case "long[]":
                    classes[i] =  long[].class;
                    break;
                case "int[]":
                    classes[i] =  int[].class;
                    break;
                case "String[]":
                    classes[i] =  String[].class;
                    break;
            }
        }
        return classes;
    }
}
