package com.nedap.archie.rmobjectvalidator;

import com.nedap.archie.aom.CPrimitiveObject;
import com.nedap.archie.query.RMObjectWithPath;

/**
 * Postpones the creation of path strings.
 * Only for failed validations the strings need to be constructed.
 */
public final class ValidationPath {

    private static final ValidationPath ROOT = new ValidationPath(null, false, null, null);

    private final ValidationPath parent;
    private final boolean fromParent;
    private final String path;
    private final String nodeId;

    private ValidationPath(ValidationPath parent, boolean fromParent, String path, String nodeId) {
        this.parent = parent;
        this.fromParent = fromParent;
        this.path = path;
        this.nodeId = nodeId;
    }

    public static ValidationPath root() {
        return ROOT;
    }

    static ValidationPath of(String path) {
        return ROOT.addSubpath(path);
    }

    public ValidationPath addChild(String attributeName, CPrimitiveObject<?, ?> cPrimitiveObject) {
        return new ValidationPath(this, false, attributeName, cPrimitiveObject.getNodeId());
    }

    public ValidationPath addSubpath(String subpath) {
        return new ValidationPath(this, false, subpath, null);
    }

    /**
     * pathSoFar ends with an attribute, but objectWithPath contains it, so remove that.
     */
    public ValidationPath addSubpath(RMObjectWithPath subpath) {
        return  new ValidationPath(this, true, subpath.getPath(), null);
    }


    public String createPathString() {
        if (parent == null) {
            return "/";
        }

        String parentPath;
        if (fromParent) {
            parentPath = stripLastPathSegment(parent.createPathString());
        } else{
            parentPath = parent.createPathString();
        }

        String p;
        if (path.startsWith("/")) {
            p = joinPaths(parentPath, path);
        } else {
            p = joinPaths(parentPath, "/", path);
        }

        if (nodeId == null) {
            return p;
        } else {
            return p + '[' + nodeId + ']';
        }
    }

    public String toString() {
        return createPathString();
    }

    private static String joinPaths(String... pathElements) {
        if(pathElements.length == 0) {
            return "/";
        }
        if(pathElements.length == 1) {
            String path =  pathElements[0];
            if(path.isEmpty()) {
                return "/";
            }
            return path;
        }
        StringBuilder result = new StringBuilder();
        boolean lastCharacterWasSlash = false;
        for(String pathElement:pathElements) {
            if(lastCharacterWasSlash && pathElement.startsWith("/")) {
                result.append(pathElement, 1, pathElement.length());
            } else {
                result.append(pathElement);
            }
            if(!pathElement.isEmpty()) {
                lastCharacterWasSlash = pathElement.charAt(pathElement.length() - 1) == '/';
            }
        }
        return result.toString();
    }

    private static String stripLastPathSegment(String path) {
        if (path.equals("/")) {
            return "";
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return path;
        }
        return path.substring(0, lastSlashIndex);
    }
}