// Copyright 2007, 2010, 2011 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.ioc.internal;

import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.StringHolder;

public class MutlipleAutobuildServiceConstructorsModule
{
    public static void bind(ServiceBinder binder)
    {
        binder.bind(StringHolder.class, ToUpperCaseStringHolder.class).withSimpleId().preventReloading();
        binder.bind(StringHolder.class, MultipleConstructorsAutobuildService.class).preventReloading();
    }
}
