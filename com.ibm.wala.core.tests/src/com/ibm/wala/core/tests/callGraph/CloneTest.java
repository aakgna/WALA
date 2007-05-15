/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.core.tests.callGraph;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * Check properties of a call to clone() in RTA
 * 
 * @author Bruno Dufour
 */
public class CloneTest extends WalaTestCase {

  public void testClone() throws ClassHierarchyException {

    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope, cha);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraph cg = CallGraphTestUtil.buildRTA(options, cha, scope, warnings);

    // Find node corresp. to java.text.MessageFormat.clone()
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/text/MessageFormat");
    MethodReference m = MethodReference.findOrCreate(t, "clone", "()Ljava/lang/Object;");
    CGNode node = (CGNode) cg.getNodes(m).iterator().next();

    // Check there's exactly one target for each super call in
    // MessageFormat.clone()
    for (Iterator<CallSiteReference> i = node.iterateSites(); i.hasNext();) {
      CallSiteReference site = (CallSiteReference) i.next();
      if (site.isSpecial()) {
        if (site.getDeclaredTarget().getDeclaringClass().equals(TypeReference.JavaLangObject)) {
          Set<CGNode> targets = node.getPossibleTargets(site);
          if (targets.size() != 1) {
            System.err.println(targets.size() + " targets found for " + site);
            for (Iterator<CGNode> k = targets.iterator(); k.hasNext();) {
              System.err.println("  " + k.next());
            }
            fail("found " + targets.size() + " targets for " + site + " in " + node);
          }
        }
      }
    }
  }
}
