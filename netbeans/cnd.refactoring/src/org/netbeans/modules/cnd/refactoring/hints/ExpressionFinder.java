/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.refactoring.hints;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.cnd.api.lexer.CppTokenId;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.cnd.api.model.CsmClass;
import org.netbeans.modules.cnd.api.model.CsmClassifier;
import org.netbeans.modules.cnd.api.model.CsmDeclaration;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmFunctionDefinition;
import org.netbeans.modules.cnd.api.model.CsmNamespaceDefinition;
import org.netbeans.modules.cnd.api.model.CsmOffsetable;
import org.netbeans.modules.cnd.api.model.CsmOffsetableDeclaration;
import org.netbeans.modules.cnd.api.model.CsmType;
import org.netbeans.modules.cnd.api.model.CsmVariable;
import org.netbeans.modules.cnd.api.model.deep.CsmCompoundStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmCondition;
import org.netbeans.modules.cnd.api.model.deep.CsmDeclarationStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmExceptionHandler;
import org.netbeans.modules.cnd.api.model.deep.CsmExpression;
import org.netbeans.modules.cnd.api.model.deep.CsmExpressionStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmForStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmIfStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmLoopStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmSwitchStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmTryCatchStatement;
import org.netbeans.modules.cnd.api.model.services.CsmCacheManager;
import org.netbeans.modules.cnd.api.model.services.CsmExpressionResolver;
import org.netbeans.modules.cnd.api.model.services.CsmFileInfoQuery;
import org.netbeans.modules.cnd.api.model.services.CsmInstantiationProvider;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.refactoring.actions.InstantRenamePerformer;
import org.netbeans.modules.cnd.utils.MIMENames;
import org.netbeans.modules.cnd.utils.cache.CharSequenceUtils;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author alsimon
 */
public class ExpressionFinder {
    private final Document doc;
    private final CsmFile file;
    private final int caretOffset;
    private final int selectionStart;
    private final int selectionEnd;
    private final AtomicBoolean canceled;
    private StatementResult result;
    
    public ExpressionFinder(Document doc, CsmFile file, int caretOffset, int selectionStart, int selectionEnd, AtomicBoolean canceled) {
        this.doc = doc;
        this.file = file;
        this.caretOffset = caretOffset;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.canceled = canceled;
    }

    public StatementResult findExpressionStatement() {
        result = findExpressionStatement(file.getDeclarations());
        return result;
    }
    
    private StatementResult findExpressionStatement(Collection<? extends CsmOffsetableDeclaration> decls) {
        for(CsmOffsetableDeclaration decl : decls) {
            if (canceled.get()) {
                return null;
            }
            if (decl.getStartOffset() < selectionStart && selectionEnd < decl.getEndOffset()) {
                if (CsmKindUtilities.isFunctionDefinition(decl)) {
                    CsmFunctionDefinition def = (CsmFunctionDefinition) decl;
                    return findExpressionStatementInBody(def.getBody());
                } else if (CsmKindUtilities.isNamespaceDefinition(decl)) {
                    CsmNamespaceDefinition def = (CsmNamespaceDefinition) decl;
                    return findExpressionStatement(def.getDeclarations());
                } else if (CsmKindUtilities.isClass(decl)) {
                    CsmClass cls = (CsmClass) decl;
                    return findExpressionStatement(cls.getMembers());
                }
            }
        }
        return null;
    }
    
    private StatementResult findExpressionStatementInBody(CsmCompoundStatement body) {
        if (body != null) {
            final List<CsmStatement> statements = body.getStatements();
            for(int i = 0; i < statements.size(); i++) {
                if (canceled.get())  {
                    break;
                }
                final CsmStatement st = statements.get(i);
                final int startOffset = st.getStartOffset();
                if (startOffset > selectionStart) {
                    break;
                }
                final int nexStartOffset;
                if(i+1 < statements.size()) {
                   nexStartOffset = statements.get(i+1).getStartOffset();
                } else {
                   nexStartOffset = body.getEndOffset();
                }
                if (startOffset <= selectionStart && selectionEnd < nexStartOffset) {
                    StatementResult res = findExpressionStatement(st, nexStartOffset);
                    if (res != null && res.statementInBody == null) {
                        res.statementInBody = st;
                    }
                    return res;
                }
            }
        }
        return null;
    }

