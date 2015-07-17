/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints.bugs;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CodeStyle;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.java.hints.ArithmeticUtilities;
import org.netbeans.modules.java.hints.errors.Utilities;
import org.netbeans.spi.java.hints.ConstraintVariableType;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.JavaFixUtilities;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author lahvac
 */
@Messages({
    "DN_indentation=Confusing indentation",
    "DESC_indentation=Warns about indentation that suggests possible missing surrounding block",
    "ERR_indentation=Confusing indentation",
    "TEXT_MissingSwitchCase=Possibly missing switch `case' statement",
    "FIX_AddMissingSwitchCase=Replace label with switch case",
})
public class Tiny {

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Tiny.stringReplaceAllDot", description = "#DESC_org.netbeans.modules.java.hints.bugs.Tiny.stringReplaceAllDot", category="bugs", suppressWarnings="ReplaceAllDot")
    @TriggerPattern(value="$str.replaceAll(\".\", $to)",
                    constraints=@ConstraintVariableType(variable="$str", type="java.lang.String"))
    public static ErrorDescription stringReplaceAllDot(HintContext ctx) {
        Tree constant = ((MethodInvocationTree) ctx.getPath().getLeaf()).getArguments().get(0);
        TreePath constantTP = new TreePath(ctx.getPath(), constant);

        String fixDisplayName = NbBundle.getMessage(Tiny.class, "FIX_string-replace-all-dot");
        Fix fix = JavaFixUtilities.rewriteFix(ctx, fixDisplayName, constantTP, "\"\\\\.\"");
        String displayName = NbBundle.getMessage(Tiny.class, "ERR_string-replace-all-dot");

        return ErrorDescriptionFactory.forTree(ctx, constant, displayName, fix);
    }

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Tiny.newObject", description = "#DESC_org.netbeans.modules.java.hints.bugs.Tiny.newObject", category="bugs", suppressWarnings="ResultOfObjectAllocationIgnored", options=Options.QUERY)
    //TODO: anonymous innerclasses?
    @TriggerPatterns({
        @TriggerPattern(value="new $type($params$);"),
        @TriggerPattern(value="$enh.new $type($params$);")
    })
    public static ErrorDescription newObject(HintContext ctx) {
        String displayName = NbBundle.getMessage(Tiny.class, "ERR_newObject");

        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), displayName);
    }

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Tiny.systemArrayCopy", description = "#DESC_org.netbeans.modules.java.hints.bugs.Tiny.systemArrayCopy", category="bugs", suppressWarnings="SuspiciousSystemArraycopy", options=Options.QUERY)
    @TriggerPattern(value="java.lang.System.arraycopy($src, $srcPos, $dest, $destPos, $length)")
    public static List<ErrorDescription> systemArrayCopy(HintContext ctx) {
        List<ErrorDescription> result = new LinkedList<ErrorDescription>();

        for (String objName : Arrays.asList("$src", "$dest")) {
            TreePath obj = ctx.getVariables().get(objName);
            TypeMirror type = ctx.getInfo().getTrees().getTypeMirror(obj);

            if (Utilities.isValidType(type) && type.getKind() != TypeKind.ARRAY) {
                String treeDisplayName = Utilities.shortDisplayName(ctx.getInfo(), (ExpressionTree) obj.getLeaf());
                String displayName = NbBundle.getMessage(Tiny.class, "ERR_system_arraycopy_notarray", treeDisplayName);
                
                result.add(ErrorDescriptionFactory.forTree(ctx, obj, displayName));
            }
        }

        for (String countName : Arrays.asList("$srcPos", "$destPos", "$length")) {
            TreePath count = ctx.getVariables().get(countName);
            Number value = ArithmeticUtilities.compute(ctx.getInfo(), count, true);

            if (value != null && value.intValue() < 0) {
                String treeDisplayName = Utilities.shortDisplayName(ctx.getInfo(), (ExpressionTree) count.getLeaf());
                String displayName = NbBundle.getMessage(Tiny.class, "ERR_system_arraycopy_negative", treeDisplayName);

                result.add(ErrorDescriptionFactory.forTree(ctx, count, displayName));
            }
        }

        return result;
    }


    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Tiny.equalsNull", description = "#DESC_org.netbeans.modules.java.hints.bugs.Tiny.equalsNull", category="bugs", suppressWarnings="ObjectEqualsNull")
    @TriggerPattern(value="$obj.equals(null)")
    public static ErrorDescription equalsNull(HintContext ctx) {
        String fixDisplayName = NbBundle.getMessage(Tiny.class, "FIX_equalsNull");
        Fix fix = JavaFixUtilities.rewriteFix(ctx, fixDisplayName, ctx.getPath(), "$obj == null");
        String displayName = NbBundle.getMessage(Tiny.class, "ERR_equalsNull");

        return ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), displayName, fix);
    }

    @Hint(displayName = "#DN_org.netbeans.modules.java.hints.bugs.Tiny.resultSet", description = "#DESC_org.netbeans.modules.java.hints.bugs.Tiny.resultSet", category="bugs", suppressWarnings="UseOfIndexZeroInJDBCResultSet", options=Options.QUERY)
    @TriggerPattern(value="$set.$method($columnIndex, $other$)",
                    constraints={
                        @ConstraintVariableType(variable="$set", type="java.sql.ResultSet"),
                        @ConstraintVariableType(variable="$columnIndex", type="int")
                    })
    public static ErrorDescription resultSet(HintContext ctx) {
        TypeElement resultSet = ctx.getInfo().getElements().getTypeElement("java.sql.ResultSet");
        String methodName = ctx.getVariableNames().get("$method");

        if (resultSet == null || !METHOD_NAME.contains(methodName)) {
            return null;
        }

        TreePath columnIndex = ctx.getVariables().get("$columnIndex");
        Number value = ArithmeticUtilities.compute(ctx.getInfo(), columnIndex, true);

        if (value == null) {
            return null;
        }

        int intValue = value.intValue();

        if (intValue > 0) {
            return null;
        }

        Element methodEl = ctx.getInfo().getTrees().getElement(ctx.getPath());

        if (methodEl == null || methodEl.getKind() != ElementKind.METHOD) {
            return null;
        }

        ExecutableElement methodElement = (ExecutableElement) methodEl;
        boolean found = false;

        for (ExecutableElement e : ElementFilter.methodsIn(resultSet.getEnclosedElements())) {
            if (e.equals(methodEl)) {
                found = true;
                break;
            }
            if (ctx.getInfo().getElements().overrides(methodElement, e, (TypeElement) methodElement.getEnclosingElement())) {
                found = true;
                break;
            }
        }

        if (!found) {
            return null;
        }

        String key = intValue == 0 ? "ERR_ResultSetZero" : "ERR_ResultSetNegative";
        String displayName = NbBundle.getMessage(Tiny.class, key);

        return ErrorDescriptionFactory.forName(ctx, columnIndex, displayName);
    }
    
    private static final Set<String> METHOD_NAME = new HashSet<String>(Arrays.asList(
            "getString", "getBoolean", "getByte", "getShort", "getInt", "getLong",
            "getFloat", "getDouble", "getBigDecimal", "getBytes", "getDate",
            "getTime", "getTimestamp", "getAsciiStream", "getUnicodeStream",
            "getBinaryStream", "getObject", "getCharacterStream", "getBigDecimal",
            "updateNull", "updateBoolean", "updateByte", "updateShort", "updateInt",
            "updateLong", "updateFloat", "updateDouble", "updateBigDecimal", "updateString",
            "updateBytes", "updateDate", "updateTime", "updateTimestamp", "updateAsciiStream",
            "updateBinaryStream", "updateCharacterStream", "updateObject", "updateObject",
            "getObject", "getRef", "getBlob", "getClob", "getArray", "getDate", "getTime",
            "getTimestamp", "getURL", "updateRef", "updateBlob", "updateClob", "updateArray",
            "getRowId", "updateRowId", "updateNString", "updateNClob", "getNClob", "getSQLXML",
            "updateSQLXML", "getNString", "getNCharacterStream", "updateNCharacterStream",
            "updateAsciiStream", "updateBinaryStream", "updateCharacterStream", "updateBlob",
            "updateClob", "updateNClob", "updateNCharacterStream", "updateAsciiStream",
            "updateBinaryStream", "updateCharacterStream", "updateBlob", "updateClob",
            "updateNClob"
    ));
    
    @Hint(displayName = "#DN_indentation", description = "#DESC_indentation", category="bugs", suppressWarnings="SuspiciousIndentAfterControlStatement", options=Options.QUERY)
    @TriggerTreeKind({Kind.IF, Kind.WHILE_LOOP, Kind.FOR_LOOP, Kind.ENHANCED_FOR_LOOP})
    public static ErrorDescription indentation(HintContext ctx) {
        Tree firstStatement;
        Tree found = ctx.getPath().getLeaf();
        
        switch (found.getKind()) {
            case IF:
                IfTree it = (IfTree) found;
                if (it.getElseStatement() != null) firstStatement = it.getElseStatement();
                else firstStatement = it.getThenStatement();
                break;
            case WHILE_LOOP:
                firstStatement = ((WhileLoopTree) found).getStatement();
                break;
            case FOR_LOOP:
                firstStatement = ((ForLoopTree) found).getStatement();
                break;
            case ENHANCED_FOR_LOOP:
                firstStatement = ((EnhancedForLoopTree) found).getStatement();
                break;
            default:
                return null;
        }
        
        if (firstStatement != null && firstStatement.getKind() == Kind.BLOCK) {
            return null;
        }
        
        Tree parent = ctx.getPath().getParentPath().getLeaf();
        List<? extends Tree> parentStatements;
        
        switch (parent.getKind()) {
            case BLOCK: parentStatements = ((BlockTree) parent).getStatements(); break;
            case CASE: parentStatements = ((CaseTree) parent).getStatements(); break;
            default: return null;
        }
        
        int index = parentStatements.indexOf(found);
        
        if (index < 0 || index + 1 >= parentStatements.size()) return null;
        
        Tree secondStatement = parentStatements.get(index + 1);
        int firstIndent = indent(ctx, firstStatement);
        int secondIndent = indent(ctx, secondStatement);
        
        if (firstIndent == (-1) || secondIndent == (-1) || firstIndent != secondIndent) return null;
        
        return ErrorDescriptionFactory.forTree(ctx, secondStatement, Bundle.ERR_indentation());
    }
    
    private static int indent(HintContext ctx, Tree t) {
        long start = ctx.getInfo().getTrees().getSourcePositions().getStartPosition(ctx.getInfo().getCompilationUnit(), t);
        LineMap lm = ctx.getInfo().getCompilationUnit().getLineMap();
        // see defect #240493; incorrect data may be provided by Lombok processing.
        if (start == -1) {
            return -1;
        }
        long lno = lm.getLineNumber(start);
        if (lno < 1) {
            return -1;
        }
        long lineStart = lm.getStartPosition(lno);
        String text = ctx.getInfo().getText();
        CodeStyle cs = CodeStyle.getDefault(ctx.getInfo().getFileObject());
        int indent = 0;
        
        while (start-- > lineStart) {
            char c = text.charAt((int) start);
            if (c == ' ') indent++;
            else if (c == '\t') indent += cs.getTabSize();
            else return -1;
        }
        
        return indent;
    }
    
    @Hint(category = "bugs", displayName = "#DN_MissingSwitchcase", description = "#DESC_MissingSwitchcase", 
            enabled = true, severity = Severity.VERIFIER)
