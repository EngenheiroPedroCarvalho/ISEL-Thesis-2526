/*
 * Copyright © 9/2/2022, Pexers (https://github.com/Pexers)
 */

package quickfaas.configurations.exceptions;

import java.util.NoSuchElementException;

public final class NoSuchConfigurationException extends NoSuchElementException {
    public NoSuchConfigurationException(String configuration) {
        super("Configuration '" + configuration + "' was not found.");
    }
}
