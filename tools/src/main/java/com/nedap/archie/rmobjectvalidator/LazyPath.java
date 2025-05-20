package com.nedap.archie.rmobjectvalidator;

import com.nedap.archie.aom.CPrimitiveObject;
import com.nedap.archie.query.RMObjectWithPath;

/**
 * Postpones the creation of path strings.
 * Only for failed validations the strings need to be constructed.
 */
public final class LazyPath {

    private static final LazyPath ROOT = new LazyPath(null, false, null, null);

    private final LazyPath parent;
    private final boolean fromParent;
    private final String path;
    private final String nodeId;

    private LazyPath(LazyPath parent, boolean fromParent, String path, String nodeId) {
        this.parent = parent;
        this.fromParent = fromParent;
        this.path = path;
        this.nodeId = nodeId;
    }

    public static LazyPath root() {
        return ROOT;
    }

    static LazyPath of(String path) {
        return ROOT.addSubpath(path);
    }

    public LazyPath addChild(String attributeName, CPrimitiveObject<?, ?> cPrimitiveObject) {
        return new LazyPath(this, false, attributeName, cPrimitiveObject.getNodeId());
    }

    public LazyPath addSubpath(String subpath) {
        return new LazyPath(this, false, subpath, null);
    }

    /**
     * pathSoFar ends with an attribute, but objectWithPath contains it, so remove that.
     */
    public LazyPath addSubpath(RMObjectWithPath subpath) {
        return  new LazyPath(this, true, subpath.getPath(), null);
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