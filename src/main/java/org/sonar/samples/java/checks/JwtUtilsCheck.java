package org.sonar.samples.java.checks;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "pfe-custom-rules:JwtUtilsCheck",
        name = "JWT Utils Security Check",
        description = "Ensure secure JWT operations, such as proper token validation and signature verification.",
        priority = Priority.CRITICAL,
        tags = {"security", "jwt"})
public class JwtUtilsCheck extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD);
    }
    private void checkValidateJwtTokenMethod(MethodTree methodTree) {
        boolean hasExceptionHandling = false;
        for (Tree statement : methodTree.block().body()) {
            if (statement instanceof MethodInvocationTree) {
                MethodInvocationTree methodInvocation = (MethodInvocationTree) statement;
                String methodName = methodInvocation.methodSelect().toString();
                if (methodName.contains("logger.error")) {
                    hasExceptionHandling = true;
                }
            }
        }
        if (!hasExceptionHandling) {
            reportIssue(methodTree, "Ensure that exceptions during JWT validation are logged.");
        }

        // Check for the use of a secure key
        boolean usesSecureKey = false;
        for (Tree statement : methodTree.block().body()) {
            if (statement instanceof MethodInvocationTree) {
                MethodInvocationTree methodInvocation = (MethodInvocationTree) statement;
                if (methodInvocation.methodSelect().toString().contains("Keys.hmacShaKeyFor")) {
                    usesSecureKey = true;
                }
            }
        }
        if (!usesSecureKey) {
            reportIssue(methodTree, "Ensure that a secure key is used for JWT validation.");
        }
    }
    private void checkMethods(ClassTree classTree) {
        for (Tree member : classTree.members()) {
            if (member instanceof MethodTree) {
                MethodTree methodTree = (MethodTree) member;
                if (methodTree.symbol().name().equals("validateJwtToken")) {
                    checkValidateJwtTokenMethod(methodTree);
                }
            }
        }
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree instanceof ClassTree) {
            ClassTree classTree = (ClassTree) tree;
            if (classTree.symbol().type().is("com.example.PokerPlanningBack.security.jwt.JwtUtils")) {
                checkMethods(classTree);
            }
        }
        super.visitNode(tree);
    }



}
//Rule: S1002 - Validation de JWT avec JwtUtils
//Description : Cette règle vérifie si la classe JwtUtils utilise les pratiques sécurisées pour la validation et la gestion des jetons JWT.
//
//Critères de Sécurité :
//
//JwtUtils doit utiliser la clé de signature sécurisée pour la validation des jetons.
//Les exceptions lors de la validation des jetons doivent être correctement enregistrées.