package org.sonar.samples.java.checks;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.*;

import java.util.*;

@Rule(key = "pfe-custom-rules:HttpStatusConsistency",
        name = "HTTP Status Consistency",
        description = "Ensure HTTP responses return consistent status codes that accurately represent the response state.",
        priority = Priority.MINOR,
        tags = {"http", "best-practice"})
public class HttpStatusConsistencyRule extends IssuableSubscriptionVisitor {

    private static final Set<String> SUCCESS_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("OK", "CREATED", "ACCEPTED"))
    );

    private static final Set<String> ERROR_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("BAD_REQUEST", "NOT_FOUND", "INTERNAL_SERVER_ERROR"))
    );

    private static final Set<String> HTTP_METHODS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE"))
    );

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.RETURN_STATEMENT);
    }

    @Override
    public void visitNode(Tree tree) {
        ReturnStatementTree returnStatement = (ReturnStatementTree) tree;
        if (returnStatement.expression().is(Tree.Kind.METHOD_INVOCATION)) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) returnStatement.expression();

            // Vérifiez les incohérences de statuts HTTP
            checkHttpStatusConsistency(methodInvocation);

            // Vérifiez la bonne gestion des exceptions avec des statuts appropriés
            checkExceptionHandling(methodInvocation);

            // Vérifiez les statuts de réponse pour des contextes spécifiques
            checkContextualHttpStatus(methodInvocation);

            // Vérifiez les réponses multiples pour des incohérences
            checkMultipleResponsesConsistency(methodInvocation);
        }
    }

    private void checkHttpStatusConsistency(MethodInvocationTree methodInvocation) {
        if (methodInvocation.symbolType().name().equals("ResponseEntity")) {
            String httpStatus = extractHttpStatus(methodInvocation);  // Extract the HTTP status
            String httpMethod = extractHttpMethod(methodInvocation);  // Extract the HTTP method

            // Check if the status code is in the success range and validate the context
            if (SUCCESS_STATUSES.contains(httpStatus) && !isSuccessContext(methodInvocation)) {
                reportIssue(methodInvocation, "Success status code should be used in appropriate success contexts.");
            }

            // Check if the status code is in the error range and validate the context
            if (ERROR_STATUSES.contains(httpStatus) && isSuccessContext(methodInvocation)) {
                reportIssue(methodInvocation, "Error status code used without an appropriate failure context.");
            }
        }
    }

    private void checkContextualHttpStatus(MethodInvocationTree methodInvocation) {
        String httpStatus = extractHttpStatus(methodInvocation);
        if (SUCCESS_STATUSES.contains(httpStatus)) {
            // Vérification du contexte des réponses de succès
            if (!isContextForSuccess(methodInvocation)) {
                reportIssue(methodInvocation, "Ensure that success status codes are used properly.");
            }
        } else if (ERROR_STATUSES.contains(httpStatus)) {
            // Vérification du contexte des réponses d'erreur
            if (!isContextForError(methodInvocation)) {
                reportIssue(methodInvocation, "Ensure that error status codes are used properly.");
            }
        }
    }

    private void checkMultipleResponsesConsistency(MethodInvocationTree methodInvocation) {
        Optional<MethodTree> method = getEnclosingMethod(methodInvocation);
        if (method.isPresent()) {
            // Rechercher les multiples réponses dans les méthodes
            if (method.get().block().body().stream()
                    .filter(statement -> statement.is(Tree.Kind.RETURN_STATEMENT))
                    .map(statement -> (ReturnStatementTree) statement)
                    .anyMatch(returnStmt -> !isValidHttpResponse(returnStmt))) {
                reportIssue(methodInvocation, "Inconsistent use of HTTP response codes in multiple return statements.");
            }
        }
    }
    private Optional<MethodTree> getEnclosingMethod(MethodInvocationTree methodInvocation) {
        Tree parent = methodInvocation.parent();
        while (parent != null && !parent.is(Tree.Kind.METHOD)) {
            parent = parent.parent();
        }
        return parent != null ? Optional.of((MethodTree) parent) : Optional.empty();
    }

    private boolean isSuccessContext(MethodInvocationTree methodInvocation) {
        // Check if the method context is suitable for a success status code
        Optional<MethodTree> enclosingMethod = getEnclosingMethod(methodInvocation);
        return enclosingMethod.isPresent() && enclosingMethod.get().symbol().name().equals("success"); // Example condition
    }

    private boolean isContextForSuccess(MethodInvocationTree methodInvocation) {
        // You can check for specific conditions, like HTTP methods (GET, POST) or annotations (@ResponseStatus)
        String httpMethod = extractHttpMethod(methodInvocation);
        return httpMethod.equals("GET") || httpMethod.equals("POST"); // Example condition
    }

    private boolean isContextForError(MethodInvocationTree methodInvocation) {
        // Similar logic for error status codes (e.g., 400, 500)
        String httpMethod = extractHttpMethod(methodInvocation);
        return httpMethod.equals("PUT") || httpMethod.equals("DELETE"); // Example condition
    }


    private boolean isValidHttpResponse(ReturnStatementTree returnStmt) {
        if (returnStmt.expression().is(Tree.Kind.METHOD_INVOCATION)) {
            MethodInvocationTree invocation = (MethodInvocationTree) returnStmt.expression();
            String status = extractHttpStatus(invocation);
            return SUCCESS_STATUSES.contains(status) || ERROR_STATUSES.contains(status);
        }
        return false;
    }

    private void checkExceptionHandling(MethodInvocationTree methodInvocation) {
        Optional<MethodTree> enclosingMethod = getEnclosingMethod(methodInvocation);
        if (enclosingMethod.isPresent()) {
            MethodTree methodTree = enclosingMethod.get();

            methodTree.block().body().stream()
                    .filter(statement -> statement.is(Tree.Kind.TRY_STATEMENT))
                    .map(statement -> (TryStatementTree) statement)
                    .forEach(tryStatement -> {
                        for (CatchTree catchBlock : tryStatement.catches()) {
                            BlockTree catchBody = catchBlock.block();
                            for (StatementTree catchStatement : catchBody.body()) {
                                if (catchStatement.is(Tree.Kind.RETURN_STATEMENT)) {
                                    ReturnStatementTree returnStatement = (ReturnStatementTree) catchStatement;
                                    if (returnStatement.expression().is(Tree.Kind.METHOD_INVOCATION)) {
                                        MethodInvocationTree returnInvocation = (MethodInvocationTree) returnStatement.expression();
                                        String status = extractHttpStatus(returnInvocation);

                                        if (!ERROR_STATUSES.contains(status)) {
                                            reportIssue(returnInvocation, "Catch block should return an error status code.");
                                        }
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private String extractHttpStatus(MethodInvocationTree methodInvocation) {
        // Extraction du code de statut HTTP à partir de la méthode d'invocation
        return methodInvocation.symbol().name();
    }

    private String extractHttpMethod(MethodInvocationTree methodInvocation) {
        // Extraction du type de méthode HTTP (GET, POST, PUT, DELETE) à partir de l'invocation
        return methodInvocation.symbol().name();
    }
}