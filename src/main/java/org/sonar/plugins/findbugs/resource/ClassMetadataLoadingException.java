package org.sonar.plugins.findbugs.resource;

public class ClassMetadataLoadingException extends RuntimeException {

    public ClassMetadataLoadingException(Throwable cause) {
        super("ASM failed to load classfile metadata", cause);
    }
}