    private StatementResult findExpressionStatement(final CsmStatement st, final int nexStartOffset) {
        switch(st.getKind()) {
            case COMPOUND:
                return findExpressionStatementInBody((CsmCompoundStatement)st);
            case SWITCH:
            {
                CsmSwitchStatement switchStmt = (CsmSwitchStatement) st;
                CsmCondition condition = switchStmt.getCondition();
                if (condition != null &&
                    condition.getStartOffset() <= selectionStart && selectionEnd <= condition.getEndOffset()) {
                    StatementResult res = new StatementResult();
                    res.container = st;
                    return res;
                }
                final CsmStatement body = switchStmt.getBody();
                if (body != null) {
                    final int startOffset = body.getStartOffset();
                    if (startOffset <= selectionStart && selectionEnd < nexStartOffset) {
                        return findExpressionStatement(body, nexStartOffset);
                    }
                }
                return null;
            }
            case FOR: 
            {
                CsmForStatement forStmt = (CsmForStatement) st;
                CsmStatement initStatement = forStmt.getInitStatement();
                if (initStatement != null && 
                    initStatement.getStartOffset() <= selectionStart && selectionEnd <= initStatement.getEndOffset()) {
                    StatementResult res = new StatementResult();
                    res.container = st;
                    return res;
                }
                //CsmExpression iterationExpression = forStmt.getIterationExpression();
                //if (iterationExpression != null && 
                //    iterationExpression.getStartOffset() <= selectionStrat && selectionEnd <= iterationExpression.getEndOffset()) {
                //    StatementResult res = new StatementResult();
                //    res.container = st;
                //    return res;
                //}
                //CsmCondition condition = forStmt.getCondition();
                //if (condition != null && 
                //    condition.getStartOffset() <= selectionStrat && selectionEnd <= condition.getEndOffset()) {
                //    StatementResult res = new StatementResult();
                //    res.container = st;
                //    return res;
                //}
                CsmStatement body = forStmt.getBody();
                if (body != null) {
                    final int startOffset = body.getStartOffset();
                    if (startOffset <= selectionStart && selectionEnd < nexStartOffset) {
                        return findExpressionStatement(body, nexStartOffset);
                    }
                }
                return null;
            }
            case WHILE:
            case DO_WHILE:
            {
                CsmLoopStatement loopStmt = (CsmLoopStatement) st;
                //CsmCondition condition = loopStmt.getCondition();
                //if (condition != null && 
                //    condition.getStartOffset() <= selectionStrat && selectionEnd <= condition.getEndOffset()) {
                //    StatementResult res = new StatementResult();
                //    res.container = st;
                //    return res;
                //}
                CsmStatement body = loopStmt.getBody();
                if (body != null) {
                    final int startOffset = body.getStartOffset();
                    int endOffset = nexStartOffset;
                    if (loopStmt.isPostCheck()) {
                        CsmCondition condition = loopStmt.getCondition();
                        if (condition != null) {
                            endOffset = condition.getStartOffset();
                        }
                    }
                    if (startOffset <= selectionStart && selectionEnd < endOffset) {
                        return findExpressionStatement(body, endOffset);
                    }
                }
                return null;
            }
            case TRY_CATCH:
            {
                CsmTryCatchStatement tryStmt = (CsmTryCatchStatement) st;
                CsmStatement tryBody = tryStmt.getTryStatement();
                List<CsmExceptionHandler> handlers = tryStmt.getHandlers();
                if (tryBody != null) {
                    final int startOffset = tryBody.getStartOffset();
                    int endOffset = nexStartOffset;
                    if (handlers != null && handlers.size() > 0) {
                        endOffset = handlers.get(0).getStartOffset();
                    }
                    if (startOffset <= selectionStart && selectionEnd < endOffset) {
                        return findExpressionStatement(tryBody, endOffset);
                    }
                }
                if (handlers != null) {
                    for(int i = 0; i < handlers.size(); i++) {
                        CsmExceptionHandler handler = handlers.get(i);
                        final int startOffset = handler.getStartOffset();
                        final int endOffset = handler.getEndOffset();
                        if (startOffset <= selectionStart && selectionEnd < endOffset) {
                            return findExpressionStatement(handler, endOffset);
                        }
                    }
                }
                return null;
            }
            case IF:
            {
                CsmIfStatement ifStmt = (CsmIfStatement) st;
                CsmCondition condition = ifStmt.getCondition();
                if (condition != null && 
                    condition.getStartOffset() <= selectionStart && selectionEnd <= condition.getEndOffset()) {
                    StatementResult res = new StatementResult();
                    res.container = st;
                    return res;
                }
                CsmStatement thenStmt = ifStmt.getThen();
                CsmStatement elseStmt = ifStmt.getElse();
                if (thenStmt != null) {
                    final int startOffset = thenStmt.getStartOffset();
                    int endOffset = thenStmt.getEndOffset();
                    if (elseStmt != null) {
                        endOffset = elseStmt.getStartOffset();
                    }
                    if (startOffset <= selectionStart && selectionEnd < endOffset) {
                        return findExpressionStatement(thenStmt, endOffset);
                    }
                }
                if (elseStmt != null) {
                    final int startOffset = elseStmt.getStartOffset();
                    int endOffset = nexStartOffset;
                    if (startOffset <= selectionStart && selectionEnd < endOffset) {
                        return findExpressionStatement(elseStmt, endOffset);
                    }
                }
                return null;
            }
            case RETURN:
            {
                StatementResult res = new StatementResult();
                res.container = st;
                return res;
            }
            case DECLARATION:
            {
                CsmDeclarationStatement decls = (CsmDeclarationStatement) st;
                List<CsmDeclaration> declarators = decls.getDeclarators();
                for(CsmDeclaration decl : declarators) {
                    if (decl instanceof CsmVariable) {
                        CsmVariable d = (CsmVariable) decl;
                        if (d.getStartOffset() <= selectionStart && selectionEnd <= d.getEndOffset()) {
                            CsmExpression initialValue = d.getInitialValue();
                            if (initialValue != null) {
                                if (initialValue.getStartOffset() <= selectionStart && selectionEnd <= initialValue.getEndOffset()) {
                                    StatementResult res = new StatementResult();
                                    res.container = st;
                                    return res;
                                }
                            }
                        }
                    }
                }
                return null;
            }
            case EXPRESSION:
            {
                final int startOffset = st.getStartOffset();
                final int endOffset = st.getEndOffset();
                final AtomicInteger trueEndOffset = new AtomicInteger(endOffset);
                doc.render(new Runnable() {

                    @Override
                    public void run() {
                        TokenHierarchy<Document> hi = TokenHierarchy.get(doc);
                        TokenSequence<?> ts = hi.tokenSequence();
                        ts.move(endOffset);
                        while (ts.moveNext()) {
                            Token<?> token = ts.token();
                            if (ts.offset() >= nexStartOffset) {
                                break;
                            }
                            if (CppTokenId.SEMICOLON.equals(token.id())) {
                                trueEndOffset.set(ts.offset()+1);
                                break;
                            }
                        }
                    }
                });
                if (startOffset <= selectionStart && selectionEnd <= trueEndOffset.get()) {
                    if(isApplicable((CsmExpressionStatement) st)) {
                        StatementResult res = new StatementResult();
                        res.expression = (CsmExpressionStatement) st;
                        res.container = (CsmExpressionStatement) st;
                        return res;
                    } else {
                        StatementResult res = new StatementResult();
                        res.container = (CsmExpressionStatement) st;
                        return res;
                    }
                }
                return null;
            }
        }
        return null;
    }
    
