/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;

import java.text.MessageFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

public class AssertionsHelper extends Assertions {

    public static void assertThrowServiceException(String message, Executable executable) {
        ServiceException exception = Assertions.assertThrows(ServiceException.class, executable);
        Assertions.assertEquals(message, exception.getMessage());
    }

    public static void assertThrowsServiceException(Status status, Executable executable) {
        ServiceException exception = Assertions.assertThrows(ServiceException.class, executable);
        Assertions.assertEquals(status.getCode(), exception.getCode());
    }

    public static void assertThrowsServiceException(String message, Executable executable) {
        ServiceException exception = Assertions.assertThrows(ServiceException.class, executable);
        Assertions.assertEquals(MessageFormat.format(Status.INTERNAL_SERVER_ERROR_ARGS.getMsg(), message),
                exception.getMessage());
    }

    public static void assertDoesNotThrow(Executable executable) {
        Assertions.assertDoesNotThrow(executable);
    }

}
