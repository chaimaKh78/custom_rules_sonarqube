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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Rule(key = "pfe-custom-rules:PasswordEncoderRule",
        name = "Password Encoder Check",
        description = "Ensure proper encoding for sensitive data, such as passwords, using secure hash algorithms.",
        priority = Priority.CRITICAL,
        tags = {"security", "authentication"})
public class PasswordEncoderRule extends IssuableSubscriptionVisitor {

    // Encodeurs forts et recommandés
    private static final Set<String> SECURE_ENCODERS = new HashSet<>(Arrays.asList(
            "BCryptPasswordEncoder",
            "Argon2PasswordEncoder",
            "PBKDF2PasswordEncoder",
            "SCryptPasswordEncoder"
    ));

    // Encodeurs faibles ou obsolètes
    private static final Set<String> WEAK_ENCODERS = new HashSet<>(Arrays.asList(
            "NoOpPasswordEncoder",
            "MessageDigestPasswordEncoder",
            "MD5PasswordEncoder",
            "SHA1PasswordEncoder",
            "StandardPasswordEncoder"
    ));

    @Override
    public List<Tree.Kind> nodesToVisit() {
        // On visite les instanciations de classe (new) et les appels de méthodes
        return Arrays.asList(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.NEW_CLASS)) {
            visitNewClass((NewClassTree) tree);
        } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
            visitMethodInvocation((MethodInvocationTree) tree);
        }
    }

    private void visitNewClass(NewClassTree newClassTree) {
        // Vérifier si la classe instanciée est un encodeur de mot de passe
        if (isPasswordEncoder(newClassTree)) {
            String encoderClassName = newClassTree.symbolType().name();

            if (isWeakPasswordEncoder(encoderClassName)) {
                reportIssue(newClassTree, "The encoder " + encoderClassName + " is considered weak or insecure. Use a secure password encoder such as BCryptPasswordEncoder or Argon2PasswordEncoder.");
            } else if (!isSecurePasswordEncoder(encoderClassName)) {
                reportIssue(newClassTree, "Unrecognized password encoder. Consider using a secure password encoder such as BCryptPasswordEncoder.");
            }
        }
    }

    private void visitMethodInvocation(MethodInvocationTree methodInvocationTree) {
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) methodInvocationTree.symbolType();
        String methodName = methodSymbol.name();

        if (isWeakHashMethod(methodName)) {
            reportIssue(methodInvocationTree, "Avoid using weak hashing methods such as " + methodName + ". Use secure hashing algorithms like SHA-256 or stronger.");
        }

        if (isCriticalMethod(methodSymbol) && !isUsingSecureEncoder(methodInvocationTree)) {
            reportIssue(methodInvocationTree, "In critical authentication methods, ensure the use of secure password encoders like BCryptPasswordEncoder.");
        }
    }

    private boolean isPasswordEncoder(NewClassTree newClassTree) {
        Type type = newClassTree.symbolType();
        return type.isSubtypeOf("org.springframework.security.crypto.password.PasswordEncoder");
    }

    private boolean isWeakHashMethod(String methodName) {
        return methodName.equals("MD5") || methodName.equals("SHA1");
    }

    private boolean isUsingSecureEncoder(MethodInvocationTree methodInvocationTree) {
        // Vérifier si un encodeur sécurisé est utilisé
        Optional<Symbol> encoderSymbol = methodInvocationTree.symbolType().symbol().lookupSymbols("passwordEncoder").stream().findFirst();
        return encoderSymbol.isPresent() && SECURE_ENCODERS.contains(encoderSymbol.get().type().name());
    }

    private boolean isCriticalMethod(Symbol.MethodSymbol methodSymbol) {
        return methodSymbol.name().contains("authenticate") || methodSymbol.name().contains("login");
    }

    private boolean isWeakPasswordEncoder(String className) {
        return WEAK_ENCODERS.contains(className);
    }

    private boolean isSecurePasswordEncoder(String className) {
        return SECURE_ENCODERS.contains(className);
    }
}