    private boolean isApplicable(CsmExpressionStatement st) {
        return isApplicableExpression(st.getExpression());        
    }
    
    CsmOffsetable applicableTextExpression() {
        try {
            String text = doc.getText(selectionStart, selectionEnd - selectionStart);
            if (text.length() > 0) {
                CsmOffsetable csmOffsetable = new CsmOffsetableImpl(file, selectionStart, selectionEnd, text);
                if (isApplicableExpression(csmOffsetable)) {
                    return csmOffsetable;
                }
            }
        } catch (BadLocationException ex) {
        }
        return null;
    }
    
    private boolean isApplicableExpression(CsmOffsetable expression) {
        final int startOffset = expression.getStartOffset();
        final int endOffset = expression.getEndOffset();
        final AtomicBoolean isAssignment = new AtomicBoolean(false);
        final AtomicBoolean isSingleID = new AtomicBoolean(true);
        doc.render(new Runnable() {

            @Override
            public void run() {
                TokenHierarchy<Document> hi = TokenHierarchy.get(doc);
                TokenSequence<?> ts = hi.tokenSequence();
                ts.move(startOffset);
                while (ts.moveNext()) {
                    Token<?> token = ts.token();
                    if (ts.offset() >= endOffset) {
                        break;
                    }
                    if (CppTokenId.EQ.equals(token.id())
                        ||CppTokenId.MINUSEQ.equals(token.id())
                        ||CppTokenId.STAREQ.equals(token.id())
                        ||CppTokenId.SLASHEQ.equals(token.id())
                        ||CppTokenId.AMPEQ.equals(token.id())
                        ||CppTokenId.BAREQ.equals(token.id())
                        ||CppTokenId.CARETEQ.equals(token.id())
                        ||CppTokenId.PERCENTEQ.equals(token.id())
                        ||CppTokenId.LTLTEQ.equals(token.id())
                        ||CppTokenId.GTGTEQ.equals(token.id())) {
                        isAssignment.set(true);
                        break;
                    }
                    if (CppTokenId.COMMENT_CATEGORY.equals(token.id().primaryCategory()) ||
                        CppTokenId.WHITESPACE_CATEGORY.equals(token.id().primaryCategory()) ||
                        CppTokenId.IDENTIFIER_CATEGORY.equals(token.id().primaryCategory())) {
                        //
                    } else {
                        isSingleID.set(false);
                    }
                }
            }
        });
        if (isAssignment.get()) {
            return false;
        }
        if (isSingleID.get()) {
            return false;
        }
        CsmType resolveType = CsmExpressionResolver.resolveType(expression, null);
        if (resolveType == null) {
            return false;
        }
        //final String typeText = resolveType.getCanonicalText().toString();
        final String typeText = resolveType.getText().toString();
        if ("void".equals(typeText)) { //NOI18N
            return false;
        }
        return true;
    }
    
