package org.openehr.bmm.v2.persistence;

public final class PBmmSinglePropertyOpen extends PBmmProperty<PBmmOpenType> {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}