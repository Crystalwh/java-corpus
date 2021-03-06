/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.subversion.remote.ui.update;

import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.netbeans.modules.subversion.remote.FileInformation;
import org.netbeans.modules.subversion.remote.FileStatusCache;
import org.netbeans.modules.subversion.remote.RepositoryFile;
import org.netbeans.modules.subversion.remote.Subversion;
import org.netbeans.modules.subversion.remote.api.ISVNInfo;
import org.netbeans.modules.subversion.remote.api.ISVNStatus;
import org.netbeans.modules.subversion.remote.api.SVNClientException;
import org.netbeans.modules.subversion.remote.api.SVNRevision;
import org.netbeans.modules.subversion.remote.api.SVNUrl;
import org.netbeans.modules.subversion.remote.client.SvnClient;
import org.netbeans.modules.subversion.remote.client.SvnClientExceptionHandler;
import org.netbeans.modules.subversion.remote.client.SvnProgressSupport;
import org.netbeans.modules.subversion.remote.ui.actions.ContextAction;
import org.netbeans.modules.subversion.remote.util.Context;
import org.netbeans.modules.subversion.remote.util.SvnUtils;
import org.netbeans.modules.remotefs.versioning.api.VCSFileProxySupport;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;

/**
 * Reverts local changes.
 *
 * @author Petr Kuzel
 */
public class RevertModificationsAction extends ContextAction {
    private static final String ICON_RESOURCE = "org/netbeans/modules/subversion/remote/resources/icons/get_clean.png"; //NOI18N
    
    public RevertModificationsAction () {
        super(ICON_RESOURCE);
    }
    
    @Override
    protected String getBaseName(Node[] activatedNodes) {
        return "CTL_MenuItem_Revert"; // NOI18N
    }
    
    @Override
    protected int getFileEnabledStatus() {
        return FileInformation.STATUS_VERSIONED & ~FileInformation.STATUS_VERSIONED_NEWINREPOSITORY;
    }
    
    @Override
    protected int getDirectoryEnabledStatus() {
        return FileInformation.STATUS_VERSIONED & ~FileInformation.STATUS_VERSIONED_NEWINREPOSITORY;
    }

    @Override
    protected String iconResource () {
        return ICON_RESOURCE;
    }
    
    @Override
    protected void performContextAction(final Node[] nodes) {
        final Context ctx = getContext(nodes);
        if(!Subversion.getInstance().checkClientAvailable(ctx)) {
            return;
        }
        VCSFileProxy[] roots = ctx.getRootFiles();
        // filter managed roots
        List<VCSFileProxy> l = new ArrayList<>();
        for (VCSFileProxy file : roots) {
            if(SvnUtils.isManaged(file)) {
                l.add(file);
            }
        }
        roots = l.toArray(new VCSFileProxy[l.size()]);

        if(roots == null || roots.length == 0) {
            return;
        }

        VCSFileProxy interestingFile;
        if(roots.length == 1) {
            interestingFile = roots[0];
        } else {
            interestingFile = SvnUtils.getPrimaryFile(roots[0]);
        }

        final SVNUrl rootUrl;
        final SVNUrl url;
        
        try {
            rootUrl = ContextAction.getSvnUrl(ctx);
            url = SvnUtils.getRepositoryUrl(interestingFile);
        } catch (SVNClientException ex) {
            SvnClientExceptionHandler.notifyException(ctx, ex, true, true);
            return;
        }
        final RepositoryFile repositoryFile = new RepositoryFile(ctx.getFileSystem(), rootUrl, url, SVNRevision.HEAD);
        
        final RevertModifications revertModifications = new RevertModifications(repositoryFile);
        if(!revertModifications.showDialog()) {
            return;
        }
        
        ContextAction.ProgressSupport support = new ContextAction.ProgressSupport(this, nodes, ctx) {
            @Override
            public void perform() {
                performRevert(revertModifications.getRevisionInterval(), revertModifications.revertNewFiles(), !revertModifications.revertRecursively(), ctx, this);
            }
        };
        support.start(createRequestProcessor(ctx));
    }
    
