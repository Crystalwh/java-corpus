/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009-2012 Sun Microsystems, Inc.
 */
package org.netbeans.lib.nbjavac.services;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javadoc.JavadocClassReader;
import com.sun.tools.javadoc.Messager;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author lahvac
 */
public class CouplingTest extends TestCase {

    public void test200122() throws Exception {
        String code = "package test; public class Test { void t() { new Runnable() { public void run() {} }; } }";
        List<String> fqns = compile(code);

        assertEquals(testCoupling(code, false, fqns), testCoupling(code, true, fqns));
    }
    
    //<editor-fold defaultstate="collapsed" desc=" Test Infrastructure ">
    private static class MyFileObject extends SimpleJavaFileObject {
        private String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    private File workingDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        workingDir = File.createTempFile("CouplingTest", "");

        workingDir.delete();
        workingDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteRecursively(workingDir);
        super.tearDown();
    }

    private List<String> compile(String code) throws Exception {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        StandardJavaFileManager std = tool.getStandardFileManager(null, null, null);

        std.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(workingDir));

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, std, null, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov", "-XDshouldStopPolicy=GENERATE"), null, Arrays.asList(new MyFileObject(code)));
        Iterable<? extends CompilationUnitTree> cuts = ct.parse();

        ct.analyze();

        final List<String> result = new ArrayList<String>();

        new TreePathScanner<Void, Void>() {
            @Override public Void visitClass(ClassTree node, Void p) {
                Element el = Trees.instance(ct).getElement(getCurrentPath());

                if (el != null && (el.getKind().isClass() || el.getKind().isInterface())) {
                    result.add(ct.getElements().getBinaryName((TypeElement) el).toString());
                }

                return super.visitClass(node, p);
            }
        }.scan(cuts, null);

        ct.generate();

        return result;
    }

    private Set<String> testCoupling(String code, boolean loadFromClasses, List<String> fqns) throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        StandardJavaFileManager std = tool.getStandardFileManager(null, null, null);

        if (loadFromClasses) {
            std.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(workingDir));
            std.setLocation(StandardLocation.CLASS_PATH, Collections.singleton(workingDir));
        }

        JavacTaskImpl ct = Utilities.createJavac(std, Utilities.fileObjectFor(code));
        
        if (loadFromClasses) {
            for (String fqn : fqns) {
                assertNotNull(fqn, ct.getElements().getTypeElementByBinaryName(fqn));
            }
        }

        ct.parse();
        ct.analyze();

        Set<String> classInfo = new HashSet<String>();

        for (String fqn : fqns) {
            ClassSymbol clazz = ct.getElements().getTypeElementByBinaryName(fqn);
            StringBuilder info = new StringBuilder();

            info.append(clazz.flatname.toString()).append(",");
            info.append(Long.toHexString(clazz.flags() & ~(Flags.FROMCLASS | Flags.APT_CLEANED))).append(",");
            info.append(clazz.hasOuterInstance());

            classInfo.add(info.toString());
        }

        return classInfo;
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecursively(c);
            }
        }

        f.delete();
    }
    //</editor-fold>
}
