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

@Rule(key = "pfe-custom-rules:InefficientDatabaseCallsRule",
        name = "Avoid Inefficient Database Calls",
        description = "Identify and refactor inefficient database calls that may lead to performance bottlenecks.",
        priority = Priority.MAJOR,
        tags = {"performance", "database"})
public class InefficientDatabaseCallsRule extends IssuableSubscriptionVisitor {

    private static final List<String> SAVE_METHODS = Arrays.asList("save", "saveAll");

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.WHILE_STATEMENT); // Utilisation correcte des kinds
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.FOR_STATEMENT)) {
            checkLoopForSaveCalls((ForStatementTree) tree);
        } else if (tree.is(Tree.Kind.FOR_EACH_STATEMENT)) { // Utilisation correcte de FOR_EACH_STATEMENT
            checkLoopForSaveCalls((ForEachStatement) tree);
        } else if (tree.is(Tree.Kind.WHILE_STATEMENT)) {
            checkLoopForSaveCalls((WhileStatementTree) tree);
        }
    }

    // Méthode pour vérifier les appels dans les boucles
    private void checkLoopForSaveCalls(StatementTree loopTree) {
        loopTree.accept(new SaveMethodInvocationVisitor());
    }

    // Classe visiteur pour détecter les appels de méthode save/saveAll
    private class SaveMethodInvocationVisitor extends BaseTreeVisitor {
        @Override
        public void visitMethodInvocation(MethodInvocationTree methodInvocationTree) {
            String methodName = methodInvocationTree.symbolType().name(); // Utilisation de symbolType() pour récupérer le type
            if (SAVE_METHODS.contains(methodName)) {
                reportIssue(methodInvocationTree, "Évitez de sauvegarder des entités à l'intérieur des boucles. Utilisez des mises à jour par lots pour améliorer les performances.");
            }
            super.visitMethodInvocation(methodInvocationTree);
        }
    }
}
