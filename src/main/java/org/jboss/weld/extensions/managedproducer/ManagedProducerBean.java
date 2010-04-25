package org.jboss.weld.extensions.managedproducer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Named;

/**
 * Bean implementation that produces a JDK proxy
 * 
 * when a method is invoked on the proxy it calls the managed producer method
 * and invokes the method on the returned object
 * 
 * @author stuart
 * 
 * @param <M>
 */
public class ManagedProducerBean<M> implements Bean<M>
{

   final Class<?> beanClass;

   final String name;

   final Set<Annotation> qualifiers;

   final Set<Type> types;

   final Class<?> proxyClass;

   final BeanManager manager;

   final Constructor<?> proxyConstructor;


   final AnnotatedMethod<?> method;

   public ManagedProducerBean(AnnotatedMethod<?> method, BeanManager manager)
   {
      this.method = method;
      beanClass = method.getDeclaringType().getJavaClass();
      // get the name
      if (method.isAnnotationPresent(Named.class))
      {
         name = method.getAnnotation(Named.class).value();
      }
      else
      {
         name = null;
      }
      // get the qualifiers
      qualifiers = new HashSet<Annotation>();
      for (Annotation a : method.getAnnotations())
      {
         if (manager.isQualifier(a.annotationType()))
         {
            qualifiers.add(a);
         }
      }
      if (qualifiers.isEmpty())
      {
         qualifiers.add(new AnnotationLiteral<Default>()
         {
         });
      }
      // get the bean types
      types = new HashSet<Type>();
      Set<Class<?>> interfaces = new HashSet<Class<?>>();
      for (Type t : method.getTypeClosure())
      {
         if (t instanceof Class<?>)
         {
            Class<?> c = (Class<?>) t;
            if (c.isInterface())
            {
               types.add(c);
               interfaces.add(c);
            }
         }
         else if (t instanceof ParameterizedType)
         {
            ParameterizedType p = (ParameterizedType) t;
            Class<?> c = (Class<?>) p.getRawType();
            if (c.isInterface())
            {
               types.add(t);
            }
         }
      }
      // build the properties
      Class<?>[] iarray = new Class[interfaces.size()];
      int count = 0;
      this.manager = manager;
      for (Class<?> c : interfaces)
      {
         iarray[count++] = c;
      }
      proxyClass = Proxy.getProxyClass(beanClass.getClassLoader(), iarray);
      try
      {
         proxyConstructor = proxyClass.getConstructor(new Class[] { InvocationHandler.class });
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }


   }

   public Class<?> getBeanClass()
   {
      return beanClass;
   }

   public Set<InjectionPoint> getInjectionPoints()
   {
      return Collections.emptySet();
   }

   public String getName()
   {
      return name;
   }

   public Set<Annotation> getQualifiers()
   {
      return qualifiers;
   }

   /**
    * the proxies that are injected all have Dependant scope
    */
   public Class<? extends Annotation> getScope()
   {
      return Dependent.class;
   }

   public Set<Class<? extends Annotation>> getStereotypes()
   {
      return Collections.emptySet();
   }

   public Set<Type> getTypes()
   {
      return types;
   }

   public boolean isAlternative()
   {
      return false;
   }

   public boolean isNullable()
   {
      return false;
   }

   public M create(CreationalContext<M> creationalContext)
   {
      Annotation[] quals = { new AnnotationLiteral<Default>()
      {
      } };
      Set<Bean<?>> beans = manager.getBeans(InjectionPoint.class, quals);
      Bean injectionPointBean = (Bean) beans.iterator().next();
      InjectionPoint injectionPoint = (InjectionPoint) manager.getReference(injectionPointBean, InjectionPoint.class, creationalContext);
      ManagedProducerInvocationHandler<?> hdl = new ManagedProducerInvocationHandler(manager, this.method, this, injectionPoint);
      try
      {
         M obj = (M) proxyConstructor.newInstance(new Object[] { hdl });
         creationalContext.push(obj);
         return obj;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void destroy(M instance, CreationalContext<M> creationalContext)
   {
      creationalContext.release();
   }

}