/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.modelimpl.impl.services;

import java.io.File;
import java.util.List;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmObject;
import org.netbeans.modules.cnd.api.model.services.CsmFileInfoQuery;
import org.netbeans.modules.cnd.api.model.util.CsmTracer;
import org.netbeans.modules.cnd.api.model.xref.CsmReference;
import org.netbeans.modules.cnd.modelimpl.trace.TraceModelTestBase;
import org.netbeans.modules.cnd.support.Interrupter;

/**
 *
 * @author Vladimir Voskresensky
 */
public class MacroUsagesTestCase extends TraceModelTestBase {

    public MacroUsagesTestCase(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
//        System.setProperty("cnd.smart.parse", "true");
        super.setUp();
    }

    @Override
    protected void postTest(String[] args, Object... params) throws Exception {
        assertNotNull(params);
        assertTrue(params.length > 0);
        for (Object o : params) {
            assertTrue(o instanceof String);
            File file = getDataFile((String) o);
            assertTrue(file.exists());
            CsmFile csmFile = super.getCsmFile(file);
            assertTrue(csmFile != null);
            System.out.printf("Macro references for %s\n", csmFile.getName());
            List<CsmReference> macroRefs = CsmFileInfoQuery.getDefault().getMacroUsages(csmFile, Interrupter.DUMMY);
            for (CsmReference ref : macroRefs) {
                CsmObject refedObj = ref.getReferencedObject();
                System.out.printf("%s %s -> %s %s\n", ref.getText(), CsmTracer.getOffsetString(ref, false), ref.getKind(), CsmTracer.getOffsetString(refedObj, true));
            }
        }
    }

    private void doTest(String fileToParse) throws Exception {
        doTest(new String[]{fileToParse}, fileToParse, fileToParse);
    }

    private void doTest(String[] filesToParse, String fileToCheck, String goldenNameBase) throws Exception {
        super.performTest(filesToParse, goldenNameBase, (Object) fileToCheck);
    }

    public void testMacroUsagesSimple() throws Exception {
        doTest("macro_refs_simple.cc");
    }

    public void DISABLED_testMacroUsages210815() throws Exception {
        // #210815 - used macros is not highlighted in editor
        doTest(new String[]{"macro_refs_parse_1.cc", "macro_refs_parse_2.cc"}, "macro_refs_headers_parse.h", "macro_refs_headers_parse.h");
    }
}
