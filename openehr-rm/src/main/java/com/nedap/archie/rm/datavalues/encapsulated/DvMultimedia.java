package com.nedap.archie.rm.datavalues.encapsulated;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvURI;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by pieter.bos on 04/11/15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DV_MULTIMEDIA", propOrder = {
        "alternateText",
        "uri",
        "data",
        "mediaType",
        "compressionAlgorithm",
        "integrityCheck",
        "integrityCheckAlgorithm",
        "size",
        "thumbnail"
})
public class DvMultimedia extends DvEncapsulated {
    @Nullable
    @XmlElement(name = "alternate_text")
    private String alternateText;
    @Nullable
    private DvURI uri;
    @Nullable
    private byte[] data;
    @XmlElement(name = "media_type", required = true)
    private CodePhrase mediaType;//TODO: construct default codephrase with mime type already set as terminology id
    @Nullable
    @XmlElement(name = "compression_algorithm")
    private CodePhrase compressionAlgorithm;
    @Nullable
    @XmlElement(name = "integrity_check")
    private byte[] integrityCheck;

    @Nullable
    @XmlElement(name = "integrity_check_algorithm")
    private CodePhrase integrityCheckAlgorithm;

    private Integer size;

    @Nullable
    private DvMultimedia thumbnail;


    public String getAlternateText() {
        return alternateText;
    }

    public void setAlternateText(@Nullable String alternateText) {
        this.alternateText = alternateText;
    }

    @Nullable
    public DvURI getUri() {
        return uri;
    }

    public void setUri(@Nullable DvURI uri) {
        this.uri = uri;
    }

    @Nullable
    public byte[] getData() {
        return data;
    }

    public void setData(@Nullable byte[] data) {
        this.data = data;
    }

    public CodePhrase getMediaType() {
        return mediaType;
    }

    public void setMediaType(CodePhrase mediaType) {
        this.mediaType = mediaType;
    }

    @Nullable
    public CodePhrase getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public void setCompressionAlgorithm(@Nullable CodePhrase compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }

    @Nullable
    public byte[] getIntegrityCheck() {
        return integrityCheck;
    }

    public void setIntegrityCheck(@Nullable byte[] integrityCheck) {
        this.integrityCheck = integrityCheck;
    }


    @Nullable
    public DvMultimedia getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(@Nullable DvMultimedia thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Nullable
    public CodePhrase getIntegrityCheckAlgorithm() {
        return integrityCheckAlgorithm;
    }

    public void setIntegrityCheckAlgorithm(@Nullable CodePhrase integrityCheckAlgorithm) {
        this.integrityCheckAlgorithm = integrityCheckAlgorithm;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DvMultimedia that = (DvMultimedia) o;
        return Objects.equals(alternateText, that.alternateText) &&
                Objects.equals(uri, that.uri) &&
                Arrays.equals(data, that.data) &&
                Objects.equals(mediaType, that.mediaType) &&
                Objects.equals(compressionAlgorithm, that.compressionAlgorithm) &&
                Arrays.equals(integrityCheck, that.integrityCheck) &&
                Objects.equals(integrityCheckAlgorithm, that.integrityCheckAlgorithm) &&
                Objects.equals(size, that.size) &&
                Objects.equals(thumbnail, that.thumbnail);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), alternateText, uri, mediaType, compressionAlgorithm, integrityCheckAlgorithm, size, thumbnail);
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + Arrays.hashCode(integrityCheck);
        return result;
    }
}
