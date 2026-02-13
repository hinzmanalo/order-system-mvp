package com.orderhub.common.exception;

/**
 * Exception thrown when an operation is attempted on an order in an invalid
 * state.
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String currentState, String attemptedAction) {
        super(String.format("Cannot perform action '%s' on order in state '%s'",
                attemptedAction, currentState));
    }
}
