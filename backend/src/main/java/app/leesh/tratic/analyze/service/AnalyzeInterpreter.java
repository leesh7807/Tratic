package app.leesh.tratic.analyze.service;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;

public interface AnalyzeInterpreter {
    AnalyzeInterpretation interpret(AnalyzeResult result);
}
