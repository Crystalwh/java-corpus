/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
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
package org.jboss.util.propertyeditor;

import java.math.BigDecimal;

/**
 * A property editor for {@link java.math.BigDecimal}.
 *
 * @version <tt>$Revision$</tt>
 * @author  <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author  <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 */
public class BigDecimalEditor extends TextPropertyEditorSupport
{
   /**
    * Returns a BigDecimal for the input object converted to a string.
    *
    * @return a BigDecimal object
    *
    */
   public Object getValue()
   {
      return new BigDecimal(getAsText());
   }
}