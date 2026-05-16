/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.triggers.http.exceptions;

import java.util.NoSuchElementException;

public final class NoSuchHeaderException extends NoSuchElementException {
    public NoSuchHeaderException(String header){
        super("Header '" + header + "' was not provided in HTTP request.");
    }
}
