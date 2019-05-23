package com.nedap.archie.rm.archetyped;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.datavalues.DvEHRURI;
import com.nedap.archie.rm.datavalues.DvText;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by pieter.bos on 04/11/15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LINK")
public class Link extends RMObject {

    private DvText meaning;
    private DvText type;
    private DvEHRURI target;

    public Link() {
    }

    public Link(DvText meaning, DvText type, DvEHRURI target) {
        this.meaning = meaning;
        this.type = type;
        this.target = target;
    }

    public DvText getMeaning() {
        return meaning;
    }

    public void setMeaning(DvText meaning) {
        this.meaning = meaning;
    }

    public DvText getType() {
        return type;
    }

    public void setType(DvText type) {
        this.type = type;
    }

    public DvEHRURI getTarget() {
        return target;
    }

    public void setTarget(DvEHRURI target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        if (meaning != null ? !meaning.equals(link.meaning) : link.meaning != null) return false;
        if (type != null ? !type.equals(link.type) : link.type != null) return false;
        return target != null ? target.equals(link.target) : link.target == null;

    }

    @Override
    public int hashCode() {
        int result = meaning != null ? meaning.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }
}
