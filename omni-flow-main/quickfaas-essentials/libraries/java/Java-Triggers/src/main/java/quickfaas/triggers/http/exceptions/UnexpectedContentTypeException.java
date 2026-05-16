/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.triggers.http.exceptions;

public final class UnexpectedContentTypeException extends RuntimeException {
    public UnexpectedContentTypeException(String expected, String found) {
        super("Expected content-type '" + expected + "', found '" + found + "'.");
    }
}
