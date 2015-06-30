package hello.domain;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import hello.crawler.SharathDemoReader;
import hello.crawler.SharathDemoReturnReader;
import hello.crawler.SharathSysReader;
import hello.crawler.Types;
import hello.model.CoderSolutions;
import hello.model.FastestJavaSolutionLink;
import hello.model.Problem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.jsoup.examples.HtmlToPlainText;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hello.domain.Main.l;
/**
 * Created by sharath on 5/7/15.
 */

public class CaseTester {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        CaseTester caseTester = new CaseTester();
        try {
            caseTester.temp();
            //caseTester.go(new int[]{6212}, true);
            //caseTester.getParamsAndReturns("IntSplit");
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    private void temp() {
        new Main.DbJob<Void>(session -> {

            try {
                List<String> strings = Files.readAllLines(Paths.get("/Users/sharath/projects/TopCoder/hello.domain.CaseTester.printFailures.txt"));
                Pattern p = Pattern.compile("Test case failed\\. className: \\w+, pid: ([\\d]+), testcase");
                for (String string : strings) {
                    Matcher matcher = p.matcher(string);
                    if(matcher.find()) {
                        int pid = Integer.parseInt(matcher.group(1));
                        log.debug("updating {}", pid);
                        Problem problem = (Problem) session.get(Problem.class, pid);
                        problem.skip = true;
                        session.update(problem);
                    }
                }
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).doWork();
    }

    JavaCompiler compiler;
    StandardJavaFileManager fileManager;

    public CaseTester() {
        compiler = ToolProvider.getSystemJavaCompiler();
        fileManager = compiler.getStandardFileManager(null, null, null);
    }

    public void go(int[] problemIds, boolean keepgoing) {
        Problem[] problems = new Problem[1];List<Exception> failures = new ArrayList<>();

        new Main.DbJob<Void>(session -> {
            for (Integer problemId : problemIds) {
                problems[0] = (Problem)session.get(Problem.class, problemId);
                //problems[0] = (Problem)session.createQuery("from Problem p left join fetch p.coderSolutions where p.sysTestsJson is not null and p.demoTestJson is not null and p.className = :c").setParameter("c", className).uniqueResult();
                go(problems[0], 0, failures, keepgoing);
            }

            return null;
        }).doWork();
        printFailures(failures);
    }
    public void go(Problem problem, int ind, List<Exception> failures, boolean keepGoing) {
        ObjectMapper mapper = new ObjectMapper();
        log.debug("completed problems: {}, failures: {}", ind++, failures.size());

        Set<CoderSolutions> coderSolutions = problem.coderSolutions;
        for (CoderSolutions cs : coderSolutions) {
            String code = "";
            Map<String, ClassBytes> classToBytesMap = null;
            Object[][] params = new Object[1][];
            Object[] returns = new Object[1];
            Consumer<Exception> logger = e -> {
                if(keepGoing) {
                    failures.add(e);
                    return;
                }
                String str = returns[0] instanceof Object[] ? Arrays.toString((Object[])returns[0]) : l(returns[0]);
                throw new RuntimeException(String.format("Error occured: classname: %s, problemId: %s, params: %s, expected: %s,", problem.className, problem.problemId, Arrays.deepToString(params[0]), str), e);
            };
            int testCaseId = 0;
            Optional<String> s = Optional.empty();
            try {
                code =  new String(cs.code);

                classToBytesMap = compile(code, problem.className);
                List<Types.TestCase> list = mapper.readValue(problem.sysTestsJson, new TypeReference<List<Types.TestCase>>() {
                });
                if(list.size()==0) return;
                for (Types.TestCase testCase : list) {
                    log.debug("processing test case #{} of {}", testCaseId, list.size());
                    ParamsReturns paramsAndReturn = getParamsAndReturn(problem, testCase, Types.SysOrDemo.DEMO);
                    params[0] = paramsAndReturn.params;
                    returns[0] = paramsAndReturn.returns;
                    s = testACase(classToBytesMap, problem.className, problem.methodName, params[0], returns[0]);
                    if(s.isPresent()) throw new RuntimeException(s.get()) ;
                    testCaseId++;
                }

            } catch (Exception e) {
                logger.accept(new RuntimeException(String.format("Test case failed. className: %s, pid: %s, testcase#: %s, paramTypes:%s, params:%s, returnType; %s, message:%s, testCaseFile: %s", problem.className, problem.problemId, testCaseId, problem.inputParamsCommaSeparated, l(params[0]), problem.returnType, s.toString(), "/Users/sharath/projects/tcproblems/"+problem.className+"/demo/"+testCaseId+".in"), e));
            }
        }
    }

    public void go(boolean keepgoind) {
        List<Problem> problemList = new ArrayList<>();
        new Main.DbJob<Void>(session -> {
            Types types = new Types();
            problemList.addAll(session.createQuery("from Problem p where p.sysTestsJson is not null and p.demoTestJson is not null and p.skip = false").list());
            int ind = 0;
            List<Exception> failures = new ArrayList<>();
            outer:for (Problem problem : problemList) {
                go(problem, ind++, failures, keepgoind);
            }
            printFailures(failures);
            log.debug("{}", failures);

            return null;

        }).doWork();

    }

    private void printFailures(List<Exception> failures) {
        for (Exception failure : failures) {
            log.error("-------------------------------", failure);
        }
    }

    public void getParamsAndReturns(String className) {
        new Main.DbJob<Void>(session -> {
            Problem problem = (Problem) session.createQuery("from Problem p where p.className=:c").setParameter("c", className).uniqueResult();
            //problem.coderSolutions.forEach(cs -> log.debug("{}", new String(cs.code)));
            ObjectMapper mapper = new ObjectMapper();
            try {
                //List<Types.TestCase> testCases = mapper.readValue(problem.demoTestJson, new TypeReference<List<Types.TestCase>>() {});
                List<Types.TestCase> testCases = mapper.readValue(problem.sysTestsJson, new TypeReference<List<Types.TestCase>>() {});
                ParamsReturns paramsAndReturn = getParamsAndReturn(problem, testCases.get(0), Types.SysOrDemo.DEMO);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }).doWork();
    }
    public ParamsReturns getParamsAndReturn(Problem problem, Types.TestCase testCase, Types.SysOrDemo sysOrDemo) {
        StringReader pr = new StringReader(testCase.input);
        StringReader rr = new StringReader(testCase.output);
        SharathSysReader paramsReader = sysOrDemo.equals(Types.SysOrDemo.SYS)?new SharathSysReader(pr) : new SharathDemoReader(pr);
        SharathSysReader returnReader = sysOrDemo.equals(Types.SysOrDemo.SYS)?new SharathSysReader(rr) : new SharathDemoReturnReader(rr);
        Types types = new Types();
        Object[] typesFRomSharathReader = types.getTypesFRomSharathReader(problem.inputParamsCommaSeparated.split(","), paramsReader);
        log.debug("parsing return types from {}", testCase.output);
        Object[] returnTypesFRomSharathReader = types.getTypesFRomSharathReader(new String[]{problem.returnType}, returnReader);
        log.debug(l(Arrays.deepToString(typesFRomSharathReader), returnTypesFRomSharathReader[0]));
        return new ParamsReturns(typesFRomSharathReader,
        returnTypesFRomSharathReader[0]);
    }

    public static class ParamsReturns {
        public Object[] params;
        public Object returns;
        public ParamsReturns(Object[] params, Object returns) {
            this.params = params;
            this.returns = returns;
        }
    }

    public Map<String, ClassBytes> compile(String javaCode, String className) throws ClassNotFoundException {
        //log.debug("{}", javaCode);
        JavaSourceFromString jsfs = new JavaSourceFromString( className, javaCode);

        MyFileManager  myFileManager = new MyFileManager(compiler.getStandardFileManager(null, null, null));
        Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(jsfs);
        boolean success = compiler.getTask(null, myFileManager, null, null, null, fileObjects).call();
        return myFileManager.classNameToBytesMap;
    }

    public Optional<String> testACase(Map<String, ClassBytes> classToBytesMap, String className, String methodName, Object[] params, Object expected) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        MyClassLoader classLoader = new MyClassLoader(classToBytesMap);
        Class<?> aClass = classLoader.findClass(className);
        Object o = aClass.newInstance();
        Class[] classes = new Class[params.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = getClass(params[i]);
            //log.debug("{}, {}, {}, {}", classes[i].getCanonicalName(), classes[i].getSimpleName(), classes[i].getName(), classes[i].getTypeName());
        }
        Method method = aClass.getDeclaredMethod(methodName, classes);
        Object ret = method.invoke(o, params);
        return check(ret, expected);
    }

    private Class getClass(Object param) {
        if(param instanceof Integer) return int.class;
        if(param instanceof Double) return double.class;
        if(param instanceof Long) return long.class;
        if(param instanceof Character) return char.class;
        return param.getClass();
    }

    private Optional<String> check(Object actual, Object expected) {
        Class c = actual.getClass();

        if(actual instanceof double[]) {
            double[] a = (double[])actual;
            double[] e = (double[])expected;
            if(a.length!=e.length) {
                return Optional.of("Lengths don't match. Expected\n"+l(e)+"\ngot\n"+l(a));
            }
            for (int i = 0; i < e.length; i++) {
                double aa = a[i];
                double ee = e[i];
                if(!doubleEquals(aa, ee)) {
                    return Optional.of(MessageFormat.format("Differs at position {0}.\nExpected:\n{1}\n Got:\n{2}", i, l(e), l(a)));
                }
            }
            return Optional.empty();
        } else if (actual instanceof long[]) {
            long[] a = (long[])actual;
            long[] e = (long[])expected;
            if(a.length!=e.length) {
                return Optional.of("Lengths don't match. Expected\n"+l(e)+"\ngot\n"+l(a));
            }
            for (int i = 0; i < e.length; i++) {
                long aa = a[i];
                long ee = e[i];
                if(aa!=ee) {
                    return Optional.of(MessageFormat.format("Differs at position {0}.\nExpected:\n{1}\n Got:\n{2}", i, l(e), l(a)));
                }
            }
            return Optional.empty();
        } else if (actual instanceof int[]) {
            int[] a = (int[])actual;
            int[] e = (int[])expected;
            if(a.length!=e.length) {
                return Optional.of("Lengths don't match. Expected\n"+l(e)+"\ngot\n"+l(a));
            }
            for (int i = 0; i < e.length; i++) {
                int aa = a[i];
                int ee = e[i];
                if(aa!=ee) {
                    return Optional.of(MessageFormat.format("Differs at position {0}.\nExpected:\n{1}\n Got:\n{2}", i, l(e), l(a)));
                }
            }
            return Optional.empty();
        } else if (actual instanceof String[]) {
            String[] a = (String[])actual;
            String[] e = (String[])expected;
            if(a.length!=e.length) {
                return Optional.of("Lengths don't match. Expected\n"+l(e)+"\ngot\n"+l(a));
            }
            for (int i = 0; i < e.length; i++) {
                String aa = a[i];
                String ee = e[i];
                if(!Objects.equals(aa, ee)) {
                    return Optional.of(MessageFormat.format("Differs at position {0}.\nExpected:\n{1}\n Got:\n{2}", i, l(e), l(a)));
                }
            }
            return Optional.empty();
        } else if (Double.class.isInstance(actual)) {
            if(!doubleEquals((double)expected,(double)actual)) {
                log.debug("difference is " + Math.abs((double)expected-(double)actual));

                return Optional.of(MessageFormat.format("Expected {0}, got {1}", expected, actual));
            }
        }
        else {
            if(!Objects.equals(actual, expected)) {
                return Optional.of(MessageFormat.format("Expected {0}, got {1}", expected, actual));
            }
        }
        return Optional.empty();
    }

    private boolean doubleEquals(double a, double b) {
        log.debug(l("comparing doubles: Math.abs(a-b)", Math.abs(a-b)));
        if(Math.abs(a-b)<1e-8) return true;
        log.debug(l("comparing doubles: Math.abs(b)", Math.abs(b)));
        if(Math.abs(b)<1e-5) return false;
        double ratio = a/b;
        log.debug(l("comparing doubles: Math.abs(ratio-1)", Math.abs(ratio-1)));

        if(Math.abs(ratio-1)<1e-8) return true;
        return false;
    }
    public static class MyClassLoader extends ClassLoader {
        private Map<String, ClassBytes> nameToClassBytesMap;

        public MyClassLoader(Map<String, ClassBytes> nameToClassBytesMap) {
            this.nameToClassBytesMap = nameToClassBytesMap;
        }


        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if(!nameToClassBytesMap.containsKey(name)) {
                throw new RuntimeException(l("name doesnt match. expected one of ", this.nameToClassBytesMap.keySet(), "got ", name));
            }
            byte[] classBytes = nameToClassBytesMap.get(name).bos.toByteArray();
            return defineClass(name, classBytes, 0, classBytes.length);
        }

    }


    public static class MyFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private static final Logger log = LogManager.getLogger();
        public Map<String, ClassBytes> classNameToBytesMap = new HashMap<>();

        protected MyFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            if(kind == JavaFileObject.Kind.SOURCE) {
                throw new RuntimeException("trying to write source file");
            }
            URI uri = URI.create("string:///" + className.replace('.', '/'));
            ClassBytes cb =  new ClassBytes(uri, kind);
            classNameToBytesMap.put(className, cb);
            return cb;
        }
    }

    public static class ClassBytes extends SimpleJavaFileObject {
        public ByteArrayOutputStream bos;

        public ClassBytes(URI uri, Kind kind) {
            super(uri, kind);
            bos = new ByteArrayOutputStream();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return bos;
        }
    }

    public static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        public JavaSourceFromString( String name, String code) {
            super( URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
