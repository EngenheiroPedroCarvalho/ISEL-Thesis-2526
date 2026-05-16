/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.resources.exceptions;

public final class InvalidAccessKeyException extends RuntimeException {
    public InvalidAccessKeyException() {
        super("The provided access key is invalid.");
    }
}
