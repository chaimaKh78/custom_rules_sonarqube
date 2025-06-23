/*
 * SonarQube Java Custom Rules Example
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.samples.java.checks;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "pfe-custom-rules:SecureAuthEntryPointCheck",
        name = "Secure Authentication Entry Point",
        description = "Ensure the authentication entry points are securely configured to prevent unauthorized access.",
        priority = Priority.CRITICAL,
        tags = {"security", "authentication"})
public class SecureAuthEntryPointCheck extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree instanceof ClassTree) {
            ClassTree classTree = (ClassTree) tree;
            if (classTree.symbol().type().is("org.springframework.security.web.AuthenticationEntryPoint")) {
                checkMethods(classTree);
            }
        }
        super.visitNode(tree);
    }

    private void checkMethods(ClassTree classTree) {
        for (Tree member : classTree.members()) {
            if (member instanceof MethodTree) {
                MethodTree methodTree = (MethodTree) member;
                if (methodTree.symbol().name().equals("commence")) {
                    checkCommenceMethod(methodTree);
                }
            }
        }
    }


    private void checkCommenceMethod(MethodTree methodTree) {
        for (Tree statement : methodTree.block().body()) {
            if (statement instanceof MethodInvocationTree) {
                MethodInvocationTree methodInvocation = (MethodInvocationTree) statement;
                String methodName = methodInvocation.methodSelect().toString();
                if (methodName.equals("sendError")) {
                    // Check for HTTP status 401
                    if (methodInvocation.arguments().get(0).toString().equals("HttpServletResponse.SC_UNAUTHORIZED")) {
                        // Ensure exception is logged
                        checkLogging(methodTree);
                    } else {
                        reportIssue(methodInvocation, "Ensure 'sendError' method uses HTTP status 401 for unauthorized errors.");
                    }
                }
            }
        }
    }


    private void checkLogging(MethodTree methodTree) {
        // Check for logging usage
        boolean hasLogger = false;
        for (Tree statement : methodTree.block().body()) {
            if (statement instanceof MethodInvocationTree) {
                MethodInvocationTree methodInvocation = (MethodInvocationTree) statement;
                if (methodInvocation.methodSelect().toString().contains("logger.error")) {
                    hasLogger = true;
                }
            }
        }
        if (!hasLogger) {
            reportIssue(methodTree, "Ensure that exceptions are logged using a logger.");
        }
    }

}