    boolean isExpressionSelection() {
        final AtomicBoolean applicableSelection = new AtomicBoolean(false);
        if (selectionStart < selectionEnd) {
            doc.render(new Runnable() {

                @Override
                public void run() {
                    TokenHierarchy<? extends Document> hi = TokenHierarchy.get(doc);
                    TokenSequence<?> ts = hi.tokenSequence();
                    // selection end between tokens?
                    ts.move(selectionEnd);
                    boolean res = false;
                    if(ts.moveNext()) {
                        int from = ts.offset();
                        if (selectionEnd == from) {
                            res = true;
                        }
                    }
                    if (!res) {
                        return;
                    }
                    // selection start between tokens?
                    ts.move(selectionStart);
                    res = false;
                    if(ts.movePrevious()) {
                        Token<?> token = ts.token();
                        int to = ts.offset()+token.length();
                        if (selectionStart == to) {
                            res = true;
                        }
                    }
                    if (!res) {
                        return;
                    }
                    // finally count paren balance
                    ts.move(selectionStart);
                    int count = 0;
                    while(ts.moveNext()) {
                        Token<?> token = ts.token();
                        if (ts.offset() >= selectionEnd) {
                            break;
                        }
                        if (token.id() == CppTokenId.LPAREN) {
                            count++;
                        }
                        if (token.id() == CppTokenId.RPAREN) {
                            count--;
                        }
                    }
                    if (count != 0) {
                        return;
                    }
                    applicableSelection.set(true);
                }
            });
        }
        return applicableSelection.get();
    }
    
    
    static final class StatementResult {
        CsmExpressionStatement expression;
        CsmStatement container;
        CsmStatement statementInBody;
    }
    
    static abstract class BaseFixImpl implements Fix {
        protected final CsmOffsetable expression;
        protected final BaseDocument doc;
        protected String name;
        
        protected BaseFixImpl(CsmOffsetable expression, Document doc) {
            this.expression = expression;
            this.doc = (BaseDocument) doc;
        }
        
        abstract protected boolean isC();

