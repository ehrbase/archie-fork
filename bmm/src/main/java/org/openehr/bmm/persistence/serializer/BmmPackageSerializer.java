package org.openehr.bmm.persistence.serializer;

/*
 * #%L
 * OpenEHR - Java Model Stack
 * %%
 * Copyright (C) 2016 - 2017  Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 * Author: Claude Nanjo
 */

import org.openehr.bmm.BmmConstants;
import org.openehr.bmm.core.BmmClass;
import org.openehr.bmm.persistence.PersistedBmmModelElement;
import org.openehr.bmm.persistence.PersistedBmmPackage;
import org.openehr.odin.utils.OdinSerializationUtils;

import java.util.List;

/**
 *
 *  Created by cnanjo on 1/24/17.
 */
@Deprecated
public class BmmPackageSerializer extends BaseBmmSerializer<PersistedBmmPackage> {

    public BmmPackageSerializer() {
        super();
    }

    public BmmPackageSerializer(PersistedBmmPackage persistedBmmPackage) {
        super(persistedBmmPackage);
    }

    @Override
    public String serialize(int indentationCount) {
        StringBuilder builder = new StringBuilder();
        serialize(indentationCount, builder);
        return builder.toString();
    }

    @Override
    public void serialize(int indentationCount, StringBuilder builder) {
        int indentCount = indentationCount;
        indentByAndAppend(builder, indentCount, OdinSerializationUtils.buildKeyedObjectOpeningDeclaration(getPersistedModel().getName())); //Open package section
        ++indentCount;
        if(getPersistedModel().getDocumentation() != null) {
            indentByAndAppend(builder, indentCount, OdinSerializationUtils.buildOdinStringObjectPropertyInitialization(BmmConstants.BMM_DOCUMENTATION, getPersistedModel().getDocumentation()));
        }
        indentByAndAppend(builder, indentCount, OdinSerializationUtils.buildOdinStringObjectPropertyInitialization(BmmConstants.BMM_PROPERTY_NAME, getPersistedModel().getName()));
        if(getPersistedModel().getClasses() != null && getPersistedModel().getClasses().size() > 0) {
            indentByAndAppend(builder, indentCount, OdinSerializationUtils.buildOdinStringListPropertyInitialization(BmmConstants.BMM_PROPERTY_CLASSES, getPersistedModel().getClasses()));
        }
        if(getPersistedModel().getPackages() != null && getPersistedModel().getPackages().size() > 0) {
            BmmPackageContainerSerializer serializer = new BmmPackageContainerSerializer(getPersistedModel());
            serializer.serialize(indentCount, builder);
        }
        --indentCount;
        indentByAndAppend(builder, indentCount, OdinSerializationUtils.closeOdinObject());//Close package section
    }
}
