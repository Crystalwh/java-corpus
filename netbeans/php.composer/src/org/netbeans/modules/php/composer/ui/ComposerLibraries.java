/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.composer.ui;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.api.executable.InvalidPhpExecutableException;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.composer.commands.Composer;
import org.netbeans.modules.php.composer.files.ComposerJson;
import org.netbeans.modules.php.composer.files.ComposerLock;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;

public final class ComposerLibraries {

    static final Logger LOGGER = Logger.getLogger(ComposerLibraries.class.getName());


    private ComposerLibraries() {
    }

    @NodeFactory.Registration(projectType = "org-netbeans-modules-php-project", position = 350)
    public static NodeFactory forPhpProject() {
        return new ComposerLibrariesNodeFactory();
    }

    //~ Inner classes

    private static final class ComposerLibrariesNodeFactory implements NodeFactory {

        @Override
        public NodeList<?> createNodes(Project project) {
            assert project != null;
            return new ComposerLibrariesNodeList(project);
        }

    }

    private static final class ComposerLibrariesNodeList implements NodeList<Node>, PropertyChangeListener {

        private final Project project;
        private final ComposerJson composerJson;
        private final ComposerLock composerLock;
        private final ComposerLibrariesChildren composerLibrariesChildren;
        private final ChangeSupport changeSupport = new ChangeSupport(this);

        // @GuardedBy("thread")
        private Node composerLibrariesNode;


        ComposerLibrariesNodeList(Project project) {
            assert project != null;
            this.project = project;
            FileObject composerDirectory = findComposerJsonDirectory();
            composerJson = new ComposerJson(composerDirectory);
            composerLock = new ComposerLock(composerDirectory);
            composerLibrariesChildren = new ComposerLibrariesChildren(composerJson, composerLock);
        }

        @Override
        public List<Node> keys() {
            if (!composerLibrariesChildren.hasDependencies()) {
                return Collections.<Node>emptyList();
            }
            if (composerLibrariesNode == null) {
                composerLibrariesNode = new ComposerLibrariesNode(composerLibrariesChildren);
            }
            return Collections.<Node>singletonList(composerLibrariesNode);
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            changeSupport.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            changeSupport.removeChangeListener(listener);
        }

        @Override
        public Node node(Node key) {
            return key;
        }

        @Override
        public void addNotify() {
            composerJson.addPropertyChangeListener(WeakListeners.propertyChange(this, composerJson));
            composerLock.addPropertyChangeListener(WeakListeners.propertyChange(this, composerLock));
        }

        @Override
        public void removeNotify() {
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (ComposerJson.PROP_REQUIRE.equals(propertyName)
                    || ComposerJson.PROP_REQUIRE_DEV.equals(propertyName)
                    || ComposerLock.PROP_PACKAGES.equals(propertyName)
                    || ComposerLock.PROP_PACKAGES_DEV.equals(propertyName)) {
                fireChange();
            }
        }

        private void fireChange() {
            composerLibrariesChildren.refreshDependencies();
            changeSupport.fireChange();
        }

        private FileObject findComposerJsonDirectory() {
            PhpModule phpModule = PhpModule.Factory.lookupPhpModule(project);
            assert phpModule != null : project.getClass().getName();
            try {
                FileObject file = Composer.getDefault().getComposerJson(phpModule);
                if (file != null) {
                    return file.getParent();
                }
            } catch (InvalidPhpExecutableException ex) {
                LOGGER.log(Level.INFO, null, ex);
            }
            return phpModule.getProjectDirectory();
        }

    }

    private static final class ComposerLibrariesNode extends AbstractNode {

        @StaticResource
        private static final String LIBRARIES_BADGE = "org/netbeans/modules/php/composer/ui/resources/libraries-badge.png"; // NOI18N

        private final Node iconDelegate;


        ComposerLibrariesNode(ComposerLibrariesChildren npmLibrariesChildren) {
            super(npmLibrariesChildren);
            iconDelegate = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
        }

        @NbBundle.Messages("ComposerLibrariesNode.name=Composer Libraries")
        @Override
        public String getDisplayName() {
            return Bundle.ComposerLibrariesNode_name();
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.mergeImages(iconDelegate.getIcon(type), ImageUtilities.loadImage(LIBRARIES_BADGE), 7, 7);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }

    }

    private static final class ComposerLibrariesChildren extends Children.Keys<ComposerLibraryInfo> {

        @StaticResource
        private static final String LIBRARIES_ICON = "org/netbeans/modules/php/composer/ui/resources/libraries.gif"; // NOI18N
        @StaticResource
        private static final String DEV_BADGE = "org/netbeans/modules/php/composer/ui/resources/libraries-dev-badge.gif"; // NOI18N


        private final ComposerJson composerJson;
        private final ComposerLock composerLock;
        private final java.util.Map<String, Image> icons = new HashMap<>();