        protected String suggestName() {
            doc.render(new Runnable() {

                @Override
                public void run() {
                    TokenHierarchy<? extends Document> hi = TokenHierarchy.get(doc);
                    TokenSequence<?> ts = hi.tokenSequence();
                    ts.move(expression.getStartOffset());
                    String lastCandidate = null;
                    String bestCandidate = null;
                    int parenDepth = 0;
                    while (ts.moveNext()) {
                        Token<?> token = ts.token();
                        if (ts.offset() > expression.getEndOffset()) {
                            break;
                        }
                        if (CppTokenId.IDENTIFIER.equals(token.id())) {
                            lastCandidate = token.text().toString();
                        } else if (CppTokenId.LPAREN.equals(token.id())) {
                            if (parenDepth == 0) {
                                bestCandidate = lastCandidate;
                            }
                            parenDepth++;
                        } else if (CppTokenId.RPAREN.equals(token.id())) {
                            parenDepth--;
                        }
                    }
                    if (bestCandidate != null) {
                        name = bestCandidate;
                    } else {
                        name = lastCandidate;
                    }
                }
            });
            if (name == null) {
                name = "variable"; //NOI18N
            } else {
                if ((name.toLowerCase().startsWith("get") || name.toLowerCase().startsWith("has")) && name.length() > 3) { //NOI18N
                    name = name.substring(3);
                } else if (name.toLowerCase().startsWith("is") && name.length() > 2) { //NOI18N
                    name = name.substring(2);
                }
            }
            return name;
        }

        protected CharSequence getExpressionType() {
            CsmCacheManager.enter();
            try {
                CharSequence typeText;
                CsmType resolveType = CsmExpressionResolver.resolveType(expression, null);
                if (resolveType == null) {
                    return null;
                }
//                if (resolveType.isTemplateBased()) {
//                    CsmClassifier classifier = CsmBaseUtilities.getClassifier(resolveType, expression.getContainingFile(), expression.getStartOffset(), true);
//                    if (!CsmKindUtilities.isTemplate(classifier)) {
//                        CsmTypes.TypeDescriptor typeDescriptor = new CsmTypes.TypeDescriptor(resolveType.isConst(), resolveType.isReference(), resolveType.getPointerDepth(), resolveType.getArrayDepth());
//                        CsmTypes.OffsetDescriptor offsetDescriptor = new CsmTypes.OffsetDescriptor(expression.getContainingFile(), expression.getStartOffset(), expression.getEndOffset());
//                        resolveType = CsmTypes.createType(classifier, typeDescriptor, offsetDescriptor);
//                    }
//                }
                typeText = CsmInstantiationProvider.getDefault().getInstantiatedText(resolveType);
                if (isC()) {
                    CsmClassifier classifier = resolveType.getClassifier();
                    if (classifier != null) {
                        if (classifier.getKind() == CsmDeclaration.Kind.STRUCT && !CharSequenceUtils.startsWith(typeText, "struct")) { //NOI18N
                            typeText = "struct "+typeText; //NOI18N
                        }
                    }
                }
                return typeText;
            } finally {
                CsmCacheManager.leave();
            }
        }
    }
    
    static final class AssignmentFixImpl extends BaseFixImpl {
        private final FileObject fo;

        AssignmentFixImpl(CsmExpression expression, Document doc, FileObject fo) {
            super(expression, doc);
            this.fo = fo;
        }

        @Override
        public String getText() {
            return NbBundle.getMessage(SuggestionFactoryTask.class, "FIX_AssignResultToVariable"); //NOI18N
        }

        @Override
        protected boolean isC() {
            return MIMENames.C_MIME_TYPE.equals(fo.getMIMEType());
        }
        
        @Override
        public ChangeInfo implement() throws Exception {
            final CharSequence typeText = getExpressionType();
            if (typeText == null || "void".contentEquals(typeText)) { //NOI18N
                return null;
            }
            final String aName = suggestName();
            final String text = typeText+" "+aName+" = "; //NOI18N
            doc.insertString(expression.getStartOffset(), text, null);
            Position startPosition = new Position() {
                
                @Override
                public int getOffset() {
                    return expression.getStartOffset()+typeText.length()+1;
                }
            };
            Position endPosition = new Position() {
                
                @Override
                public int getOffset() {
                    return expression.getStartOffset()+text.length() - 3;
                }
            };
            ChangeInfo changeInfo = new ChangeInfo(fo, startPosition, endPosition);
            return changeInfo;
        }        
    }
    
