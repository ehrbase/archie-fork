package com.nedap.archie.rmobjectvalidator;

import com.google.common.base.Joiner;
import com.nedap.archie.adlparser.modelconstraints.ReflectionConstraintImposer;
import com.nedap.archie.aom.*;
import com.nedap.archie.aom.utils.AOMUtils;
import com.nedap.archie.flattener.OperationalTemplateProvider;
import com.nedap.archie.query.RMObjectWithPath;
import com.nedap.archie.query.RMPathQuery;
import com.nedap.archie.rminfo.InvariantMethod;
import com.nedap.archie.rminfo.MetaModel;
import com.nedap.archie.rminfo.ModelInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import org.openehr.utils.message.I18n;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates a created reference model object, both against an Operational Template and against all model constraints.
 * If no archetype is given, validates against the model constraints only.
 * Created by pieter.bos on 15/02/16.
 */
public class RMObjectValidator extends RMObjectValidatingProcessor {

    private final MetaModel metaModel;
    private final OperationalTemplateProvider operationalTemplateProvider;
    private APathQueryCache queryCache = new APathQueryCache();
    private ModelInfoLookup lookup;
    private ReflectionConstraintImposer constraintImposer;
    private final ValidationConfiguration validationConfiguration;
    private boolean validateInvariants = true;
    private final RmOccurrenceValidator rmOccurrenceValidator;
    private final RmPrimitiveObjectValidator rmPrimitiveObjectValidator;
    private final RmTupleValidator rmTupleValidator;
    private final RmMultiplicityValidator rmMultiplicityValidator;

    /**
     * Creates an RM Object Validator with the given ModelInfoLook class, and the given OperationalTemplateProvider
     * The ModelInfoLookup is used for model access, and model specific constructions.
     * The OperationalTemplateProvider is used to retrieve other referenced archetypes in case of ArchetypeSlots.
     * @param lookup
     * @param provider
     * @deprecated Use {@link #RMObjectValidator(ModelInfoLookup, OperationalTemplateProvider, ValidationConfiguration)} instead.
     */
    @Deprecated
    public RMObjectValidator(ModelInfoLookup lookup, OperationalTemplateProvider provider) {
        this.lookup = lookup;
        this.metaModel = new MetaModel(lookup, null);
        constraintImposer = new ReflectionConstraintImposer(lookup);
        this.operationalTemplateProvider = provider;

        this.validationConfiguration = null; // Leave this null to indicate that no ValidationConfiguration was provided
        ValidationConfiguration dummyValidationConfiguration = new ValidationConfiguration.Builder()
                .failOnUnknownTerminologyId(com.nedap.archie.ValidationConfiguration.isFailOnUnknownTerminologyId())
                .build();
        ValidationHelper validationHelper = new ValidationHelper(this.lookup, dummyValidationConfiguration);
        rmOccurrenceValidator = new RmOccurrenceValidator();
        rmPrimitiveObjectValidator = new RmPrimitiveObjectValidator(validationHelper);
        rmTupleValidator = new RmTupleValidator(this.lookup, validationHelper, rmPrimitiveObjectValidator);
        rmMultiplicityValidator = new RmMultiplicityValidator();
    }

    /**
     * Creates an RM Object Validator with the given ModelInfoLook class, and the given OperationalTemplateProvider
     * The ModelInfoLookup is used for model access, and model specific constructions.
     * The OperationalTemplateProvider is used to retrieve other referenced archetypes in case of ArchetypeSlots.
     */
    public RMObjectValidator(ModelInfoLookup lookup, OperationalTemplateProvider provider, ValidationConfiguration validationConfiguration) {
        this.lookup = lookup;
        this.metaModel = new MetaModel(lookup, null);
        constraintImposer = new ReflectionConstraintImposer(lookup);
        this.operationalTemplateProvider = provider;

        this.validationConfiguration = validationConfiguration;
        ValidationHelper validationHelper = new ValidationHelper(this.lookup, validationConfiguration);
        validateInvariants = validationConfiguration.isValidateInvariants();
        rmOccurrenceValidator = new RmOccurrenceValidator();
        rmPrimitiveObjectValidator = new RmPrimitiveObjectValidator(validationHelper);
        rmTupleValidator = new RmTupleValidator(this.lookup, validationHelper, rmPrimitiveObjectValidator);
        rmMultiplicityValidator = new RmMultiplicityValidator();
    }

