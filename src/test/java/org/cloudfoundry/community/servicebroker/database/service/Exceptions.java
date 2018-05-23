package org.cloudfoundry.community.servicebroker.database.service;

import org.assertj.core.api.ThrowableAssert;

class Exceptions {
    
    static void swallowException(ThrowableAssert.ThrowingCallable throwingCallable) {
        try {
            throwingCallable.call();
        } catch (Throwable e) {
            System.out.println("failed to run " + throwingCallable + " due to - " + e.getMessage());
        }
    }
}