/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.util.xml;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A utility class to cover up the rough bits of xml parsing
 *      
 * @author <a href="mailto:chris@kimptoc.net">Chris Kimpton</a>
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class XmlHelper
{
   public static void write(Writer out, Document dom)
      throws Exception
   {
      new DOMWriter(out).setPrettyprint(true).print(dom);
   }

   /**
    * Returns an iterator over the children of the given element with
    * the given tag name.
    *
    * @param element    The parent element
    * @param tagName    The name of the desired child
    * @return           An interator of children or null if element is null.
    */
   public static Iterator getChildrenByTagName(Element element,
                                               String tagName)
   {
      if (element == null) return null;
      // getElementsByTagName gives the corresponding elements in the whole 
      // descendance. We want only children

      NodeList children = element.getChildNodes();
      ArrayList goodChildren = new ArrayList();
      for (int i=0; i<children.getLength(); i++) {
         Node currentChild = children.item(i);
         if (currentChild.getNodeType() == Node.ELEMENT_NODE && 
             ((Element)currentChild).getTagName().equals(tagName)) {
            goodChildren.add(currentChild);
         }
      }
      return goodChildren.iterator();
   }

   /**
    * Gets the child of the specified element having the specified unique
    * name.  If there are more than one children elements with the same name
    * and exception is thrown.
    *
    * @param element    The parent element
    * @param tagName    The name of the desired child
    * @return           The named child.
    *
    * @throws Exception   Child was not found or was not unique.
    */
   public static Element getUniqueChild(Element element, String tagName)
      throws Exception
   {
      Iterator goodChildren = getChildrenByTagName(element, tagName);

      if (goodChildren != null && goodChildren.hasNext()) {
         Element child = (Element)goodChildren.next();
         if (goodChildren.hasNext()) {
            throw new Exception
               ("expected only one " + tagName + " tag");
         }
         return child;
      } else {
         throw new Exception
            ("expected one " + tagName + " tag");
      }
   }

   /**
    * Gets the child of the specified element having the
    * specified name. If the child with this name doesn't exist
    * then null is returned instead.
    *
    * @param element the parent element
    * @param tagName the name of the desired child
    * @return either the named child or null
    * @throws Exception
    */
   public static Element getOptionalChild(Element element, String tagName)
      throws Exception
   {
      return getOptionalChild(element, tagName, null);
   }

   /**
    * Gets the child of the specified element having the
    * specified name. If the child with this name doesn't exist
    * then the supplied default element is returned instead.
    *
    * @param element the parent element
    * @param tagName the name of the desired child
    * @param defaultElement the element to return if the child
    *                       doesn't exist
    * @return either the named child or the supplied default
    * @throws Exception 
    */
   public static Element getOptionalChild(Element element,
                                          String tagName,
                                          Element defaultElement)
      throws Exception
   {
      Iterator goodChildren = getChildrenByTagName(element, tagName);

      if (goodChildren != null && goodChildren.hasNext()) {
         Element child = (Element)goodChildren.next();
         if (goodChildren.hasNext()) {
            throw new Exception
               ("expected only one " + tagName + " tag");
         }
         return child;
      } else {
         return defaultElement;
      }
   }

   /**
    * Get the content of the given element.
    *
    * @param element    The element to get the content for.
    * @return           The content of the element or null.
    * @throws Exception 
    */
   public static String getElementContent(final Element element)
      throws Exception
   {
      return getElementContent(element, null);
   }

   /**
    * Get the content of the given element.
    *
    * @param element       The element to get the content for.
    * @param defaultStr    The default to return when there is no content.
    * @return              The content of the element or the default.
    * @throws Exception 
    */
   public static String getElementContent(Element element, String defaultStr)
      throws Exception
   {
      if (element == null)
         return defaultStr;

      NodeList children = element.getChildNodes();
      String result = "";
      for (int i = 0; i < children.getLength(); i++)
      {
         if (children.item(i).getNodeType() == Node.TEXT_NODE || 
             children.item(i).getNodeType() == Node.CDATA_SECTION_NODE)
         {
            result += children.item(i).getNodeValue();
         }
         else if( children.item(i).getNodeType() == Node.COMMENT_NODE )
         {
            // Ignore comment nodes
         }
      }
      return result.trim();
   }

   /**
    * Macro to get the content of a unique child element.
    *
    * @param element    The parent element.
    * @param tagName    The name of the desired child.
    * @return           The element content or null.
    * @throws Exception 
    */
   public static String getUniqueChildContent(Element element,
                                              String tagName)
      throws Exception
   {
      return getElementContent(getUniqueChild(element, tagName));
   }

   /**
    * Macro to get the content of an optional child element.
    * 
    * @param element    The parent element.
    * @param tagName    The name of the desired child.
    * @return           The element content or null.
    * @throws Exception 
    */
   public static String getOptionalChildContent(Element element,
                                                String tagName)
      throws Exception
   {
      return getElementContent(getOptionalChild(element, tagName));
   }

   public static boolean getOptionalChildBooleanContent(Element element, String name) throws Exception
   {
      Element child = getOptionalChild(element, name);
      if(child != null)
      {
         String value = getElementContent(child).toLowerCase();
         return value.equals("true") || value.equals("yes");
      }

      return false;
   }


}

