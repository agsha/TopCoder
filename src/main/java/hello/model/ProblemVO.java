package hello.model;

/**
 * Created by sharath on 5/19/15.
 */
public class ProblemVO {
    public String className;
    public String match;
    public String div;
    public String level;
    public String points;
    public String participants;
    public String open;
    public String submitted;
    public String correct;
    public String avgCorrectTime;
    public int problemId;

    public static ProblemVO fromProblem(Problem p) {
        ProblemVO vo = new ProblemVO();
        vo.className = p.className;
        vo.match = p.matchName;
        vo.problemId = p.problemId;

        vo.div = p.d1Level > 0 ? "Div 1":"";
        vo.div += p.d2Level > 0 ? " Div 2" : "";

        vo.level = p.d1Level > 0 ? "Div 1 level "+p.d1Level:"";
        vo.level += p.d2Level > 0 ? " Div 2 level"+p.d2Level : "";

        vo.points = p.d1Level > 0 ? "Div 1  "+p.d1PointValue:"";
        vo.points += p.d2Level > 0 ? " Div 2 "+p.d2PointValue : "";

        vo.participants = p.d1Level > 0 ? "Div 1  "+p.d1Competitors:"";
        vo.participants += p.d2Level > 0 ? " Div 2 "+p.d2Competitors : "";

        vo.open = p.d1Level > 0 ? "Div 1  "+p.d1PercentOpen:"";
        vo.open += p.d2Level > 0 ? " Div 2 "+p.d2PercentOpen : "";

        vo.submitted = p.d1Level > 0 ? "Div 1  "+p.d1PercentSubmitted:"";
        vo.submitted += p.d2Level > 0 ? " Div 2 "+p.d2PercentSubmitted : "";

        vo.correct = p.d1Level > 0 ? "Div 1  "+p.d1PercentCorrect:"";
        vo.correct += p.d2Level > 0 ? " Div 2 "+p.d2PercentCorrect : "";

        vo.avgCorrectTime = p.d1Level > 0 ? "Div 1  "+p.d1AverageCorrectSeconds:"";
        vo.avgCorrectTime += p.d2Level > 0 ? " Div 2 "+p.d2AverageCorrectSeconds : "";

        return vo;

    }

    @Override
    public String toString() {
        return "ProblemVO{" +
                "className='" + className + '\'' +
                ", match='" + match + '\'' +
                ", div='" + div + '\'' +
                ", level='" + level + '\'' +
                ", points='" + points + '\'' +
                ", participants='" + participants + '\'' +
                ", open='" + open + '\'' +
                ", submitted='" + submitted + '\'' +
                ", correct='" + correct + '\'' +
                ", avgCorrectTime='" + avgCorrectTime + '\'' +
                '}';
    }
}
