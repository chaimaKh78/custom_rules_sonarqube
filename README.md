# Custom SonarQube Rules (Java Plugin)

This project demonstrates how to create custom static code analysis rules for [SonarQube](https://www.sonarsource.com/products/sonarqube/) using Java. These rules can be used to enforce coding standards and detect specific patterns or anti-patterns in your Java codebase.

---

## 🎯 Project Objectives

- Develop and package custom static analysis rules for Java using SonarQube plugin API
- Extend SonarQube's default rule set with organization-specific best practices
- Test custom rules with unit tests and validate rule behavior with test projects
- Generate a plugin that can be deployed in a running SonarQube instance

---

## ✅ Key Benefits

- **Tailored Analysis**: Enforce rules aligned with your internal coding guidelines
- **Better Code Quality**: Catch project-specific anti-patterns early
- **Static Rules**: Plug-in runs before deployment, reducing bugs in production
- **Reusable Plugin**: Once compiled, the plugin can be used across teams and projects

---
## 🚀 How It Works

1. Each custom rule is defined by extending SonarQube's rule classes.
2. Metadata (name, description, severity) is declared to make the rule visible in the SonarQube UI.
3. Unit tests are provided to validate rule behavior on sample Java files.
4. The plugin is compiled with Maven and packaged as a `.jar`.
5. The jar is deployed to a SonarQube instance (`/extensions/plugins/`) and activated via the quality profile.

---

## 🔧 Requirements

- Java 11 or 17
- Maven 3.x
- SonarQube (compatible version)
- IDE like IntelliJ or Eclipse (optional)

---
