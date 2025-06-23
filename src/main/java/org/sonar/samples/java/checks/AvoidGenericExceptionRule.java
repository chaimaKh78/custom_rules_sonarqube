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
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
@Rule(key = "pfe-custom-rules:AvoidGenericExceptionRule",
        name = "Avoid Generic Exceptions",
        description = "Avoid catching generic exceptions; specify the exception type to improve error handling and readability.",
        priority = Priority.MAJOR,
        tags = {"error-handling", "best-practice"})
public class AvoidGenericExceptionRule extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.TRY_STATEMENT);  // Utilisation de TRY_STATEMENT pour visiter les blocs try
    }

    @Override
    public void visitNode(Tree tree) {
        TryStatementTree tryStatementTree = (TryStatementTree) tree;

        for (CatchTree catchTree : tryStatementTree.catches()) {
            TypeTree caughtType = catchTree.parameter().type();

            if (isGenericExceptionType(caughtType)) {
                String message = "Avoid using generic exception types like 'Exception' or 'Throwable'. Prefer more specific exceptions.";

                // Vérification des différents scénarios
                if (isNestedTryCatch(catchTree)) {
                    message += " This catch block contains nested try-catch blocks. Consider handling exceptions more specifically.";
                }

                if (isExceptionHandlingInResourceContext(catchTree)) {
                    message += " Ensure that exceptions in resource handling or cleanup contexts are handled appropriately.";
                }

                if (isSwallowedException(catchTree)) {
                    message += " Avoid swallowing exceptions without logging or handling them.";
                }

                if (isRethrownException(catchTree)) {
                    message += " If exceptions are rethrown, ensure that they are properly documented or wrapped.";
                }

                if (isOverlyBroadExceptionHandling(catchTree)) {
                    message += " Avoid broad exception handling that can mask other issues.";
                }

                // Report d'une issue sur le type d'exception générique
                reportIssue(caughtType, message);
            }
        }
    }

    private boolean isGenericExceptionType(TypeTree caughtType) {
        // Vérification du type d'exception capturé
        String typeName = caughtType.symbolType().fullyQualifiedName();
        return "java.lang.Exception".equals(typeName) || "java.lang.Throwable".equals(typeName);
    }

    private boolean isNestedTryCatch(CatchTree catchTree) {
        // Détection des blocs try-catch imbriqués
        BlockTree blockTree = catchTree.block();
        return blockTree.body().stream().anyMatch(statement -> statement.is(Tree.Kind.TRY_STATEMENT));
    }

    private boolean isExceptionHandlingInResourceContext(CatchTree catchTree) {
        // Détection des méthodes de gestion des ressources dans le bloc catch (ex. close, flush, release)
        BlockTree blockTree = catchTree.block();
        return blockTree.body().stream().anyMatch(statement -> {
            if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
                ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
                if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
                    MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
                    return Pattern.compile(".*(close|flush|release|commit).*").matcher(methodInvocation.methodSelect().toString()).find();
                }
            }
            return false;
        });
    }

    private boolean isSwallowedException(CatchTree catchTree) {
        // Détection des exceptions avalées (sans gestion ni journalisation)
        BlockTree blockTree = catchTree.block();
        return blockTree.body().isEmpty() || blockTree.body().stream().noneMatch(statement ->
                (statement.is(Tree.Kind.EXPRESSION_STATEMENT) &&
                        ((ExpressionStatementTree) statement).expression().is(Tree.Kind.METHOD_INVOCATION) &&
                        ((MethodInvocationTree) ((ExpressionStatementTree) statement).expression()).methodSelect().toString().contains("log")) ||
                        statement.is(Tree.Kind.THROW_STATEMENT)
        );
    }

    private boolean isRethrownException(CatchTree catchTree) {
        // Vérification des exceptions relancées
        BlockTree blockTree = catchTree.block();
        return blockTree.body().stream().anyMatch(statement -> statement.is(Tree.Kind.THROW_STATEMENT));
    }

    private boolean isOverlyBroadExceptionHandling(CatchTree catchTree) {
        // Vérification de la gestion trop large des exceptions
        TypeTree caughtType = catchTree.parameter().type();
        String typeName = caughtType.symbolType().fullyQualifiedName();
        return "java.lang.Exception".equals(typeName) || "java.lang.Throwable".equals(typeName);
    }
}