        public ComposerLibrariesChildren(ComposerJson composerJson, ComposerLock composerLock) {
            super(true);
            assert composerJson != null;
            assert composerLock != null;
            this.composerJson = composerJson;
            this.composerLock = composerLock;
        }

        public boolean hasDependencies() {
            return !composerJson.getDependencies().isEmpty();
        }

        public void refreshDependencies() {
            setKeys();
        }

        @Override
        protected Node[] createNodes(ComposerLibraryInfo key) {
            return new Node[] {new NpmLibraryNode(key)};
        }

        @Override
        protected void addNotify() {
            setKeys();
        }

        @Override
        protected void removeNotify() {
            setKeys(Collections.<ComposerLibraryInfo>emptyList());
        }

        @NbBundle.Messages("ComposerLibrariesChildren.library.dev=dev")
        private void setKeys() {
            ComposerJson.ComposerDependencies dependencies = composerJson.getDependencies();
            if (dependencies.isEmpty()) {
                setKeys(Collections.<ComposerLibraryInfo>emptyList());
                return;
            }
            ComposerLock.ComposerPackages packages = composerLock.getPackages();
            List<ComposerLibraryInfo> keys = new ArrayList<>(dependencies.getCount());
            keys.addAll(getKeys(dependencies.dependencies, packages.packages, null, null));
            keys.addAll(getKeys(dependencies.devDependencies, packages.packagesDev, DEV_BADGE, Bundle.ComposerLibrariesChildren_library_dev()));
            setKeys(keys);
        }

        @NbBundle.Messages({
            "# {0} - library name",
            "# {1} - library version",
            "ComposerLibrariesChildren.description.version={0}: {1}",
            "# {0} - library name",
            "# {1} - library version",
            "# {2} - installed library version",
            "ComposerLibrariesChildren.description.versions={0}: {1} -> {2}",
            "# {0} - library description",
            "# {1} - library type",
            "ComposerLibrariesChildren.description.type={0} ({1})",
            "ComposerLibrariesChildren.na=n/a",
        })
        private List<ComposerLibraryInfo> getKeys(java.util.Map<String, String> dependencies, java.util.Map<String, String> packages,
                String badge, String libraryType) {
            if (dependencies.isEmpty()) {
                return Collections.emptyList();
            }
            List<ComposerLibraryInfo> keys = new ArrayList<>(dependencies.size());
            for (java.util.Map.Entry<String, String> entry : dependencies.entrySet()) {
                String description;
                String name = entry.getKey();
                String version = entry.getValue();
                String installedVersion = packages.get(name);
                if (installedVersion == null) {
                    // not installed
                    installedVersion = Bundle.ComposerLibrariesChildren_na();
                }
                if (Objects.equals(version, installedVersion)) {
                    description = Bundle.ComposerLibrariesChildren_description_version(name, version);
                } else {
                    description = Bundle.ComposerLibrariesChildren_description_versions(name, version, installedVersion);
                }
                if (libraryType != null) {
                    description = Bundle.ComposerLibrariesChildren_description_type(description, libraryType);
                }
                keys.add(new ComposerLibraryInfo(geIcon(badge), name, description));
            }
            Collections.sort(keys);
            return keys;
        }

        private Image geIcon(String badge) {
            Image icon = icons.get(badge);
            if (icon == null) {
                icon = ImageUtilities.loadImage(LIBRARIES_ICON);
                if (badge != null) {
                    icon = ImageUtilities.mergeImages(icon, ImageUtilities.loadImage(badge), 8, 8);
                }
                icons.put(badge, icon);
            }
            return icon;
        }

    }

    private static final class NpmLibraryNode extends AbstractNode {

        private final ComposerLibraryInfo libraryInfo;


        NpmLibraryNode(ComposerLibraryInfo libraryInfo) {
            super(Children.LEAF);
            this.libraryInfo = libraryInfo;
        }

        @Override
        public String getName() {
            return libraryInfo.name;
        }

        @Override
        public String getShortDescription() {
            return libraryInfo.description;
        }

        @Override
        public Image getIcon(int type) {
            return libraryInfo.icon;
        }

        @Override
        public Image getOpenedIcon(int type) {
            return libraryInfo.icon;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }

    }

    private static final class ComposerLibraryInfo implements Comparable<ComposerLibraryInfo> {

        final Image icon;
        final String name;
        final String description;


        ComposerLibraryInfo(Image icon, String name, String descrition) {
            assert icon != null;
            assert name != null;
            assert descrition != null;
            this.icon = icon;
            this.name = name;
            this.description = descrition;
        }

        @Override
        public int compareTo(ComposerLibraryInfo other) {
            return name.compareToIgnoreCase(other.name);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ComposerLibraryInfo other = (ComposerLibraryInfo) obj;
            return name.equalsIgnoreCase(other.name);
        }

    }

}