    static final class IntroduceFixImpl extends BaseFixImpl {
        private final CsmStatement st;
        private final FileObject fo;
        private final JTextComponent comp;

        IntroduceFixImpl(CsmStatement st, CsmOffsetable expression, Document doc, JTextComponent comp, FileObject fo) {
            super(expression, doc);
            this.fo = fo;
            this.st = st;
            this.comp = comp;
        }

        @Override
        public String getText() {
            return NbBundle.getMessage(SuggestionFactoryTask.class, "FIX_IntroduceVariable"); //NOI18N
        }

        @Override
        protected boolean isC() {
            return MIMENames.C_MIME_TYPE.equals(fo.getMIMEType());
        }
        
        @Override
        public ChangeInfo implement() throws Exception {
            final CharSequence typeText = getExpressionType();
            if (typeText == null || "void".contentEquals(typeText)) { //NOI18N
                return null;
            }            
            final String aName = suggestName();
            final String exprText = expression.getText().toString();
            final ChangeInfo changeInfo = new ChangeInfo();
            final String typeTextPrefix = typeText+" ";//NOI18N
            final String text = typeTextPrefix+aName+" = "+expression.getText()+";\n"; //NOI18N
            doc.runAtomicAsUser(new Runnable() {
                @Override
                public void run() {
                    try {
                        doc.remove(expression.getStartOffset(), exprText.length());
                        doc.insertString(expression.getStartOffset(), aName, null);
                        Position exprStart = NbDocument.createPosition(doc, expression.getStartOffset(), Position.Bias.Forward);
                        Position exprEnd = NbDocument.createPosition(doc, expression.getStartOffset() + aName.length(), Position.Bias.Backward);
                        doc.insertString(st.getStartOffset(), text, null);
                        Position stmtStart = NbDocument.createPosition(doc, st.getStartOffset() + typeTextPrefix.length(), Position.Bias.Forward);
                        Position stmtEnd = NbDocument.createPosition(doc, st.getStartOffset() + typeTextPrefix.length() +aName.length(), Position.Bias.Backward);
                        changeInfo.add(fo, stmtStart, stmtEnd);
                        changeInfo.add(fo, exprStart, exprEnd);
                        Indent indent = Indent.get(doc);
                        indent.lock();
                        try {
                            indent.reindent(st.getStartOffset()+text.length()+1);
                        } finally {
                            indent.unlock();
                        }
                    }   catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            });
            if (comp != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            comp.setCaretPosition(changeInfo.get(0).getEnd().getOffset());
                            InstantRenamePerformer.invokeInstantRename(comp, changeInfo);
                        } catch (BadLocationException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                });
            }
            return changeInfo;
        }        
    }
    
    private static class CsmOffsetableImpl implements CsmOffsetable {

        private final CsmFile file;
        private final int selectionStart;
        private final int selectionEnd;
        private final String text;

        public CsmOffsetableImpl(CsmFile file, int selectionStart, int selectionEnd, String text) {
            this.file = file;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
            this.text = text;
        }

        @Override
        public CsmFile getContainingFile() {
            return file;
        }

        @Override
        public int getStartOffset() {
            return selectionStart;
        }

        @Override
        public int getEndOffset() {
            return selectionEnd;
        }

        @Override
        public CsmOffsetable.Position getStartPosition() {
            return new CsmOffsetable.Position() {

                @Override
                public int getOffset() {
                    return selectionStart;
                }

                @Override
                public int getLine() {
                    return CsmFileInfoQuery.getDefault().getLineColumnByOffset(file, selectionStart)[0];
                }

                @Override
                public int getColumn() {
                    return CsmFileInfoQuery.getDefault().getLineColumnByOffset(file, selectionStart)[1];
                }
            };
        }

        @Override
        public CsmOffsetable.Position getEndPosition() {
            return new CsmOffsetable.Position() {

                @Override
                public int getOffset() {
                    return selectionEnd;
                }

                @Override
                public int getLine() {
                    return CsmFileInfoQuery.getDefault().getLineColumnByOffset(file, selectionEnd)[0];
                }

                @Override
                public int getColumn() {
                    return CsmFileInfoQuery.getDefault().getLineColumnByOffset(file, selectionEnd)[1];
                }
            };
        }

        @Override
        public CharSequence getText() {
            return text;
        }
    }
    

}