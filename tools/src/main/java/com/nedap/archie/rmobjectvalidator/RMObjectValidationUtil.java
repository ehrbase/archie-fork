package com.nedap.archie.rmobjectvalidator;

import com.nedap.archie.adlparser.modelconstraints.ModelConstraintImposer;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.aom.CObject;
import com.nedap.archie.aom.terminology.ArchetypeTerm;
import com.nedap.archie.rminfo.ModelInfoLookup;
import com.nedap.archie.rminfo.RMAttributeInfo;
import com.nedap.archie.rminfo.RMTypeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RMObjectValidationUtil {

    /**
     * Retrieve the terminology name of the observation that an attribute is a part of.
     *
     * @param attribute The attribute for which the observation is retrieved
     * @return The name of the observation
     */
    public static String getParentObservationTerm(CAttribute attribute) {
        String result = "";
        CObject parent = attribute.getParent();
        CAttribute attributeParent = parent.getParent();
        while (result.equals("") && parent != null && attributeParent != null) {
            parent = attributeParent.getParent();
            if (parent != null && parent.getRmTypeName().equals("OBSERVATION")) {
                ArchetypeTerm parentTerm = parent.getTerm();
                if (parentTerm != null) {
                    result = parentTerm.getText();
                }
            }
            attributeParent = parent == null ? null : parent.getParent();
        }
        return result;
    }

    public static boolean hasNoneWithWrongType(List<RMObjectValidationMessage> subResult) {
        return subResult.stream().noneMatch((message) -> message.getType() == RMObjectValidationMessageType.WRONG_TYPE);
    }

    public static List<CAttribute> getDefaultAttributeConstraints(CObject cObject, List<CAttribute> attributes, ModelInfoLookup lookup, ModelConstraintImposer constraintImposer) {
        RMTypeInfo typeInfo = lookup.getTypeInfo(cObject.getRmTypeName());
        if (typeInfo == null) {
            return Collections.emptyList();
        }

        List<CAttribute> result = new ArrayList<>();

        Set<String> alreadyConstrainedAttributes = attributes.isEmpty()
                ? Collections.emptySet()
                : attributes.stream().map(CAttribute::getRmAttributeName).collect(Collectors.toSet());

        for (RMAttributeInfo defaultAttribute : typeInfo.getAttributes().values()) {
            if (!defaultAttribute.isComputed()) {
                if (!alreadyConstrainedAttributes.contains(defaultAttribute.getRmName())) {
                    CAttribute attribute = constraintImposer.getDefaultAttribute(cObject.getRmTypeName(), defaultAttribute.getRmName());
                    attribute.setParent(cObject);
                    result.add(attribute);
                }
            }
        }
        return result;

    }

    public static List<CAttribute> getDefaultAttributeConstraints(String rmTypeName, List<CAttribute> attributes, ModelInfoLookup lookup, ModelConstraintImposer constraintImposer) {
        CComplexObject fakeParent = new CComplexObject();
        fakeParent.setRmTypeName(rmTypeName);
        return getDefaultAttributeConstraints(fakeParent, attributes, lookup, constraintImposer);
    }
}