//    @TriggerPattern("switch ($expr) { $cases1$; case $c: $stmts1$; $l: $stmt; $stmts2$; $cases2$;")
    @TriggerPattern("case $c: $stmts1$; $l: $stmt;")
    public static ErrorDescription switchCaseLabelMismatch(HintContext ctx) {
        TreePath path = ctx.getPath();
        if (path.getLeaf().getKind() != Tree.Kind.CASE) {
            return null;
        }
        final CompilationInfo ci = ctx.getInfo();
        Tree swTree = path.getParentPath().getLeaf();
        assert swTree.getKind() == Tree.Kind.SWITCH;
        Tree xp = ((SwitchTree)swTree).getExpression();
        TypeMirror m = ci.getTrees().getTypeMirror(new TreePath(path.getParentPath(), xp));
        if (m == null || m.getKind() != TypeKind.DECLARED) {
            return null;
        }
        Element e = ((DeclaredType)m).asElement();
        // check that the switch expression is an enum; enum identifiers can be confused with labels
        if (e == null || e.getKind() != ElementKind.ENUM) {
            return null;
        }
        
        // check that the label is not used within its case statement in no break / continue clause
        // the $l is bound to the label identifier, not to the labeled statement!
        TreePath stPath = ctx.getVariables().get("$stmt"); // NOI18N
        TreePath lPath = stPath.getParentPath();
        LabeledStatementTree lt = (LabeledStatementTree)lPath.getLeaf();
        final Name l = lt.getLabel();
        Boolean b = new TreeScanner<Boolean, Void>() {

            @Override
            public Boolean reduce(Boolean r1, Boolean r2) {
                if (r1 == null) {
                    return r2;
                } else if (r2 == null) {
                    return r1;
                } else {
                    return r1 || r2;
                }
            }

            @Override
            public Boolean visitContinue(ContinueTree node, Void p) {
                return node.getLabel() == l;
            }

            @Override
            public Boolean visitBreak(BreakTree node, Void p) {
                return node.getLabel() == l;
            }
            
        }.scan(path.getLeaf(), null);
        if (Boolean.TRUE == b) {
            // label is a target of a break/continue do not report.
            return null;
        }
        
        return ErrorDescriptionFactory.forName(ctx, lt, Bundle.TEXT_MissingSwitchCase(), 
                JavaFixUtilities.rewriteFix(ctx, Bundle.FIX_AddMissingSwitchCase(), lPath, 
                    "case " + l.toString() + ": $stmt;"));
    }
    
    @Hint(
            displayName = "#DN_HashCodeOnArray",
            description = "#DESC_HashCodeOnArray",
            category = "bugs",
            enabled = true,
            suppressWarnings = { "ArrayHashCode" }
    )
    @TriggerPatterns({
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "java.lang.Object[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "int[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "short[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "byte[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "long[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "char[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "float[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "double[]", variable = "$v")
                }
        ),
        @TriggerPattern(
                value = "$v.hashCode()", 
                constraints = {
                    @ConstraintVariableType(type = "boolean[]", variable = "$v")
                }
        )
    })
    @NbBundle.Messages({
        "TEXT_HashCodeOnArray=hashCode() called on array instance",
        "FIX_UseArraysHashCode=Use Arrays.hashCode()",
        "FIX_UseArraysDeepHashCode=Use Arrays.deepHashCode()"
    })
    public static List<ErrorDescription> hashCodeOnArray(HintContext ctx) {
        CompilationInfo ci = ctx.getInfo();
        TreePath arrayRef = ctx.getVariables().get("$v"); // NOI18N
        boolean enableDeep = ArrayStringConversions.canContainArrays(ci, arrayRef);
        List<ErrorDescription> result = new ArrayList<ErrorDescription>(enableDeep ? 2 : 1);
        TreePathHandle handle = TreePathHandle.create(ctx.getPath(), ci);
        result.add(ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), Bundle.TEXT_HashCodeOnArray(),
                new HashCodeFix(false, handle).toEditorFix()));
        if (enableDeep) {
            result.add(ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), Bundle.TEXT_HashCodeOnArray(),
                    new HashCodeFix(true, handle).toEditorFix()));
        }
        return result;
    }
    
    private static final class HashCodeFix extends JavaFix {
        private final boolean deep;

        public HashCodeFix(boolean deep, TreePathHandle handle) {
            super(handle);
            this.deep = deep;
        }
        
        @Override
        protected String getText() {
            return deep ? Bundle.FIX_UseArraysDeepHashCode() : Bundle.FIX_UseArraysHashCode();
        }

        @Override
        protected void performRewrite(JavaFix.TransformationContext ctx) throws Exception {
            Tree t = ctx.getPath().getLeaf();
            if (t.getKind() != Tree.Kind.METHOD_INVOCATION) {
                return;
            }
            MethodInvocationTree mi = (MethodInvocationTree)t;
            if (mi.getMethodSelect().getKind() != Tree.Kind.MEMBER_SELECT) {
                return;
            }
            MemberSelectTree selector = ((MemberSelectTree)mi.getMethodSelect());
            TreeMaker maker = ctx.getWorkingCopy().getTreeMaker();
            ExpressionTree ms = maker.MemberSelect(maker.QualIdent("java.util.Arrays"), deep ? "deepHashCode" : "hashCode"); // NOI18N
            Tree nue = maker.MethodInvocation(
                            Collections.<ExpressionTree>emptyList(), 
                            ms, 
                            Collections.singletonList(selector.getExpression())
            );
            ctx.getWorkingCopy().rewrite(t, nue);
        }
    }
}