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
import org.sonar.plugins.java.api.tree.*;

import java.util.Arrays;
import java.util.List;

@Rule(key = "pfe-custom-rules:FileUploadSecurityRule",
        name = "File Upload Security Rule",
        description = "Ensure security checks during file uploads, such as validating file type and scanning for potential threats.",
        priority = Priority.CRITICAL,
        tags = {"security", "upload"})
public class FileUploadSecurityRule extends IssuableSubscriptionVisitor {

    private static final List<String> VALIDATION_METHODS = Arrays.asList(
            "isValidExcelFile", "validateFileType", "validateFileSize", "scanForMalware"
    );

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.ASSIGNMENT);
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
            String methodName = methodInvocation.symbolType().name();
            if (methodName.equals("saveAll") || methodName.equals("save")) {
                // Ensure validations are done before saving the file
                if (!hasPriorValidation(tree)) {
                    reportIssue(methodInvocation, "Ensure file validation (type, size, content) and scanning are performed before saving.");
                }
            }
        } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
            AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
            if (assignment.variable().symbolType().name().toLowerCase().contains("file")) {
                // Ensure validation methods are applied to the file
                ExpressionTree expression = assignment.expression();
                if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
                    MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
                    if (!VALIDATION_METHODS.contains(methodInvocation.symbolType().name())) {
                        reportIssue(expression, "Uploaded files must be validated (type, size, content) and scanned for malware.");
                    }
                }
            }
        }
    }

    private boolean hasPriorValidation(Tree tree) {
        // Get the parent block (scope) where this method call is located
        Tree parent = tree.parent();

        // Iterate over the previous statements in the block
        while (parent != null && parent.is(Tree.Kind.BLOCK)) {
            BlockTree block = (BlockTree) parent;
            List<StatementTree> statements = block.body();

            // Iterate over the statements to find the position of the current tree
            for (int i = 0; i < statements.size(); i++) {
                StatementTree statement = statements.get(i);

                // If the current tree matches the statement, check previous statements
                if (statement.equals(tree)) {
                    // Look backwards to check if validation methods were called
                    for (int j = i - 1; j >= 0; j--) {
                        StatementTree previousStatement = statements.get(j);

                        // Check if it's a method invocation and if it's one of the validation methods
                        if (previousStatement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
                            ExpressionStatementTree exprStatement = (ExpressionStatementTree) previousStatement;
                            ExpressionTree expression = exprStatement.expression();

                            if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
                                MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
                                String methodName = methodInvocation.symbolType().name();

                                // Check if the method is a known validation method
                                if (VALIDATION_METHODS.contains(methodName)) {
                                    return true; // Validation was performed before saving
                                }
                            }
                        }
                    }
                }
            }

            // Move to the next parent block (in case of nested blocks)
            parent = parent.parent();
        }

        return false; // No prior validation found
    }
}


