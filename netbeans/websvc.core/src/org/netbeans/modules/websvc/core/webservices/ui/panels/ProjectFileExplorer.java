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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.websvc.core.webservices.ui.panels;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.tree.TreeSelectionModel;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;

import org.openide.DialogDescriptor;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 *
 * @author rico
 */
public class ProjectFileExplorer extends JPanel implements ExplorerManager.Provider, PropertyChangeListener {

    private DialogDescriptor descriptor;
    private ExplorerManager manager;
    private BeanTreeView treeView;
    private DataObject selectedFolder;
    private Project[] projects;
    private Children rootChildren;
    private Node explorerClientRoot;
    private List<Node> projectNodeList;

    public ProjectFileExplorer() {
        projects = OpenProjects.getDefault().getOpenProjects();
        rootChildren = new Children.Array();
        explorerClientRoot = new AbstractNode(rootChildren);
        projectNodeList = new ArrayList<Node>();
        manager = new ExplorerManager();

        initComponents();
        initUserComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        dontCopyCB = new javax.swing.JCheckBox();
        jLblTreeView = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(dontCopyCB, org.openide.util.NbBundle.getMessage(ProjectFileExplorer.class, "TXT_DONOTCOPY")); // NOI18N
        dontCopyCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dontCopyCBActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 0, 11);
        add(dontCopyCB, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jLblTreeView, org.openide.util.NbBundle.getMessage(ProjectFileExplorer.class, "LBL_SelectProjectLocation")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 0, 11);
        add(jLblTreeView, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

private String getTreeViewLabel(boolean dontCopy){
    if(dontCopy){
        return NbBundle.getMessage(ProjectFileExplorer.class, "TXT_DONOTCOPY_TOOLTIP");
    }
    return NbBundle.getMessage(ProjectFileExplorer.class, "LBL_SelectProjectLocation");
}

private void dontCopyCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dontCopyCBActionPerformed
if(dontCopyCB.isSelected()){
    descriptor.setValid(true);
    treeView.setEnabled(false);
    jLblTreeView.setText(getTreeViewLabel(true));
}else if (getSelectedFile() == null){
    descriptor.setValid(false);
    treeView.setEnabled(true);
    jLblTreeView.setText(getTreeViewLabel(false));
}
else{
    descriptor.setValid(true);
    treeView.setEnabled(true);
    jLblTreeView.setText(getTreeViewLabel(false));
}
}//GEN-LAST:event_dontCopyCBActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox dontCopyCB;
    private javax.swing.JLabel jLblTreeView;
    // End of variables declaration//GEN-END:variables
    private void initUserComponents() {
        treeView = new BeanTreeView();
        treeView.setRootVisible(false);
        treeView.setPopupAllowed(false);
        treeView.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(11, 11, 0, 11);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(treeView, gridBagConstraints);
        jLblTreeView.setLabelFor(treeView.getViewport().getView());
        treeView.getAccessibleContext().setAccessibleName(NbBundle.getMessage(ClientExplorerPanel.class, "ACSD_AvailableWebServicesTree"));
        treeView.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(ClientExplorerPanel.class, "ACSD_AvailableWebServicesTree"));
        dontCopyCB.setToolTipText(NbBundle.getMessage(ProjectFileExplorer.class, "TXT_DONOTCOPY_TOOLTIP"));
    }

    public ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        manager.addPropertyChangeListener(this);
        for (int i = 0; i < projects.length; i++) {
            try {
                Project project = projects[i];
                FileObject projectDir = project.getProjectDirectory();
                DataObject projectDirDObj = DataObject.find(projectDir);
                Node rootNode = projectDirDObj.getNodeDelegate();
                FilterNode node = new FilterNode(rootNode);
                projectNodeList.add(node);

            } catch (DataObjectNotFoundException ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
        Node[] projectNodes = new Node[projectNodeList.size()];
        projectNodeList.<Node>toArray(projectNodes);
        rootChildren.add(projectNodes);
        manager.setRootContext(explorerClientRoot);

        descriptor.setValid(false);
    }

    public void removeNotify() {
        manager.removePropertyChangeListener(this);
        super.removeNotify();
    }

    public void setDescriptor(DialogDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public boolean dontCopy(){
        return dontCopyCB.isSelected();
    }
     public DataObject getSelectedFolder() {
          return selectedFolder;
      }
     
     private DataObject getSelectedFile(){
         Node nodes[] = manager.getSelectedNodes();
         if(nodes != null && nodes.length > 0){
             Node node = nodes[0];
             DataObject dobj = node.getLookup().lookup(DataObject.class);
             if(dobj != null){
                 if(dobj.getPrimaryFile().isFolder()){
                     return dobj;
                 }
             }
         }
         return null;
     }
     
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == manager) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                DataObject folder = getSelectedFile();
                if(folder != null){
                    selectedFolder = folder;
                    descriptor.setValid(true);
                }else{
                    selectedFolder = null;
                    descriptor.setValid(false);
                }
            }
        }
    }
}