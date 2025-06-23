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

import java.util.Collections;
import java.util.List;

@Rule(key = "pfe-custom-rules:ExceptionHandlingRule",
        name = "Exception Handling Best Practices",
        description = "Follow best practices in exception handling, including logging and rethrowing meaningful exception messages.",
        priority = Priority.MAJOR,
        tags = {"error-handling"})
public class ExceptionHandlingRule extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);  // Visiter les méthodes
    }

    @Override
    public void visitNode(Tree tree) {
        MethodTree methodTree = (MethodTree) tree;

        // Récupérer les exceptions déclarées dans la clause throws de la méthode
        List<TypeTree> thrownExceptions = methodTree.throwsClauses();

        if (!thrownExceptions.isEmpty() && !containsTryCatch(methodTree)) {
            // Si des exceptions sont déclarées mais aucun bloc try-catch n'est présent
            reportIssue(methodTree.simpleName(), "La méthode doit gérer les exceptions déclarées ou utiliser @ControllerAdvice pour la gestion centralisée.");
        }
    }

    // Vérifier si la méthode contient un bloc try-catch
    private boolean containsTryCatch(MethodTree methodTree) {
        if (methodTree.block() == null) {
            return false; // Pas de bloc de code dans la méthode
        }

        // Vérifier si un bloc try-catch est présent dans le corps de la méthode
        return methodTree.block().body().stream()
                .anyMatch(statement -> statement.is(Tree.Kind.TRY_STATEMENT));
    }
}
