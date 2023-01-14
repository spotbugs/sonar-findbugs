/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs.classpath;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.JavaResourceLocator;

/**
 * @author gtoison
 *
 */
public class DefaultClassPathLocator implements ClassPathLocator {
  @SuppressWarnings("rawtypes")
  private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  
  private static final Logger LOG = Loggers.get(DefaultClassPathLocator.class);

  private JavaResourceLocator javaResourceLocator;
  
  public DefaultClassPathLocator(JavaResourceLocator javaResourceLocator) {
    this.javaResourceLocator = javaResourceLocator;
  }

  @Override
  public Collection<File> binaryDirs() {
    return callNoArgMethodReturningFilesCollection("binaryDirs");
  }

  @Override
  public Collection<File> classpath() {
    return javaResourceLocator.classpath();
  }

  @Override
  public Collection<File> testBinaryDirs() {
    return callNoArgMethodReturningFilesCollection("testBinaryDirs");
  }
  
  @Override
  public Collection<File> testClasspath() {
    return callNoArgMethodReturningFilesCollection("testClassPath");
  }

  @SuppressWarnings("unchecked")
  private Collection<File> callNoArgMethodReturningFilesCollection(String methodName) {
    try {
      Method method = JavaResourceLocator.class.getDeclaredMethod(methodName, EMPTY_CLASS_ARRAY);
      return (Collection<File>) method.invoke(javaResourceLocator, EMPTY_OBJECT_ARRAY);
    } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      LOG.info("JavaResourceLocator." + methodName + "() not available before SonarQube 9.8");
      LOG.debug("Error calling JavaResourceLocator." + methodName + "()", e);
      
      return Collections.emptySet();
    }
  }
}
