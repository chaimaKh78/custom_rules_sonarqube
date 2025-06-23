package org.sonar.samples.java.checks;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "CustomCheckForDefineClass",
        name = "Use java.lang.invoke.MethodHandles.Lookup.defineClass instead sun.misc.Unsafe.defineClass",
        description = "Recommended to use java.lang.invoke.MethodHandles.Lookup.defineClass instead sun.misc.Unsafe.defineClass",
        priority = Priority.CRITICAL,
        tags = {"bug"})
public class CustomCheckForDefineClass extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
    }

    @Override
    public void visitNode(Tree tree) {
        Symbol methodSymbol = ((MethodInvocationTree) tree).symbol();
        if (methodSymbol.type() != null) {
            String methodFullName = methodSymbol.type().fullyQualifiedName() + "." + methodSymbol.name();
            if ("sun.misc.Unsafe.defineClass".equals(methodFullName)) {
                reportIssue(tree, "It is recommended to use the method java.lang.invoke.MethodHandles.Lookup.defineClass instead sun.misc.Unsafe.defineClass");
            }
        }
    }
}
