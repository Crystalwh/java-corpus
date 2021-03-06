/*
 * Copyright 2008 Ralf Joachim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exolab.castor.jdo;

/**
 * An attempt to open a JDBC connection failed.
 *
 * @author <a href="ralf.joachim@syscon.eu">Ralf Joachim</a>
 * @version $Revision: 8994 $ $Date: 2011-08-02 01:40:59 +0200 (Di, 02 Aug 2011) $
 */
public class ConnectionFailedException extends PersistenceException {
    /** SerialVersionUID. */
    private static final long serialVersionUID = 4841105599318131969L;

    public ConnectionFailedException(final String message) {
        super(message);
    }

    public ConnectionFailedException(final String message, final Throwable exception) {
        super(message, exception);
    }
}

