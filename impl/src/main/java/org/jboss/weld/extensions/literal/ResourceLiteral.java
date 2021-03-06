package org.jboss.weld.extensions.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.weld.extensions.resourceLoader.Resource;

public class ResourceLiteral extends AnnotationLiteral<Resource> implements Resource
{

   private static final long serialVersionUID = 4907169607105615674L;
   
   private final String name;

   public ResourceLiteral(String name)
   {
      this.name = name;
   }

   public String value()
   {
      return name;
   }
}