    /**
     * Reverts given files
     * @param revisions
     * @param revertNewFiles
     * @param onlySelectedFiles if set to false then the revert will act recursively, otherwise only selected roots will be reverted (without any of their children)
     * @param ctx
     * @param support 
     */
    public static void performRevert(final RevertModifications.RevisionInterval revisions, boolean revertNewFiles, final boolean onlySelectedFiles, final Context ctx, final SvnProgressSupport support) {
        final SvnClient client;
        try {
            client = Subversion.getInstance().getClient(ctx, support);
        } catch (SVNClientException ex) {
            SvnClientExceptionHandler.notifyException(ctx, ex, true, true);
            return;
        }
        
        VCSFileProxy files[] = ctx.getFiles();
        final VCSFileProxy[][] split;
        if (onlySelectedFiles) {
            split = new VCSFileProxy[2][0];
        } else {
            split = VCSFileProxySupport.splitFlatOthers(files);
        }
        try {
            SvnUtils.runWithoutIndexing(new Callable<Void>() {

                @Override
                public Void call () throws Exception {
                    for (int c = 0; c<split.length; c++) {
                        if(support.isCanceled()) {
                            return null;
                        }
                        VCSFileProxy[] files = split[c];
                        boolean recursive = c == 1;
                        if (!recursive && revisions == null) {
                            // not recursively
                            if (onlySelectedFiles) {
                                // ONLY the selected files, no children
                                files = ctx.getFiles();
                            } else {
                                // get selected files and it's direct descendants for flat folders
                                files = SvnUtils.flatten(files, FileInformation.STATUS_REVERTIBLE_CHANGE);
                            }
                        }

                        try {
                            if(revisions != null) {
                                for (int i= 0; i < files.length; i++) {
                                    if(support.isCanceled()) {
                                        return null;
                                    }
                                    SVNUrl url = SvnUtils.getRepositoryUrl(files[i]);
                                    RevertModifications.RevisionInterval targetInterval = recountStartRevision(ctx, client, url, revisions);
                                    if(files[i].exists()) {
                                        client.merge(url, targetInterval.endRevision,
                                                     url, targetInterval.startRevision,
                                                     files[i], false, recursive);
                                    } else {
                                        assert targetInterval.startRevision instanceof SVNRevision.Number
                                               : "The revision has to be a Number when trying to undelete file!"; //NOI18N
                                        client.copy(url, files[i], targetInterval.startRevision);
                                    }
                                }
                            } else {
                                if(support.isCanceled()) {
                                    return null;
                                }
                                if(files.length > 0 ) {                        
                                    // check for deleted files, we also want to undelete their parents
                                    Set<VCSFileProxy> deletedFiles = new HashSet<>();
                                    for(VCSFileProxy file : files) {
                                        deletedFiles.addAll(getDeletedParents(file));
                                    }
                                    
                                    handleCopiedFiles(client, files, recursive);

                                    // XXX JAVAHL client.revert(files, recursive);
                                    for (VCSFileProxy file : files) {
                                        client.revert(file, recursive);
                                    }

                                    // revert also deleted parent folders
                                    // for all undeleted files
                                    if(deletedFiles.size() > 0) {
                                        // XXX JAVAHL client.revert(deletedFiles.toArray(new File[deletedFiles.size()]), false);
                                        for (VCSFileProxy file : deletedFiles) {
                                            client.revert(file, false);
                                        }    
                                    }
                                }
                            }
                        } catch (SVNClientException ex) {
                            support.annotate(ex);
                        }
                    }
                    return null;
                }

                private void handleCopiedFiles (SvnClient client, VCSFileProxy[] files, boolean recursively) {
                    FileStatusCache cache = Subversion.getInstance().getStatusCache();
                    if (recursively) {
                        files = cache.listFiles(files, FileInformation.STATUS_VERSIONED_ADDEDLOCALLY);
                    }
                    for (VCSFileProxy f : files) {
                        FileInformation fi = cache.getStatus(f);
                        if (fi.getStatus() == FileInformation.STATUS_VERSIONED_ADDEDLOCALLY) {
                            ISVNStatus entry = fi.getEntry(f);
                            if (entry.isCopied()) {
                                // file exists but it's status is set to deleted
                                VCSFileProxy temporary = VCSFileProxySupport.generateTemporaryFile(f.getParentFile(), f.getName());
                                try {
                                    if (VCSFileProxySupport.renameTo(f, temporary)) {
                                        client.remove(new VCSFileProxy[] { f }, true);
                                    } else {
                                        Subversion.LOG.log(Level.WARNING, "RevertModifications.handleCopiedFiles: cannot rename {0} to {1}", new Object[] { f, temporary }); //NOI18N
                                    }
                                } catch (SVNClientException ex) {
                                    Subversion.LOG.log(Level.INFO, null, ex);
                                } finally {
                                    if (temporary.exists()) {
                                        try {
                                            if (!VCSFileProxySupport.renameTo(temporary, f)) {
                                                VCSFileProxySupport.copyFile(temporary, f);
                                            }
                                        } catch (IOException ex) {
                                            Subversion.LOG.log(Level.INFO, "RevertModifications.handleCopiedFiles: cannot copy {0} back to {1}", new Object[] { temporary, f }); //NOI18N
                                        } finally {
                                            VCSFileProxySupport.delete(temporary);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
            }, files);
        } catch (SVNClientException ex) {
            SvnClientExceptionHandler.notifyException(ctx, ex, true, false);
        }
        
        if(support.isCanceled()) {
            return;
        }
        
        FileStatusCache cache = Subversion.getInstance().getStatusCache();
        for (VCSFileProxy file : cache.listFiles(ctx, FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY | FileInformation.STATUS_VERSIONED_DELETEDLOCALLY | FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
            FileInformation fi;
            if (file.isDirectory() 
                    || (fi = cache.getCachedStatus(file)) != null && (fi.getStatus() & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY) != 0) { // added files turned to not versioned
                cache.refresh(file, null);
            }
        }
        
        if(support.isCanceled()) {
            return;
        }

        if(revertNewFiles) {
            VCSFileProxy[] newfiles = Subversion.getInstance().getStatusCache().listFiles(ctx.getRootFiles(), FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY | FileInformation.STATUS_VERSIONED_ADDEDLOCALLY);
            for (VCSFileProxy file : newfiles) {
                // do not act recursively if not allowed
                if (!onlySelectedFiles || ctx.getRoots().contains(file)) {
                    FileObject fo = file.toFileObject();
                    try {
                        if(fo != null) {
                            fo.delete();
                        }
                    } catch (IOException ex) {
                        Subversion.LOG.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }     

    private static List<VCSFileProxy> getDeletedParents(VCSFileProxy file) {
        List<VCSFileProxy> ret = new ArrayList<>();
        for(VCSFileProxy parent = file.getParentFile(); parent != null; parent = parent.getParentFile()) {        
            FileInformation info = Subversion.getInstance().getStatusCache().getStatus(parent);
            if( !((info.getStatus() & FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY) != 0 ||
                  (info.getStatus() & FileInformation.STATUS_VERSIONED_DELETEDLOCALLY) != 0) )  
            {
                return ret;
            }
            ret.add(parent);                                
        }        
        return ret;
    }
    
    private static RevertModifications.RevisionInterval recountStartRevision(Context context, SvnClient client, SVNUrl repository, RevertModifications.RevisionInterval ret) throws SVNClientException {
        SVNRevision currStartRevision = ret.startRevision;
        SVNRevision currEndRevision = ret.endRevision;

        if(currStartRevision.equals(SVNRevision.HEAD)) {
            ISVNInfo info = client.getInfo(context, repository);
            currStartRevision = info.getRevision();
        }

        long currStartRevNum = Long.parseLong(currStartRevision.toString());
        long newStartRevNum = (currStartRevNum > 0) ? currStartRevNum - 1
                                                    : currStartRevNum;

        return new RevertModifications.RevisionInterval(
                                         new SVNRevision.Number(newStartRevNum),
                                         currEndRevision);
    }

}
