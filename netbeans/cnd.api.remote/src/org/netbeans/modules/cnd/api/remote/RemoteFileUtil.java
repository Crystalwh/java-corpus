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

package org.netbeans.modules.cnd.api.remote;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cnd.utils.CndPathUtilities;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.netbeans.modules.cnd.utils.ui.FileChooser;
import org.netbeans.modules.dlight.libs.common.InvalidFileObjectSupport;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.remote.api.ui.FileChooserBuilder;
import org.netbeans.modules.remote.api.ui.FileChooserBuilder.JFileChooserEx;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Vladimir Kvashin
 */
public class RemoteFileUtil {

    /**
     * Checks whether file exists or not
     * @param absolutePath - should be ABSOLUTE, but not necessarily normalized
     */
    public static boolean fileExists(String absolutePath, ExecutionEnvironment executionEnvironment) {
        FileObject fo = getFileObject(normalizeAbsolutePath(absolutePath, executionEnvironment), executionEnvironment);
        return (fo != null && fo.isValid());
    }

    public static boolean isDirectory(String absolutePath, ExecutionEnvironment executionEnvironment) {
        FileObject fo = getFileObject(absolutePath, executionEnvironment);
        return (fo != null && fo.isFolder());
    }

    /**
     * In many places, standard sequence is as follows:
     *  - convert path to absolute if need
     *  - normalize it
     *  - find file object
     * In the case of non-local file systems we should delegate it to correspondent file systems.
     */
    public static FileObject getFileObject(FileObject baseFileObject, String relativeOrAbsolutePath) {
        FileObject result = FileSystemProvider.getFileObject(baseFileObject, relativeOrAbsolutePath);
        if (result == null) {
            String absRootPath = CndPathUtilities.toAbsolutePath(baseFileObject, relativeOrAbsolutePath);
            try {
                // XXX:fullRemote we use old logic for local and new for remote
                // but remote approach for local gives #197093 -  Exception: null file
                final FileSystem fs = baseFileObject.getFileSystem();
                if (CndFileUtils.isLocalFileSystem(fs)) {
                    result = CndFileUtils.toFileObject(CndFileUtils.normalizeAbsolutePath(absRootPath));
                } else {
                    result = InvalidFileObjectSupport.getInvalidFileObject(fs, absRootPath);
                }
            } catch (FileStateInvalidException ex) {
                Exceptions.printStackTrace(ex);
                result = InvalidFileObjectSupport.getInvalidFileObject(InvalidFileObjectSupport.getDummyFileSystem(), absRootPath);
            }
        }
        return result;
    }

    private RemoteFileUtil() {}

    public static FileObject getFileObject(String absolutePath, ExecutionEnvironment execEnv) {
        CndUtils.assertAbsolutePathInConsole(absolutePath, "path for must be absolute"); //NOI18N
        if (execEnv.isRemote()) {
            if (CndUtils.isDebugMode()) {
                String normalizedPath = normalizeAbsolutePath(absolutePath, execEnv);
                if (! normalizedPath.equals(absolutePath)) {
                    CndUtils.assertTrueInConsole(false, "Warning: path is not normalized:  absolute path is _" + absolutePath + "_ normailzed path is _"  + normalizedPath + "_");
                }
                //absolutePath = normalizedPath;
            }
            return FileSystemProvider.getFileSystem(execEnv).findResource(absolutePath); //NOI18N
        } else {
            return CndFileUtils.toFileObject(absolutePath);
        }
    }