    public APathQueryCache getQueryCache() {
        return queryCache;
    }

    public void setQueryCache(APathQueryCache queryCache) {
        this.queryCache = queryCache;
    }

    /**
     * @deprecated Use {@link ValidationConfiguration.Builder#validateInvariants(boolean)} instead.
     */
    @Deprecated
    public void setRunInvariantChecks(boolean validateInvariants) {
        if(this.validationConfiguration != null) {
            throw new IllegalStateException("validateInvariants is already set via validationConfiguration, cannot set it again via setRunInvariantChecks");
        }
        this.validateInvariants = validateInvariants;
    }

    public List<RMObjectValidationMessage> validate(OperationalTemplate template, Object rmObject) {
        clearMessages();
        List<RMObjectWithPath> objects = Collections.singletonList(new RMObjectWithPath(rmObject, ""));
        addAllMessages(runArchetypeValidations(objects, ValidationPath.root(), template.getDefinition()));
        return getMessages();
    }

    public List<RMObjectValidationMessage> validate(Object rmObject) {
        clearMessages();
        List<RMObjectWithPath> objects = Collections.singletonList(new RMObjectWithPath(rmObject, "/"));
        addAllMessages(runArchetypeValidations(objects, ValidationPath.root(), null));
        return getMessages();
    }

    private static <T> void addAll(Collection<T> target, Collection<T> source) {
        //prevent potential initialization of target data structure if source is empty
        if (!source.isEmpty()) {
            target.addAll(source);
        }
    }

    private List<RMObjectValidationMessage> runArchetypeValidations(List<RMObjectWithPath> rmObjects, ValidationPath path, CObject cobject) {
        List<RMObjectValidationMessage> result = rmOccurrenceValidator.validate(metaModel, rmObjects, path, cobject);
        if (rmObjects.isEmpty()) {
            //if this branch of the archetype tree is null in the reference model, we're done validating
            //this has to be done after validateOccurrences(), or required fields do not get validated
            return result;
        }
        for (RMObjectWithPath objectWithPath : rmObjects) {
            validateInvariants(result, objectWithPath, path);
        }
        if(cobject == null) {
            //add default validations
            for (RMObjectWithPath objectWithPath : rmObjects) {
                validateUnconstrainedObjectWithPath(result, path, objectWithPath);
            }
        }
        else if (cobject instanceof CPrimitiveObject) {
            addAll(result, rmPrimitiveObjectValidator.validate(rmObjects, path, (CPrimitiveObject<?, ?>) cobject));
        } else if (cobject instanceof ArchetypeSlot) {
            validateArchetypeSlot(rmObjects, path, cobject, result);
        } else {
            if (cobject instanceof CComplexObject) {
                CComplexObject cComplexObject = (CComplexObject) cobject;
                for (CAttributeTuple tuple : cComplexObject.getAttributeTuples()) {
                    addAll(result, rmTupleValidator.validate(cobject, path, rmObjects, tuple));
                }
            }
            for (RMObjectWithPath objectWithPath : rmObjects) {
                validateConstrainedObjectWithPath(result, cobject, path, objectWithPath);
            }
        }
        return result;
    }

