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

package org.netbeans.modules.subversion.ui.copy;

import org.netbeans.modules.subversion.ui.wizards.*;

/**
 *
 * @author  Petr Kuzel
 */
public class MergePanel extends javax.swing.JPanel {

    /** Creates new form WorkdirPanel */
    public MergePanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        typeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setLabelFor(typeComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MergePanel.class, "CTL_MergePanel_MergeFrom")); // NOI18N

        typeDescriptionLabel.setLabelFor(mergeFieldsPanel);
        org.openide.awt.Mnemonics.setLocalizedText(typeDescriptionLabel, "jLabel2"); // NOI18N

        mergeFieldsPanel.setMinimumSize(new java.awt.Dimension(350, 500));

        javax.swing.GroupLayout mergeFieldsPanelLayout = new javax.swing.GroupLayout(mergeFieldsPanel);
        mergeFieldsPanel.setLayout(mergeFieldsPanelLayout);
        mergeFieldsPanelLayout.setHorizontalGroup(
            mergeFieldsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 806, Short.MAX_VALUE)
        );
        mergeFieldsPanelLayout.setVerticalGroup(
            mergeFieldsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 806, Short.MAX_VALUE)
        );
        previewPanelLayout.setVerticalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 73, Short.MAX_VALUE)
        );

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(MergePanel.class, "CTL_MergePanel_Preview")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mergeFieldsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(typeDescriptionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(typeComboBox, 0, 704, Short.MAX_VALUE))
                    .addComponent(previewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(25, 25, 25)
                .addComponent(typeDescriptionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mergeFieldsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 245, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(previewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        typeComboBox.getAccessibleContext().setAccessibleDescription("Merge From Repository URL");
        jLabel1.getAccessibleContext().setAccessibleDescription("Merge From Repository URL");
        typeDescriptionLabel.getAccessibleContext().setAccessibleDescription("Merge desired selection");
        jLabel2.getAccessibleContext().setAccessibleDescription("Preview Figures for Chosen Action");
    }// </editor-fold>//GEN-END:initComponents

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        
    }//GEN-LAST:event_typeComboBoxActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    final javax.swing.JPanel mergeFieldsPanel = new javax.swing.JPanel();
    final javax.swing.JPanel previewPanel = new javax.swing.JPanel();
    final javax.swing.JComboBox typeComboBox = new javax.swing.JComboBox();
    final javax.swing.JLabel typeDescriptionLabel = new javax.swing.JLabel();
    // End of variables declaration//GEN-END:variables
    
}