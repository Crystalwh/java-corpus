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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.model.License;
import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.api.FileUtilities;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.PluginPropertyUtils;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.CreateFromTemplateAttributesProvider;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author mkleint
 */
@ProjectServiceProvider(service=CreateFromTemplateAttributesProvider.class, projectType="org-netbeans-modules-maven")
public class TemplateAttrProvider implements CreateFromTemplateAttributesProvider {
    private static final Logger LOG = Logger.getLogger(TemplateAttrProvider.class.getName());

    private final Project project;
    
    public TemplateAttrProvider(Project prj) {
        project = prj;
    }
    
    public @Override Map<String,?> attributesFor(DataObject template, DataFolder target, String name) {
        Map<String,Object> values = new TreeMap<String,Object>();
        AuxiliaryProperties auxProps = project.getLookup().lookup(AuxiliaryProperties.class);
        String licensePath = auxProps.get(Constants.HINT_LICENSE_PATH, true); //NOI18N
        if (licensePath != null) {
            ExpressionEvaluator eval = PluginPropertyUtils.createEvaluator(project);
            
            try {
                Object no = eval.evaluate(licensePath);
                if (no != null) {
                    licensePath = no.toString();
                }
            } catch (ExpressionEvaluationException ex) {
                Exceptions.printStackTrace(ex);
            }
            File path = FileUtil.normalizeFile(FileUtilities.resolveFilePath(FileUtil.toFile(project.getProjectDirectory()), licensePath));
            if (path.exists() && path.isAbsolute()) { //is this necessary? should prevent failed license header inclusion
                URI uri = Utilities.toURI(path);
                licensePath = uri.toString();
                values.put("licensePath", licensePath);
            } else {
               LOG.log(Level.INFO, "project.licensePath value not accepted - " + licensePath);
            }
        }
        
        String license = auxProps.get(Constants.HINT_LICENSE, true); //NOI18N
        MavenProject mp = project.getLookup().lookup(NbMavenProject.class).getMavenProject();
        if (license == null) {
            license = findLicenseByMavenProjectContent(mp);
        }
        if (license != null) {
            values.put("license", license); // NOI18N
        }

        Organization organization = mp.getOrganization();
        if (organization != null) {
            String organizationName = organization.getName();
            if (organizationName != null) {
                values.put("organization", organizationName); // NOI18N
            }
        }

        FileEncodingQueryImplementation enc = project.getLookup().lookup(FileEncodingQueryImplementation.class);
        Charset charset = enc.getEncoding(target.getPrimaryFile());
        String encoding = (charset != null) ? charset.name() : null;
        if (encoding != null) {
            values.put("encoding", encoding); // NOI18N
        }

        ProjectInformation pi = ProjectUtils.getInformation(project);
        values.put("name", mp.getArtifactId()); // NOI18N
        values.put("displayName", pi.getDisplayName()); // NOI18N
        
        //#206321
        if (mp.getProperties() != null) {
            Map<String, Object> props = new HashMap<String, Object>();
            for (String prop : mp.getProperties().stringPropertyNames()) {
                String[] split = prop.split("\\.");
                String value = mp.getProperties().getProperty(prop);
                putProp(split, props, value);        
            }
            if (props.size() > 0) {
                values.put("property", props);
            }
        }

        if (values.size() > 0) {
            return Collections.singletonMap("project", values); // NOI18N        
        } else {
            return null;
        }
    }

    private void putProp(String[] split, Map<String, Object> props, String value) {
        if (split.length > 0) {
            if (split.length == 1) {
                props.put(split[0], value);
            } else {
                Object valu = props.get(split[0]);
                Map<String, Object> childProp;
                if (valu == null) {
                    childProp = new HashMap<String, Object>();
                    props.put(split[0], childProp);
                } else {
                    if (valu instanceof Map) {
                        childProp = (Map<String, Object>) valu;
                    } else {
                        //cannot have both maven.test and maven.test.skip properties defined :(
                        return;
                    }
                }
                putProp(Arrays.copyOfRange(split, 1, split.length), childProp, value);
            }
        }
    }

    public static String findLicenseByMavenProjectContent(MavenProject mp) {
        // try to match the project's license URL and the mavenLicenseURL attribute of license template
        FileObject licensesFO = FileUtil.getConfigFile("Templates/Licenses"); //NOI18N
        if (licensesFO == null) {
            return null;
        }
        FileObject[] licenseFiles = licensesFO.getChildren();
        for (License license : mp.getLicenses()) {
            String url = license.getUrl();
            if (url != null) {
                for (FileObject fo : licenseFiles) {
                    String str = (String)fo.getAttribute("mavenLicenseURL"); //NOI18N
                    if (str != null && Arrays.asList(str.split(" ")).contains(url)) {
                        if (fo.getName().startsWith("license-")) { // NOI18N
                            return fo.getName().substring("license-".length()); //NOI18N
                        } else {
                            Logger.getLogger(TemplateAttrProvider.class.getName()).log(Level.WARNING, "Bad license file name {0} (expected to start with ''license-'' prefix)", fo.getName());
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }
}