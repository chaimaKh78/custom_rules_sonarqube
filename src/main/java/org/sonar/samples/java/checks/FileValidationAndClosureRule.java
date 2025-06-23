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

import java.util.*;


@Rule(key = "pfe-custom-rules:FileValidationAndClosure",
        name = "Ensure File Validation and Scanning Before Saving",
        description = "Validate file type, size, and scan for malware before saving uploaded files.",
        priority = Priority.CRITICAL,
        tags = {"security"})
public class FileValidationAndClosureRule extends IssuableSubscriptionVisitor {

    // Déclaration des méthodes d'ouverture et de fermeture des fichiers
    private static final Set<String> FILE_OPEN_METHODS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("getInputStream", "openStream", "readFile", "read"))
    );
    private static final String FILE_VALIDATION_METHOD = "isValidFile";
    private static final String FILE_CLOSE_METHOD = "close";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
    }

    @Override
    public void visitNode(Tree tree) {
        MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
        String methodName = methodInvocation.symbolType().name();

        // Vérifiez si la méthode actuelle est une méthode d'ouverture de fichier
        if (FILE_OPEN_METHODS.contains(methodName)) {
            // Vérifiez la validation du fichier avant son traitement
            if (!isValidationDone(methodInvocation)) {
                reportIssue(methodInvocation, "File should be validated before processing.");
            }

            // Vérifiez si le fichier est correctement fermé
            if (!isFileClosed(methodInvocation)) {
                reportIssue(methodInvocation, "File should be closed after processing.");
            }
        }
    }

    private boolean isValidationDone(MethodInvocationTree methodInvocation) {
        Optional<MethodTree> method = getEnclosingMethod(methodInvocation);
        if (method.isPresent()) {
            BlockTree methodBody = method.get().block();
            for (StatementTree statement : methodBody.body()) {
                if (statement.is(Tree.Kind.METHOD_INVOCATION)) {
                    MethodInvocationTree invocation = (MethodInvocationTree) statement;
                    if (invocation.symbolType().name().equals(FILE_VALIDATION_METHOD)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isFileClosed(MethodInvocationTree methodInvocation) {
        Optional<MethodTree> method = getEnclosingMethod(methodInvocation);
        if (method.isPresent()) {
            BlockTree methodBody = method.get().block();
            for (StatementTree statement : methodBody.body()) {
                if (statement.is(Tree.Kind.METHOD_INVOCATION)) {
                    MethodInvocationTree invocation = (MethodInvocationTree) statement;
                    if (invocation.symbolType().name().equals(FILE_CLOSE_METHOD)) {
                        return true;
                    }
                }
            }
        }
        // Recherche d'une structure try-with-resources
        return containsTryWithResources(methodInvocation);
    }

    private Optional<MethodTree> getEnclosingMethod(MethodInvocationTree methodInvocation) {
        Tree parent = methodInvocation.parent();
        while (parent != null && !(parent instanceof MethodTree)) {
            parent = parent.parent();
        }
        return Optional.ofNullable((MethodTree) parent);
    }

    private boolean containsTryWithResources(MethodInvocationTree methodInvocation) {
        Tree parent = methodInvocation.parent();
        while (parent != null) {
            if (parent.is(Tree.Kind.TRY_STATEMENT)) {
                TryStatementTree tryStatement = (TryStatementTree) parent;

                // Parcourir les blocs de ressources dans le try-with-resources
                if (tryStatement.block() != null) {
                    // Logique personnalisée pour analyser les ressources dans le bloc try
                    return isResourceManagedProperly(tryStatement, methodInvocation);
                }
            }
            parent = parent.parent();
        }
        return false;
    }

    private boolean isResourceManagedProperly(TryStatementTree tryStatement, MethodInvocationTree methodInvocation) {
        // Parcourir les éléments à l'intérieur du bloc `try`
        for (Tree resource : tryStatement.resourceList()) {
            // Si la ressource est un appel de méthode, vérifier si elle correspond à l'appel d'ouverture de fichier
            if (resource.is(Tree.Kind.VARIABLE)) {
                VariableTree variableTree = (VariableTree) resource;
                if (variableTree.initializer() instanceof MethodInvocationTree) {
                    MethodInvocationTree resourceMethodInvocation = (MethodInvocationTree) variableTree.initializer();
                    if (resourceMethodInvocation.symbolType().equals(methodInvocation.symbolType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    ////Dans une grande application de gestion de fichiers, une mauvaise gestion des fichiers non validés et non fermés provoque des fuites de ressources et des erreurs système. La règle "FileValidationAndClosure" agit comme un garde-fou, en s'assurant que chaque fichier est validé avant d'être traité et correctement fermé après usage. Cela évite des fuites de mémoire et des erreurs inattendues, garantissant la stabilité du système.



}