    public static FileSystem getProjectSourceFileSystem(Lookup.Provider project) {
        if (project != null) {
            RemoteProject rp = project.getLookup().lookup(RemoteProject.class);
            if (rp == null) {
                return null;
            }
            FileObject projectDir = rp.getSourceBaseDirFileObject();
            if (projectDir != null) {
                try {
                return projectDir.getFileSystem();
                } catch (FileStateInvalidException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return CndFileUtils.getLocalFileSystem();
    }
    
    public static FileObject getProjectSourceBaseFileObject(Lookup.Provider project) {
        if (project != null) {
            RemoteProject rp = project.getLookup().lookup(RemoteProject.class);
            if (rp == null) {
                return null;
            }
            return rp.getSourceBaseDirFileObject();
        }
        return null;
    }

    public static ExecutionEnvironment getProjectSourceExecutionEnvironment(Project project) {
        if (project != null) {
            RemoteProject remoteProject = project.getLookup().lookup(RemoteProject.class);
            if (remoteProject != null) {
                return remoteProject.getSourceFileSystemHost();
            }
        }
        return ExecutionEnvironmentFactory.getLocal();
    }

    // it should take not-normalized path ok, since the caller can not normalize
    // because it does not know execution environment
    public static FileObject getFileObject(String absolutePath, Project project) {
        ExecutionEnvironment execEnv = getProjectSourceExecutionEnvironment(project);
        absolutePath = FileSystemProvider.normalizeAbsolutePath(absolutePath, execEnv);
        if (execEnv != null && execEnv.isRemote()) {
            return getFileObject(absolutePath, execEnv);
        }
        FileObject projectDir = project.getProjectDirectory();
        CndUtils.assertNotNull(projectDir, "Null project dir for ", project); //NOI18N
        final FileSystem fs;
        try {
            fs = projectDir.getFileSystem();            
            return fs.findResource(absolutePath);
        } catch (FileStateInvalidException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    public static String normalizeAbsolutePath(String absPath, Project project) {
        ExecutionEnvironment execEnv = getProjectSourceExecutionEnvironment(project);
        if (execEnv != null && execEnv.isRemote()) {
            return normalizeAbsolutePath(absPath, execEnv);
        } else {
            return CndFileUtils.normalizeAbsolutePath(absPath);
        }

    }

    public static String normalizeAbsolutePath(String absPath, ExecutionEnvironment execEnv) {
        if (execEnv.isRemote()) {
            return FileSystemProvider.normalizeAbsolutePath(absPath, execEnv);
        } else {
            return FileUtil.normalizePath(absPath);
        }
    }

    public static String getAbsolutePath(FileObject fileObject) {
        return fileObject.getPath();
    }

    public static String getCanonicalPath(FileObject fo) throws IOException {
        //XXX:fullRemote
        if (FileSystemProvider.getExecutionEnvironment(fo).isLocal()) {
            File file = FileUtil.toFile(fo);
            return (file == null) ? fo.getPath() : file.getCanonicalPath();
        } else {
            FileObject file = FileSystemProvider.getCanonicalFileObject(fo);
            return (file == null) ? fo.getPath() : file.getPath();
        }
    }

    public static boolean isRemote(FileSystem fs) {
        if (fs != null) {
            ExecutionEnvironment env = FileSystemProvider.getExecutionEnvironment(fs);
            return (env == null) ? false : env.isRemote();
        }
        return false;
    }
    
    public static JFileChooser createFileChooser(FileSystem fs,
            String titleText, String buttonText, int mode, FileFilter[] filters,
            String initialPath, boolean useParent) {
        ExecutionEnvironment env = FileSystemProvider.getExecutionEnvironment(fs);
        return createFileChooser(env, titleText, buttonText, mode, filters, initialPath, useParent);
    }
    
    public static JFileChooser createFileChooser(ExecutionEnvironment execEnv,
            String titleText, String buttonText, int mode, FileFilter[] filters,
            String initialPath, boolean useParent) {


        // TODO support useParent or rework it
        final FileChooserBuilder fileChooserBuilder = new FileChooserBuilder(execEnv).setPreferences(NbPreferences.forModule(RemoteFileUtil.class));
        JFileChooserEx fileChooser = fileChooserBuilder.createFileChooser(initialPath);
        fileChooser.setApproveButtonText(buttonText);
        fileChooser.setDialogTitle(titleText);
        fileChooser.setFileSelectionMode(mode);
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                fileChooser.addChoosableFileFilter(filters[i]);
            }
            fileChooser.setFileFilter(filters[0]);
        }
        
        return fileChooser;
    }
    
    /**
     * Use this method when your initial path calculation can take a long time
     * @param execEnv
     * @param titleText
     * @param buttonText
     * @param mode
     * @param filters
     * @param initialPath callable which will be invoked in separate RP *after* dialog will be shown to the user
     * @param useParent
     * @return 
     */
    public static JFileChooser createFileChooser(ExecutionEnvironment execEnv,
            String titleText, String buttonText, int mode, FileFilter[] filters,
            Callable<String> initialPath, boolean useParent) {


        // TODO support useParent or rework it
        final FileChooserBuilder fileChooserBuilder = new FileChooserBuilder(execEnv).setPreferences(NbPreferences.forModule(RemoteFileUtil.class));
        JFileChooserEx fileChooser = fileChooserBuilder.createFileChooser(initialPath);
        fileChooser.setApproveButtonText(buttonText);
        fileChooser.setDialogTitle(titleText);
        fileChooser.setFileSelectionMode(mode);
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                fileChooser.addChoosableFileFilter(filters[i]);
            }
            fileChooser.setFileFilter(filters[0]);
        }
        
        return fileChooser;
    }

    public static JFileChooser createProjectChooser(ExecutionEnvironment execEnv,
            String titleText, String description, String buttonText, String initialPath) {
        JFileChooser fileChooser;
//        if (execEnv.isLocal()) {
//            fileChooser = ProjectChooser.projectChooser();
//            fileChooser.getAccessibleContext().setAccessibleDescription(description);
//            fileChooser.setDialogTitle(titleText);
//            fileChooser.setApproveButtonText(buttonText);
//            if (initialPath != null) {
//                fileChooser.setCurrentDirectory(new File(initialPath));
//            }
//        } else {
            fileChooser = (JFileChooserEx) createFileChooser(execEnv,
                          titleText, buttonText, JFileChooser.DIRECTORIES_ONLY, null, initialPath, true);
            fileChooser.setFileView(new ProjectSelectionFileView(fileChooser));
        //}
        return fileChooser;
        
    }

    /**
     * Returns the folder last used for creating a new project.
     * @return File the folder
     */
    public static String getProjectsFolder(ExecutionEnvironment env) {
        Preferences pref = NbPreferences.forModule(RemoteFileUtil.class);
        String envID = ExecutionEnvironmentFactory.toUniqueID(env);
        return pref.get("ProjectPath"+envID, null); // NOI18N
    }
    /**
     * Sets the folder last used for creating a new project.
     * @param folder The folder to be set as last used. Must not be null
     */
    public static void setProjectsFolder(String folder, ExecutionEnvironment env) {
        Preferences pref = NbPreferences.forModule(RemoteFileUtil.class);
        String envID = ExecutionEnvironmentFactory.toUniqueID(env);
        pref.put("ProjectPath"+envID, folder); // NOI18N
    }
    
    /**
     * Returns the folder last used for selecting files.
     * FileChooserBuilder.RemoteFileChooserImpl.
     * @param key
     * @param env
     * @return File the path to last selected file.
     */
    public static String getCurrentChooserFile(String key, ExecutionEnvironment env) {
        if (env.isLocal()) {
            if (FileChooser.getCurrentChooserFile() != null) {
                return FileChooser.getCurrentChooserFile().getPath();
            }
            return null;
        } else {
            Preferences pref = NbPreferences.forModule(RemoteFileUtil.class);
            String envID = ExecutionEnvironmentFactory.toUniqueID(env);
            return pref.get("FileChooserPath"+envID + key, null); // NOI18N
        }
    }    
    /**
     * Sets the folder last used for creating a new project.
     * @param key
     * @param path
     * @param env
     */
    public static void setCurrentChooserFile(String key, String path, ExecutionEnvironment env) {
        if (path == null) {
            return;
        }
        if (env.isLocal()) {
            FileChooser.setCurrentChooserFile(new File(path));
        } else {
            Preferences pref = NbPreferences.forModule(RemoteFileUtil.class);
            String envID = ExecutionEnvironmentFactory.toUniqueID(env);
            pref.put("FileChooserPath"+envID + key, path); // NOI18N
        }
    }    

    /**
     * Returns the folder last used for selecting files.
     * FileChooserBuilder.RemoteFileChooserImpl.
     * @param env
     * @return File the path to last selected file.
     */
    public static String getCurrentChooserFile(ExecutionEnvironment env) {
        if (env.isLocal()) {
            if (FileChooser.getCurrentChooserFile() != null) {
                return FileChooser.getCurrentChooserFile().getPath();
            }
            return null;
        } else {
            Preferences pref = NbPreferences.forModule(RemoteFileUtil.class);
            String envID = ExecutionEnvironmentFactory.toUniqueID(env);
            return pref.get("FileChooserPath"+envID, null); // NOI18N
        }
    }
                
    /**
     * Sets the folder last used for creating a new project.
     * @param path
     * @param env
     */
    public static void setCurrentChooserFile(String path, ExecutionEnvironment env) {
        if (path == null) {
            return;
        }
        if (env.isLocal()) {
            FileChooser.setCurrentChooserFile(new File(path));
        } else {
            Preferences pref = NbPreferences.forModule(RemoteFileUtil.class);
            String envID = ExecutionEnvironmentFactory.toUniqueID(env);
            pref.put("FileChooserPath"+envID, path); // NOI18N
        }
    }

    private static final class ProjectSelectionFileView extends FileView implements Runnable {

        private final JFileChooser chooser;
        private final Map<File, Icon> knownProjectIcons = new HashMap<File, Icon>();
        private final RequestProcessor.Task task = new RequestProcessor("ProjectIconFileView").create(this);//NOI18N
        private File lookingForIcon;

        public ProjectSelectionFileView(JFileChooser chooser) {
            this.chooser = chooser;
        }

        @Override
        public Icon getIcon(File f) {
            if (f.isDirectory() && // #173958: do not call ProjectManager.isProject now, could block
                    !f.toString().matches("/[^/]+") && // Unix: /net, /proc, etc. //NOI18N
                    f.getParentFile() != null) { // do not consider drive roots
                synchronized (this) {
                    Icon icon = knownProjectIcons.get(f);
                    if (icon != null) {
                        return icon;
                    } else if (lookingForIcon == null) {
                        lookingForIcon = f;
                        task.schedule(20);
                        // Only calculate one at a time.
                        // When the view refreshes, the next unknown icon
                        // should trigger the task to be reloaded.
                    }
                }
            }
            return chooser.getFileSystemView().getSystemIcon(f);
        }

        @Override
        public void run() {
            String path = lookingForIcon.getAbsolutePath();
            String project = path + "/nbproject"; // NOI18N
            File projectDir = chooser.getFileSystemView().createFileObject(project);
            Icon icon = chooser.getFileSystemView().getSystemIcon(lookingForIcon);
            if (projectDir.exists() && projectDir.isDirectory() && projectDir.canRead()) {
                String projectXml = path + "/nbproject/project.xml"; // NOI18N
                File projectFile = chooser.getFileSystemView().createFileObject(projectXml);
                if (projectFile.exists()) {
                    String conf = path + "/nbproject/configurations.xml"; // NOI18N
                    File configuration = chooser.getFileSystemView().createFileObject(conf);
                    if (configuration.exists()) {
                        icon = ImageUtilities.loadImageIcon("org/netbeans/modules/cnd/makeproject/ui/resources/makeProject.gif", true); // NOI18N
                    }
                }
            }
            synchronized (this) {
                knownProjectIcons.put(lookingForIcon, icon);
                lookingForIcon = null;
            }
            chooser.repaint();
        }
    }

}
