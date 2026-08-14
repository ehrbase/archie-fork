package com.nedap.archie.rmobjectvalidator;

import com.nedap.archie.aom.CPrimitiveObject;

/**
 * Tracks the path during validation, so string concatenation is only performed for failed validations
 */
abstract class ValidationPath {

    static ValidationPath ROOT = new ValidationPath(){
        @Override
        public String toString() {
            return "";
        }
    };
    static ValidationPath of(String path) {
        return new SimpleLazyPath(null, path, null);
    }

    private ValidationPath() {
        //NOOP
    }

    @Override
    public abstract String toString();

    ValidationPath add(String rmAttributeName) {
        return new SimpleLazyPath(this, rmAttributeName, null);
    }

    ValidationPath add(String attributeName, CPrimitiveObject<?, ?> cPrimitiveObject) {
        return new SimpleLazyPath(this, attributeName, cPrimitiveObject.getNodeId());
    }

    ValidationPath joinPathsWithSeparator(String other) {
        return new PathJoin(this, true, other);
    }

    ValidationPath joinPaths(String other) {
        return new PathJoin(this, false, other);
    }

    ValidationPath stripLastPathSegment() {
        return new StripLastPathSegment(this);
    }

    static final class SimpleLazyPath extends ValidationPath {

            private final ValidationPath parent;
            private final String path;
            private final String nodeId;

            SimpleLazyPath(ValidationPath parent, String path, String nodeId) {
                this.parent = parent;
                this.path = path;
                this.nodeId = nodeId;
            }

            @Override
            public String toString() {
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
        }

        static final class PathJoin extends ValidationPath {
            private final ValidationPath parent;
            private final String path;
            private final boolean enforceSlash;

            PathJoin(ValidationPath parent, boolean enforceSlash, String path) {
                this.parent = parent;
                this.enforceSlash = enforceSlash;
                this.path = path;
            }

            @Override
            public String toString() {
                if (enforceSlash) {
                    return joinPaths(parent.toString(), "/", path);
                } else {
                    return joinPaths(parent.toString(), path);
                }
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

        static final class StripLastPathSegment extends ValidationPath {
            private final ValidationPath child;

            StripLastPathSegment(ValidationPath child) {
                this.child = child;
            }

            @Override
            public String toString() {
                return RMObjectValidationUtil.stripLastPathSegment(child.toString());
            }
        }
    }