package com.nedap.archie.rmobjectvalidator;

import com.nedap.archie.aom.CPrimitiveObject;

/**
 * Postpones the creation of path strings.
 * Only for failed validations the strings need to be constructed.
 */
public interface LazyPath {

    static LazyPath of(String path) {
        return new SimpleLazyPath(null, path, null);
    }

    default LazyPath add(String rmAttributeName) {
        return new SimpleLazyPath(this, rmAttributeName, null);
    }

    default LazyPath add(String attributeName, CPrimitiveObject<?, ?> cPrimitiveObject) {
        return new SimpleLazyPath(this, attributeName, cPrimitiveObject.getNodeId());
    }

    default LazyPath joinPaths(String other, boolean enforceSlash) {
        return new LazyPathJoin(this, enforceSlash, other);
    }

    default LazyPath stripLastPathSegment() {
        return new LazyStripLastPathSegment(this);
    }

    String createPathString();

    final class SimpleLazyPath implements LazyPath {
        private final LazyPath parent;
        private final String path;
        private final String nodeId;

        public SimpleLazyPath(LazyPath parent, String path, String nodeId) {
            this.parent = parent;
            this.path = path;
            this.nodeId = nodeId;
        }

        @Override
        public String createPathString() {
            String p = path;
            if (nodeId != null) {
                p = p + '[' + nodeId + ']';
            }
            if (parent == null) {
                return p;
            } else {
                return parent + "/" + p;
            }
        }

        public String toString() {
            return createPathString();
        }
    }

    final class LazyPathJoin implements LazyPath {
        private final LazyPath parent;
        private final String path;
        private final boolean enforceSlash;

        public LazyPathJoin(LazyPath parent, boolean enforceSlash, String path) {
            this.parent = parent;
            this.enforceSlash = enforceSlash;
            this.path = path;
        }

        @Override
        public String createPathString() {
            if (enforceSlash) {
                return joinPaths(parent.createPathString(), "/", path);
            } else {
                return joinPaths(parent.createPathString(), path);
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
                    result.append(pathElement.substring(1));
                } else {
                    result.append(pathElement);
                }
                if(!pathElement.isEmpty()) {
                    lastCharacterWasSlash = pathElement.charAt(pathElement.length() - 1) == '/';
                }
            }
            return result.toString();
        }
    }

    // RMObjectValidationUtil.stripLastPathSegment(pathSoFar)
    final class LazyStripLastPathSegment implements LazyPath {
        private final LazyPath child;

        public LazyStripLastPathSegment(LazyPath child) {
            this.child = child;
        }

        @Override
        public String createPathString() {
            return RMObjectValidationUtil.stripLastPathSegment(child.createPathString());
        }

        public String toString() {
            return createPathString();
        }
    }
}