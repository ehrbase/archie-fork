package com.nedap.archie.rules.evaluation;

import com.google.common.collect.ArrayListMultimap;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.creation.RMObjectCreator;
import com.nedap.archie.query.RMQueryContext;
import com.nedap.archie.rminfo.ModelInfoLookup;
import com.nedap.archie.rules.Expression;
import com.nedap.archie.rules.RuleElement;
import com.nedap.archie.rules.RuleStatement;
import com.nedap.archie.rules.evaluation.evaluators.AssertionEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.BinaryOperatorEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.ConstantEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.ForAllEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.FunctionEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.ModelReferenceEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.UnaryOperatorEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.VariableDeclarationEvaluator;
import com.nedap.archie.rules.evaluation.evaluators.VariableReferenceEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pieter.bos on 31/03/16.
 */
public class RuleEvaluation<T> {

    private static Logger logger = LoggerFactory.getLogger(RuleEvaluation.class);

    private Archetype archetype;
    private List<Evaluator> evaluators = new ArrayList<>();
    private HashMap<Class, Evaluator> classToEvaluator = new HashMap<>();

    //evaluation state
    private T root;
    private VariableMap variables;
    EvaluationResult evaluationResult;
    private List<AssertionResult> assertionResults;

    private RMQueryContext queryContext;

    private ArrayListMultimap<RuleElement, ValueList> ruleElementValues = ArrayListMultimap.create();
    private FixableAssertionsChecker fixableAssertionsChecker;

    private ModelInfoLookup modelInfoLookup;

    private RMObjectCreator creator;

    private final AssertionsFixer assertionsFixer;

    public RuleEvaluation(ModelInfoLookup modelInfoLookup, Archetype archetype) {
        this.modelInfoLookup = modelInfoLookup;
        this.creator = new RMObjectCreator(modelInfoLookup);
        assertionsFixer = new AssertionsFixer(this, creator);
        this.archetype = archetype;
        add(new VariableDeclarationEvaluator());
        add(new ConstantEvaluator());
        add(new AssertionEvaluator());
        add(new BinaryOperatorEvaluator(modelInfoLookup));
        add(new UnaryOperatorEvaluator());
        add(new VariableReferenceEvaluator());
        add(new ModelReferenceEvaluator());
        add(new ForAllEvaluator());
        add(new FunctionEvaluator());
    }

    private void add(Evaluator evaluator) {
        evaluators.add(evaluator);
        for(Object clazz: evaluator.getSupportedClasses()) {
            classToEvaluator.put((Class) clazz, evaluator);
        }
    }

    public EvaluationResult evaluate(T root, List<RuleStatement> rules) {
        this.root = (T) modelInfoLookup.clone(root);

        ruleElementValues = ArrayListMultimap.create();
        variables = new VariableMap();
        assertionResults = new ArrayList<>();
        evaluationResult = new EvaluationResult();
        queryContext = new RMQueryContext(modelInfoLookup, this.root);

        fixableAssertionsChecker = new FixableAssertionsChecker(ruleElementValues);

        for(RuleStatement rule:rules) {
            evaluate(rule);
        }
        return evaluationResult;

    }

    public ValueList evaluate(RuleElement rule) {
        Evaluator evaluator = classToEvaluator.get(rule.getClass());
        if(evaluator != null) {
            ValueList valueList = evaluator.evaluate(this, rule);
            ruleElementValueSet(rule, valueList);
            logger.debug("evaluated rule: {}", valueList);
            return valueList;
        }
        throw new UnsupportedOperationException("no evaluator present for rule type " + rule.getClass().getSimpleName());
    }

    public T getRMRoot() {
        return root;
    }

    public VariableMap getVariableMap() {
        return variables;
    }

    private void ruleElementValueSet(RuleElement expression, ValueList values) {
        ruleElementValues.put(expression, values);
    }

    public RMQueryContext getQueryContext() {
        return queryContext;
    }

    public void refreshQueryContext() {
        //updating a single node does not seem to work with the default JAXB-implementation, so just reload the entire query
        //context
        queryContext = new RMQueryContext(modelInfoLookup, root);
    }

    /**
     * Callback: an assertion has been evaluated with the given result
     */
    public void assertionEvaluated(String tag, Expression expression, ValueList valueList) {
        AssertionResult assertionResult = new AssertionResult();
        assertionResult.setTag(tag);
        assertionResult.setAssertion(expression);

        boolean result = valueList.getSingleBooleanResult();
        assertionResult.setResult(result);
        assertionResult.setRawResult(valueList);
        evaluationResult.addAssertionResult(assertionResult);

        fixableAssertionsChecker.checkAssertionForFixablePatterns(assertionResult, expression, 0);

        //Fix any assertions that should be fixed before processing the next rule
        //this means we can calculate a score, then use that score in the next rule
        //otherwise this would mean several passes through the evaluator
        assertionsFixer.fixAssertions(archetype, assertionResult);


        //before re-evaluation, reset any overridden existence from evaluation?
    }


    public EvaluationResult getEvaluationResult() {
        return evaluationResult;
    }

    public ModelInfoLookup getModelInfoLookup() {
        return modelInfoLookup;
    }
}
