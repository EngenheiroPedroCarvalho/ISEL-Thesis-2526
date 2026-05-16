/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.triggers.http.exceptions;

import java.util.NoSuchElementException;

public final class NoSuchPropertyException extends NoSuchElementException {
    public NoSuchPropertyException(String property){
        super("Property '" + property + "' was not found in HTTP request.");
    }
}
