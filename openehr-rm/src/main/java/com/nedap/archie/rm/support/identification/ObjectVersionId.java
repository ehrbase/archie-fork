package com.nedap.archie.rm.support.identification;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pieter.bos on 08/07/16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OBJECT_VERSION_ID")
public class ObjectVersionId extends UIDBasedId {

    public ObjectVersionId() {
    }

    public ObjectVersionId(String value) {
        super(value);
    }
}
