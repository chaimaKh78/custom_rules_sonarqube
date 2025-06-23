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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.*;

import java.util.Arrays;
import java.util.List;

@Rule(key = "pfe-custom-rules:InputValidationRule",
        name = "Input Validation Rule",
        description = "Validate inputs effectively to prevent injection vulnerabilities and ensure input quality.",
        priority = Priority.CRITICAL,
        tags = {"security", "input-validation"})
public class InputValidationRule extends IssuableSubscriptionVisitor {

    private static final List<String> INPUT_ANNOTATIONS = Arrays.asList(
            "org.springframework.web.bind.annotation.RequestParam",
            "org.springframework.web.bind.annotation.RequestBody",
            "org.springframework.web.bind.annotation.PathVariable"
    );

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.METHOD);  // Pas besoin de vérifier les constructeurs ici
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.METHOD)) {
            MethodTree method = (MethodTree) tree;

            for (VariableTree param : method.parameters()) {
                if (isInputParameter(param)) {
                    if (!isValidated(param)) {
                        reportIssue(param, "Les paramètres d'entrée doivent être validés avec @Valid ou des annotations de validation équivalentes.");
                    }
                }
            }
        }
    }

    // Vérifie si le paramètre a une annotation d'entrée comme @RequestParam, @RequestBody, ou @PathVariable
    private boolean isInputParameter(VariableTree param) {
        return param.symbol().metadata().annotations().stream()
                .anyMatch(annotation -> {
                    Type annotationType = annotation.symbol().type();
                    return INPUT_ANNOTATIONS.contains(annotationType.fullyQualifiedName());  // Utilisation de symbolType().fullyQualifiedName()
                });
    }

    // Vérifie si le paramètre est annoté avec @Valid pour la validation
    private boolean isValidated(VariableTree param) {
        return param.symbol().metadata().isAnnotatedWith("javax.validation.Valid") ||
                param.symbol().metadata().isAnnotatedWith("jakarta.validation.Valid");
    }
}