    private void validateInvariants(List<RMObjectValidationMessage> result, RMObjectWithPath objectWithPath, ValidationPath pathSoFar) {
        if (!validateInvariants) {
            return;
        }
        Object rmObject = objectWithPath.getObject();
        if (rmObject != null) {
            RMTypeInfo typeInfo = lookup.getTypeInfo(rmObject.getClass());
            if (typeInfo != null) {
                for (InvariantMethod invariantMethod : typeInfo.getInvariants()) {
                    if (!invariantMethod.getAnnotation().ignored()) {
                        try {
                            boolean passed = (boolean) invariantMethod.getMethod().invoke(rmObject);
                            if (!passed) {
                                result.add(new RMObjectValidationMessage(null, pathSoFar.addSubpath(objectWithPath).createPathString(),
                                        I18n.t("Invariant {0} failed on type " + typeInfo.getRmName(),invariantMethod.getAnnotation().value()),
                                        RMObjectValidationMessageType.INVARIANT_ERROR));
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            result.add(new RMObjectValidationMessage(null, pathSoFar.addSubpath(objectWithPath).createPathString(),
                                    I18n.t("Exception {0} invoking invariant {1} on {2}: {3}\n{4}",
                                            e.getCause() == null ? e.getClass().getSimpleName() : e.getCause().getClass().getSimpleName(),
                                            invariantMethod.getAnnotation().value(),
                                            typeInfo.getRmName(),
                                            e.getCause() == null ? e.getMessage() : e.getCause().getMessage(),
                                            Joiner.on("\n\t").join(e.getStackTrace())),
                                    RMObjectValidationMessageType.EXCEPTION));
                        }
                    }
                }
            }
        }
    }

    private void validateUnconstrainedObjectWithPath(
            List<RMObjectValidationMessage> result, ValidationPath path, RMObjectWithPath objectWithPath) {
        Object rmObject = objectWithPath.getObject();
        String archetypeId = lookup.getArchetypeIdFromArchetypedRmObject(rmObject);
        if (archetypeId != null) {
            validateArchetypedObject(result, null, path, objectWithPath, archetypeId);
        } else {
            validateObjectAttributes(result, null, path, objectWithPath);
        }
    }

    private void validateArchetypeSlot(List<RMObjectWithPath> rmObjects, ValidationPath path, CObject cobject, List<RMObjectValidationMessage> result) {
        ArchetypeSlot slot = (ArchetypeSlot) cobject;
        for (RMObjectWithPath objectWithPath : rmObjects) {

            Object object = objectWithPath.getObject();

            String archetypeId =  metaModel.getModelInfoLookup().getArchetypeIdFromArchetypedRmObject(object);

            if(archetypeId != null) {
                if(!AOMUtils.archetypeRefMatchesSlotExpression(archetypeId, slot)) {
                    //invalid archetype id, add message
                    this.addMessage(slot, objectWithPath.getPath(),
                            RMObjectValidationMessageIds.rm_ARCHETYPE_ID_SLOT_MISMATCH.getMessage(archetypeId),
                            RMObjectValidationMessageType.ARCHETYPE_SLOT_ID_MISMATCH);
                }
                //but do continue validation!
                validateArchetypedObject(result, cobject, path, objectWithPath, archetypeId);
            } else {
                this.addMessage(slot, objectWithPath.getPath(),
                        RMObjectValidationMessageIds.rm_SLOT_WITHOUT_ARCHETYPE_ID.getMessage(),
                        RMObjectValidationMessageType.ARCHETYPE_SLOT_ID_MISMATCH);
                //but continue validating the RM Objects, of course
                validateConstrainedObjectWithPath(result, cobject, path, objectWithPath);
            }
        }
    }

    private void validateArchetypedObject(List<RMObjectValidationMessage> result, CObject cobject, ValidationPath path, RMObjectWithPath objectWithPath, String archetypeId) {
        OperationalTemplate operationalTemplate = operationalTemplateProvider.getOperationalTemplate(archetypeId);
        if (operationalTemplate != null) {
            //occurrences already validated, so nothing left to validate from the archetyepe root
            //from now on, validate from the root of the found OPT
            CObject newRoot = operationalTemplate.getDefinition();
            validateConstrainedObjectWithPath(result, newRoot, path, objectWithPath);
        } else {
            this.addMessage(cobject, objectWithPath.getPath(),
                    RMObjectValidationMessageIds.rm_ARCHETYPE_NOT_FOUND.getMessage(archetypeId),
                    RMObjectValidationMessageType.ARCHETYPE_NOT_FOUND);
            //but continue validating the RM Objects, of course
            if (cobject != null) {
                validateConstrainedObjectWithPath(result, cobject, path, objectWithPath);
            } else {
                validateObjectAttributes(result, null, path, objectWithPath);
            }
        }
    }

    private void validateConstrainedObjectWithPath(List<RMObjectValidationMessage> result, CObject cobject, ValidationPath path, RMObjectWithPath objectWithPath) {
        Class<?> classInConstraint = this.lookup.getClass(cobject.getRmTypeName());
        if (!classInConstraint.isAssignableFrom(objectWithPath.getObject().getClass())) {
            //not a matching constraint. Cannot validate. add error message and stop validating.
            //If another constraint is present, that one will succeed
            result.add(new RMObjectValidationMessage(
                    cobject,
                    objectWithPath.getPath(),
                    RMObjectValidationMessageIds.rm_INCORRECT_TYPE.getMessage(cobject.getRmTypeName(), objectWithPath.getObject().getClass().getSimpleName()),
                    RMObjectValidationMessageType.WRONG_TYPE)
            );
        } else {
            validateObjectAttributes(result, cobject, path, objectWithPath);
        }
    }

    private void validateObjectAttributes(List<RMObjectValidationMessage> result, CObject cobject, ValidationPath path, RMObjectWithPath objectWithPath) {
        Object rmObject = objectWithPath.getObject();
        Iterator<CAttribute> attributeIterator;
        if (cobject == null) {
            RMTypeInfo typeInfo = lookup.getTypeInfo(rmObject.getClass());
            if (typeInfo != null) {
                attributeIterator = RMObjectValidationUtil.getDefaultAttributeConstraints(
                        typeInfo.getRmName(), Collections.emptyList(), lookup, constraintImposer).iterator();
            } else {
                return; // Type unknown, nothing to validate
            }
        } else {
            List<CAttribute> cobjectAttributes = cobject.getAttributes();
            List<CAttribute> defaultAttributeConstraints = RMObjectValidationUtil.getDefaultAttributeConstraints(
                    cobject, cobjectAttributes, lookup, constraintImposer);
            if (defaultAttributeConstraints.isEmpty()) {
                attributeIterator = cobjectAttributes.iterator();
            } else if (cobjectAttributes.isEmpty()) {
                attributeIterator = defaultAttributeConstraints.iterator();
            } else {
                attributeIterator = Stream.of(cobjectAttributes, defaultAttributeConstraints)
                        .flatMap(List::stream)
                        .iterator();
            }
        }
        if (attributeIterator.hasNext()) {
            validateCAttributes(result, path, objectWithPath, rmObject, cobject, attributeIterator);
        }
    }

    private void validateCAttributes(List<RMObjectValidationMessage> result, ValidationPath path, RMObjectWithPath objectWithPath, Object rmObject, CObject cObject, Iterator<CAttribute> attributeIterator) {
        //the path contains an attribute, but is missing the [idx] part. So strip the attribute, and add the attribute plus the [idx] part.
        ValidationPath pathSoFar = path.addSubpath(objectWithPath);
        attributeIterator.forEachRemaining(attribute ->
                validateAttributes(result, attribute, cObject, rmObject, pathSoFar));
    }

    private void validateAttributes(List<RMObjectValidationMessage> result, CAttribute attribute, CObject cobject, Object rmObject, ValidationPath pathSoFar) {
        String rmAttributeName = attribute.getRmAttributeName();
        String prefixedAttributeName = "/" + attribute.getRmAttributeName();
        RMPathQuery aPathQuery = queryCache.getApathQuery(prefixedAttributeName);
        Object attributeValue = aPathQuery.find(lookup, rmObject);
        boolean observationValid = isObservationEmpty(result, attribute, rmAttributeName, attributeValue, pathSoFar, cobject);

        if (observationValid) {

            addAll(result, rmMultiplicityValidator.validate(attribute, pathSoFar.addSubpath(rmAttributeName), attributeValue));

            if(attribute.getChildren() == null || attribute.getChildren().isEmpty()) {
                //no child CObjects. Cardinality/existence has already been validated. Run default RM validations
                List<RMObjectWithPath> childRmObjects = aPathQuery.findList(lookup, rmObject);
                addAll(result, runArchetypeValidations(childRmObjects, pathSoFar.addSubpath(prefixedAttributeName), null));
            }
            else if (attribute.isSingle()) {
                validateSingleAttribute(result, attribute, rmObject, pathSoFar);
            } else {

                for (CObject childCObject : attribute.getChildren()) {
                    String query = prefixedAttributeName + '[' + childCObject.getNodeId() + ']';
                    aPathQuery = queryCache.getApathQuery(query);
                    List<RMObjectWithPath> childRmObjects = aPathQuery.findList(lookup, rmObject);
                    addAll(result, runArchetypeValidations(childRmObjects, pathSoFar.addSubpath(query), childCObject));
                    //TODO: find all other child RM Objects that don't match with a given node id (eg unconstraint in archetype) and
                    //run default validations against them!
                }
            }
        }
    }

    private void validateSingleAttribute(List<RMObjectValidationMessage> result, CAttribute attribute, Object rmObject, ValidationPath pathSoFar) {
        List<List<RMObjectValidationMessage>> subResults = new ArrayList<>();

        for (CObject childCObject : attribute.getChildren()) {
            String query = "/" + attribute.getRmAttributeName() + "[" + childCObject.getNodeId() + "]";
            RMPathQuery aPathQuery = queryCache.getApathQuery(query);
            List<RMObjectWithPath> childNodes = aPathQuery.findList(lookup, rmObject);
            List<RMObjectValidationMessage> subResult = runArchetypeValidations(childNodes, pathSoFar.addSubpath(query), childCObject);
            boolean cObjectWithoutErrorsFound = subResult.isEmpty();
            if (cObjectWithoutErrorsFound) {
                //a single attribute with multiple CObjects means you can choose which CObject you use
                //for example, a data value can be a string or an integer.
                //in this case, only one of the CObjects will validate to a correct value
                //so as soon as one is correct, so is the data!
                return;
            }
            subResults.add(subResult);
        }

        boolean atLeastOneWithoutWrongTypeFound = subResults.stream().anyMatch(RMObjectValidationUtil::hasNoneWithWrongType);

        if (atLeastOneWithoutWrongTypeFound) {
            for (List<RMObjectValidationMessage> subResult : subResults) {
                //at least one has the correct type, we can filter out all others
                subResult.stream().filter((message) -> message.getType() != RMObjectValidationMessageType.WRONG_TYPE).forEach(result::add);
            }
        } else {
            subResults.forEach(result::addAll);
        }
    }

    /**
     * Check if an observation is empty. This is the case if its event contains an empty data attribute.
     *
     * @param result          List to add validation messages to
     * @param attribute       The attribute that is checked
     * @param rmAttributeName The name of the attribute
     * @param attributeValue  The value of the attribute
     * @param pathSoFar       The path of the attribute
     * @param cobject         The constraints that the attribute is checked against
     */
    private boolean isObservationEmpty(List<RMObjectValidationMessage> result, CAttribute attribute, String rmAttributeName, Object attributeValue, ValidationPath pathSoFar, CObject cobject) {
        CObject parent = attribute.getParent();

        if (
            //attribute is empty
            attributeValue == null
            && parent != null
            // attribute is data
            && rmAttributeName.equals("data")
            //parent is event
            && parent.getRmTypeName().contains("EVENT")
            //attribute should not be empty
            && attribute.getExistence() != null && !attribute.getExistence().has(0))
        {
            String message = "Observation " + RMObjectValidationUtil.getParentObservationTerm(attribute) + " contains no results";
            result.add(new RMObjectValidationMessage(cobject == null ? null : cobject.getParent().getParent(), pathSoFar.createPathString(), message, RMObjectValidationMessageType.EMPTY_OBSERVATION));
            return false;
        } else {
            return true;
        }
    }
}
