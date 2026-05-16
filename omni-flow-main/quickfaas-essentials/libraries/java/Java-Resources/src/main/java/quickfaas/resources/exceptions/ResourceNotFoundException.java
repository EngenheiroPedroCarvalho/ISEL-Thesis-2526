/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources.exceptions;

public final class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName) {
        super("Resource '" + resourceName + "' was not found.");
    }

    public ResourceNotFoundException(String resourceName, String extraInfo) {
        super("Resource '" + resourceName + "' was not found. " + extraInfo);
    }
}
