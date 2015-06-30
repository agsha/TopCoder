package hello.web;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import hello.crawler.Types;
import hello.domain.CaseTester;
import hello.domain.Main;
import hello.model.Problem;
import hello.model.ProblemVO;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static hello.domain.Main.l;

/**
 * Created by sharath on 5/15/15.
 */
public class Controller {
    private static final Logger log = LogManager.getLogger();

    public List<Problem> resultList(String query) {
        return ImmutableList.of();
    }

    public String echo(String message) {
        return "Hello "+message;
    }

    public List<ProblemVO> getResults(String query) {
        String fullQuery = "select * from Problem p where "+query;
        log.debug("full query is {}", fullQuery);
        List<ProblemVO> problemList = new Main.DbJob<List<ProblemVO>>(session -> {
            Query sessionQuery = session.createSQLQuery(fullQuery).addEntity(Problem.class);
            List<Problem> list = sessionQuery.list();
            return list.stream().map(ProblemVO::fromProblem).collect(Collectors.toList());
        }).doWork();
        return problemList;
    }

    public Optional<String> runSystemTest(String javaCode, Integer problemId) {
        Optional<Integer> optPid = Optional.ofNullable(problemId);
        Problem problem = new Main.DbJob<>(session -> {
            if(optPid.isPresent()) return Problem.fromId(session, optPid.get());
            String className = getClassName(javaCode);
            return Problem.fromName(session, className);
        }).doWork();
        try {
            List<String> failures = compileAndRunSystemTest(problem, javaCode);
            if(failures.size()>0) return Optional.of(Joiner.on(",").join(failures));
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(ExceptionUtils.getStackTrace(e));
        }
    }

    String getClassName(String javaCode) {
        String regex = "public\\s+class\\s+(\\w+)\\s+\\{";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(javaCode);
        if(!m.find()) {
            throw new RuntimeException("could not find className from javacode "+javaCode);
        }
        String className = m.group(1);
        return className;
    }

    List<String> compileAndRunSystemTest(Problem problem, String javaCode) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        CaseTester ct = new CaseTester();
        Map<String, CaseTester.ClassBytes> classBytesMap = ct.compile(javaCode, problem.className);
        ObjectMapper mapper = new ObjectMapper();
        List<Types.TestCase> list = mapper.readValue(problem.sysTestsJson, new TypeReference<List<Types.TestCase>>() {
        });
        List<String> failures = new ArrayList<>();
        int ind = 0;
        for (Types.TestCase testCase : list) {
            CaseTester.ParamsReturns paramsAndReturn = ct.getParamsAndReturn(problem, testCase, Types.SysOrDemo.SYS);
            Optional<String> result = ct.testACase(classBytesMap, problem.className, problem.methodName, paramsAndReturn.params, paramsAndReturn.returns);
            if(result.isPresent()) {
                String paramsString = "";
                for (Object param : paramsAndReturn.params) {
                    paramsString += l(param);
                }
                String failure = String.format("Case #%d failed.\nparams:\n%s\n%s", ind, paramsString, result.get());
                failures.add(failure);
            }
            ind++;
        }
        return failures;
    }

    public GetStubFileForProblemRet getStubFileForProblem(String problemIdStr) {
        Integer pid = Ints.tryParse(problemIdStr);
        if(pid==null) throw new RuntimeException("invalid problemId");
        Problem problem = Problem.fromId(pid);
        if(problem==null) throw new RuntimeException("invalid problemId");
        String java = String.format("import java.util.*;\npublic class %s {\n\t%s {\n\t}\n}", problem.className, problem.signature);
        return new GetStubFileForProblemRet(java, problem.className+".java");
    }

    static class GetStubFileForProblemRet {
        public String fileContent;
        public String fileName;

        public GetStubFileForProblemRet(String fileContent, String fileName) {
            this.fileContent = fileContent;
            this.fileName = fileName;
        }
    }
}
