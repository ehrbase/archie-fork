package com.nedap.archie.rminfo;

import com.google.common.base.Joiner;
import com.nedap.archie.ArchieLanguageConfiguration;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CAttributeTuple;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.aom.CPrimitiveTuple;
import com.nedap.archie.aom.OperationalTemplate;
import com.nedap.archie.aom.primitives.CTerminologyCode;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.archie.aom.terminology.ArchetypeTerminology;
import com.nedap.archie.base.Interval;
import com.nedap.archie.paths.PathSegment;
import com.nedap.archie.query.APathQuery;
import com.nedap.archie.query.RMObjectWithPath;
import com.nedap.archie.query.RMPathQuery;
import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

public class UpdatedValueHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpdatedValueHandler.class);

    public static void pathHasBeenUpdated(Object rmObject, Archetype archetype, String pathOfParent, Object parent) {
        if(parent instanceof CodePhrase) {
            fixCodePhrase(rmObject, archetype, pathOfParent);
        }
    }

    private static void fixCodePhrase(Object rmObject, Archetype archetype, String pathOfParent) {
        try {
            //special case: if at-code has been set, we need to do more!
            if (pathOfParent.endsWith("value/defining_code")) {
                fixDvCodedText(rmObject, archetype, pathOfParent);
            } else if (pathOfParent.endsWith("symbol/defining_code")) {
                fixDvOrdinal(rmObject, archetype, pathOfParent);
            } else {
            }
        } catch (Exception e) {
            logger.warn("cannot fix codephrase", e);
        }
    }

    private static void fixDvOrdinal(Object rmObject, Archetype archetype, String pathOfParent) throws XPathExpressionException {
        RMPathQuery rmPathQuery = new RMPathQuery(pathOfParent.replace("/symbol/defining_code", ""));
        DvOrdinal ordinal = rmPathQuery.find(ArchieRMInfoLookup.getInstance(), rmObject);
        CAttribute symbolAttribute = archetype.itemAtPath(pathOfParent.replace("/symbol/defining_code", "/symbol"));//TODO: remove all numeric indices from path!
        if (symbolAttribute != null) {
            CAttributeTuple socParent = (CAttributeTuple) symbolAttribute.getSocParent();
            if (socParent != null) {
                int valueIndex = socParent.getMemberIndex("value");
                int symbolIndex = socParent.getMemberIndex("symbol");
                if (valueIndex != -1 && symbolIndex != -1) {
                    for (CPrimitiveTuple tuple : socParent.getTuples()) {
                        if (tuple.getMembers().get(symbolIndex).getConstraint().get(0).equals(ordinal.getSymbol().getDefiningCode().getCodeString())) {
                            List<Interval> valueConstraint = tuple.getMembers().get(valueIndex).getConstraint();
                            if(valueConstraint.size() == 1) {
                                Interval<Long> interval  = valueConstraint.get(0);
                                if(interval.getLower().equals(interval.getUpper()) && !interval.isLowerUnbounded() && !interval.isUpperUnbounded()) {
                                    ordinal.setValue(interval.getLower());
                                }

                            }
                        }
                    }

                }
            }
        }
    }

    private static void fixDvCodedText(Object rmObject, Archetype archetype, String pathOfParent) throws XPathExpressionException {
        RMPathQuery rmPathQuery = new RMPathQuery(pathOfParent.replace("/defining_code", ""));
        DvCodedText codedText = rmPathQuery.find(ArchieRMInfoLookup.getInstance(), rmObject);
        Archetyped details = findLastArchetypeDetails(rmObject, pathOfParent);
        if(details == null) {
            setDefaultTermDefinitionInCodedText(archetype, codedText);
        } else if(archetype instanceof OperationalTemplate) {

            OperationalTemplate template = (OperationalTemplate) archetype;

            String archetypePath = convertRMObjectPathToArchetypePath(pathOfParent);
            setTerminologyFromArchetype(archetype, codedText, archetypePath);

            setValueFromTermDefinition(codedText, details, template);
        } else {
            setDefaultTermDefinitionInCodedText(archetype, codedText);
        }
    }

    private static ArchetypeTerm getTermDefinition(OperationalTemplate template, Archetyped details, DvCodedText codedText) {
        ArchetypeTerminology archetypeTerminology = template.getComponentTerminologies().get(details.getArchetypeId().toString());
        if(archetypeTerminology != null) {
            ArchetypeTerm termDefinition = archetypeTerminology.getTermDefinition(ArchieLanguageConfiguration.getMeaningAndDescriptionLanguage(), codedText.getDefiningCode().getCodeString());
            if(termDefinition != null) {
                return termDefinition;
            }
        }
        return template.getTerminology().getTermDefinition(ArchieLanguageConfiguration.getMeaningAndDescriptionLanguage(), codedText.getDefiningCode().getCodeString());
    }

    private static void setValueFromTermDefinition(DvCodedText codedText, Archetyped details, OperationalTemplate template) {
        ArchetypeTerm termDefinition = getTermDefinition(template, details, codedText);
        if(termDefinition != null) {
            codedText.setValue(termDefinition.getText());
        }
    }

    private static void setDefaultTermDefinitionInCodedText(Archetype archetype, DvCodedText codedText) {
        ArchetypeTerm termDefinition = archetype.getTerminology().getTermDefinition(ArchieLanguageConfiguration.getMeaningAndDescriptionLanguage(), codedText.getDefiningCode().getCodeString());
        if(termDefinition != null) {
            codedText.setValue(termDefinition.getText());
        }
    }

    private static void setTerminologyFromArchetype(Archetype archetype, DvCodedText codedText, String s) {
        ArchetypeModelObject archetypeModelObject = archetype.itemAtPath(s);
        if(archetypeModelObject instanceof CAttribute) {
            CAttribute definingCodeConstraint = (CAttribute) archetypeModelObject;
            for(CObject child:definingCodeConstraint.getChildren()) {
                if(child instanceof CTerminologyCode) {
                    if(((CTerminologyCode) child).getConstraint().get(0).startsWith("ac")) {
                        codedText.getDefiningCode().setTerminologyId(new TerminologyId(((CTerminologyCode) child).getConstraint().get(0)));
                    }
                }
            }
        }
    }

    public static Archetyped findLastArchetypeDetails(Object rmObject, String path) throws XPathExpressionException {
        APathQuery query = new APathQuery(path);
        for(int i = query.getPathSegments().size();i > 0; i--) {
            String subpath = Joiner.on("").join(query.getPathSegments().subList(0, i));

            List<RMObjectWithPath> list = new RMPathQuery(subpath).findList(ArchieRMInfoLookup.getInstance(), rmObject);
            for(RMObjectWithPath objectWithPath:list) {
                Object object = objectWithPath.getObject();
                if(object instanceof Locatable) {
                    Locatable object1 = (Locatable) object;
                    if(object1.getArchetypeDetails() != null) {
                        return object1.getArchetypeDetails();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Convert an RM path query into an AOM path query. Not a complete implementation though. this could actually be useful in more places.
     * @param path
     * @return
     */
    public static String convertRMObjectPathToArchetypePath(String path) {
        APathQuery query = new APathQuery(path);
        for(PathSegment segment:query.getPathSegments()) {
            segment.setIndex(null);
        }
        return Joiner.on("").join(query.getPathSegments());
    }
}