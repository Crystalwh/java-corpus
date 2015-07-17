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
package org.netbeans.modules.collab.ui;

import java.awt.Dialog;
import java.util.*;
import javax.swing.event.*;

import org.openide.*;
import org.openide.util.NbBundle;



/**
 *
 * @author  sherylsu
 */
public class SelectConferenceForm extends javax.swing.JPanel implements ListSelectionListener {
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JList jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel matchesFoundLabel;
    // End of variables declaration//GEN-END:variables
    private String[] conversations;
    private DialogDescriptor descriptor;

    /** Creates new form SelectUser */
    public SelectConferenceForm(String[] conversations) {
        this.conversations = conversations;

        descriptor = new DialogDescriptor(
                this, NbBundle.getMessage(SelectConferenceForm.class, "TITLE_SelectConferenceForm")
            ); // NOI18N
        descriptor.setValid(false);

        initComponents();

        Vector v = new Vector();
        v.addAll(Arrays.asList(conversations));

        // TODO: TAF: We need Conversation to implement Comparator
        //		Collections.sort(v);
        ListModel model = new ListModel(jList1, v, false, true, false);

        ListRenderer renderer = new ListRenderer(model);
        jList1.setCellRenderer(renderer);
        jList1.setModel(model);
        jList1.addListSelectionListener(this);
    }

    public String[] getSelectedConversations() {
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);

        try {
            dialog.show();

            if (descriptor.getValue() == DialogDescriptor.OK_OPTION) {
                int[] indices = jList1.getSelectedIndices();
                String[] result = new String[indices.length];

                for (int i = 0; i < indices.length; i++) {
                    int j = indices[i];
                    result[i] = conversations[j];
                }

                return result;
            } else {
                return null;
            }
        } finally {
            dialog.dispose();
        }
    }

    /**
     *
     *
     */
    protected void updateValidStatus() {
        descriptor.setValid(jList1.getModel().getSize() != 0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        matchesFoundLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(matchesFoundLabel, org.openide.util.NbBundle.getMessage(SelectConferenceForm.class, "LBL_SelectConferenceForm_Num_Conf", new Object[] {new Integer(conversations.length)})); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        add(matchesFoundLabel, gridBagConstraints);

        jLabel2.setLabelFor(jList1);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SelectConferenceForm.class, "LBL_SelectConferenceForm_PleaseSelect")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        add(jLabel2, gridBagConstraints);

        jList1.setPreferredSize(null);
        jScrollPane1.setViewportView(jList1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        add(jScrollPane1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    public void valueChanged(ListSelectionEvent e) {
        updateValidStatus();
    }
}