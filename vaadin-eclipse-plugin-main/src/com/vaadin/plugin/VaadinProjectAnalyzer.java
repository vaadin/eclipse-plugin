package com.vaadin.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.*;

/**
 * Utility class for analyzing Vaadin projects and finding various components.
 */
public class VaadinProjectAnalyzer {

    private final IJavaProject javaProject;

    public VaadinProjectAnalyzer(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    /**
     * Find all classes with @Route annotation.
     */
    public List<Map<String, Object>> findVaadinRoutes() throws CoreException {
        List<Map<String, Object>> routes = new ArrayList<>();

        // Search for all types with @Route annotation
        List<IType> routeTypes = findTypesWithAnnotation("com.vaadin.flow.router.Route");

        for (IType type : routeTypes) {
            Map<String, Object> route = new HashMap<>();

            // Get the route value from annotation
            String routeValue = getAnnotationValue(type, "com.vaadin.flow.router.Route", "value");
            if (routeValue == null) {
                routeValue = ""; // Default route
            }

            route.put("route", routeValue);
            route.put("classname", type.getFullyQualifiedName());
            routes.add(route);
        }

        return routes;
    }

    /**
     * Find all Vaadin components (classes extending Component).
     */
    public List<Map<String, Object>> findVaadinComponents(boolean includeMethods) throws CoreException {
        List<Map<String, Object>> components = new ArrayList<>();

        // Find the Component type
        IType componentType = javaProject.findType("com.vaadin.flow.component.Component");
        if (componentType == null) {
            // Vaadin not in classpath
            return components;
        }

        // Search for all subtypes of Component
        ITypeHierarchy hierarchy = componentType.newTypeHierarchy(javaProject, null);
        IType[] allSubtypes = hierarchy.getAllSubtypes(componentType);

        for (IType type : allSubtypes) {
            // Only include project types, not library types
            if (type.getResource() != null && type.getResource().getProject().equals(javaProject.getProject())) {
                Map<String, Object> component = new HashMap<>();
                component.put("class", type.getFullyQualifiedName());
                component.put("origin", "project");
                component.put("source", "java");

                if (type.getResource() != null) {
                    component.put("path", type.getResource().getProjectRelativePath().toString());
                }

                if (includeMethods) {
                    StringBuilder methods = new StringBuilder();
                    for (IMethod method : type.getMethods()) {
                        if (methods.length() > 0) {
                            methods.append(",");
                        }
                        methods.append(getMethodSignature(method));
                    }
                    component.put("methods", methods.toString());
                }

                components.add(component);
            }
        }

        return components;
    }

    /**
     * Find all JPA entities.
     */
    public List<Map<String, Object>> findEntities(boolean includeMethods) throws CoreException {
        List<Map<String, Object>> entities = new ArrayList<>();
        Set<String> processedTypes = new HashSet<>();

        // Search for types with @Entity annotation
        List<IType> entityTypes = new ArrayList<>();
        entityTypes.addAll(findTypesWithAnnotation("javax.persistence.Entity"));
        entityTypes.addAll(findTypesWithAnnotation("jakarta.persistence.Entity"));

        for (IType type : entityTypes) {
            String fullyQualifiedName = type.getFullyQualifiedName();
            // Skip if already processed (to avoid duplicates)
            if (processedTypes.contains(fullyQualifiedName)) {
                continue;
            }
            processedTypes.add(fullyQualifiedName);
            
            Map<String, Object> entity = new HashMap<>();
            entity.put("classname", fullyQualifiedName);

            if (type.getResource() != null) {
                entity.put("path", type.getResource().getProjectRelativePath().toString());
            }

            if (includeMethods) {
                StringBuilder methods = new StringBuilder();
                for (IMethod method : type.getMethods()) {
                    if (methods.length() > 0) {
                        methods.append(",");
                    }
                    methods.append(getMethodSignature(method));
                }
                entity.put("methods", methods.toString());
            }

            entities.add(entity);
        }

        return entities;
    }

    /**
     * Find Spring Security configurations.
     */
    public List<Map<String, Object>> findSecurityConfigurations() throws CoreException {
        List<Map<String, Object>> configs = new ArrayList<>();

        // Search for @EnableWebSecurity or @Configuration with security beans
        List<IType> securityTypes = findTypesWithAnnotation(
                "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity");

        for (IType type : securityTypes) {
            Map<String, Object> config = new HashMap<>();
            config.put("class", type.getFullyQualifiedName());
            config.put("origin", "project");
            config.put("source", "java");

            if (type.getResource() != null) {
                config.put("path", type.getResource().getProjectRelativePath().toString());
            }

            // Try to find login view from annotations or method returns
            String loginView = findLoginView(type);
            if (loginView != null) {
                config.put("loginView", loginView);
            }

            configs.add(config);
        }

        return configs;
    }

    /**
     * Find UserDetailsService implementations.
     */
    public List<Map<String, Object>> findUserDetailsServices() throws CoreException {
        List<Map<String, Object>> services = new ArrayList<>();

        // Find UserDetailsService interface
        IType userDetailsServiceType = javaProject
                .findType("org.springframework.security.core.userdetails.UserDetailsService");
        if (userDetailsServiceType == null) {
            return services;
        }

        // Search for all implementations
        ITypeHierarchy hierarchy = userDetailsServiceType.newTypeHierarchy(javaProject, null);
        IType[] implementations = hierarchy.getAllSubtypes(userDetailsServiceType);

        for (IType type : implementations) {
            // Only include project types
            if (type.getResource() != null && type.getResource().getProject().equals(javaProject.getProject())) {
                Map<String, Object> service = new HashMap<>();
                service.put("class", type.getFullyQualifiedName());
                service.put("origin", "project");
                service.put("source", "java");

                if (type.getResource() != null) {
                    service.put("path", type.getResource().getProjectRelativePath().toString());
                }

                // Try to find related entity classes
                String entities = findRelatedEntities(type);
                if (entities != null) {
                    service.put("entity", entities);
                }

                services.add(service);
            }
        }

        return services;
    }

    /**
     * Helper method to find types with a specific annotation.
     */
    private List<IType> findTypesWithAnnotation(String annotationName) throws CoreException {
        List<IType> types = new ArrayList<>();

        // Search all compilation units in the project
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment pkg : packages) {
            if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
                for (ICompilationUnit unit : pkg.getCompilationUnits()) {
                    // Use getTypes() to get only top-level types first
                    IType[] topLevelTypes = unit.getTypes();
                    for (IType type : topLevelTypes) {
                        // Check the top-level type
                        if (hasAnnotation(type, annotationName)) {
                            types.add(type);
                        }
                        // Check nested types
                        checkNestedTypes(type, annotationName, types);
                    }
                }
            }
        }

        return types;
    }

    /**
     * Recursively check nested types for annotations.
     */
    private void checkNestedTypes(IType type, String annotationName, List<IType> types) throws JavaModelException {
        IType[] nestedTypes = type.getTypes();
        for (IType nested : nestedTypes) {
            if (hasAnnotation(nested, annotationName)) {
                types.add(nested);
            }
            // Recursively check deeper nested types
            checkNestedTypes(nested, annotationName, types);
        }
    }

    /**
     * Check if a type has a specific annotation.
     */
    private boolean hasAnnotation(IType type, String annotationName) throws JavaModelException {
        IAnnotation[] annotations = type.getAnnotations();
        String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);

        for (IAnnotation annotation : annotations) {
            String name = annotation.getElementName();
            // Check both simple name and fully qualified name
            if (name.equals(simpleName) || name.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get annotation value for a specific attribute.
     */
    private String getAnnotationValue(IType type, String annotationName, String attributeName)
            throws JavaModelException {
        IAnnotation[] annotations = type.getAnnotations();
        String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);

        for (IAnnotation annotation : annotations) {
            String name = annotation.getElementName();
            if (name.equals(simpleName) || name.equals(annotationName)) {
                org.eclipse.jdt.core.IMemberValuePair[] pairs = annotation.getMemberValuePairs();
                if (pairs != null && pairs.length > 0) {
                    // Look for the specific attribute
                    for (org.eclipse.jdt.core.IMemberValuePair pair : pairs) {
                        if (pair.getMemberName().equals(attributeName)) {
                            Object value = pair.getValue();
                            if (value != null) {
                                return value.toString().replaceAll("\"", "");
                            }
                        }
                    }
                    // If attribute not found, try first pair's value if it's "value"
                    if (attributeName.equals("value") && pairs.length > 0 && pairs[0].getMemberName().equals("value")) {
                        Object value = pairs[0].getValue();
                        if (value != null) {
                            return value.toString().replaceAll("\"", "");
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get method signature in a readable format.
     */
    private String getMethodSignature(IMethod method) throws JavaModelException {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getElementName());
        signature.append("(");

        String[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            signature.append(Signature.getSignatureSimpleName(parameterTypes[i]));
        }

        signature.append(")");
        return signature.toString();
    }

    /**
     * Try to find login view configuration.
     */
    private String findLoginView(IType type) throws JavaModelException {
        // Look for methods that might configure login view
        for (IMethod method : type.getMethods()) {
            // This is simplified - would need more sophisticated analysis
            if (method.getElementName().contains("configure") || method.getElementName().contains("formLogin")) {
                // Would need to parse method body to find actual login view
                return "/login"; // Default assumption
            }
        }
        return null;
    }

    /**
     * Find entities related to a UserDetailsService.
     */
    private String findRelatedEntities(IType type) throws JavaModelException {
        List<String> entities = new ArrayList<>();

        // Look for fields that might be entity types
        for (IJavaElement element : type.getChildren()) {
            if (element.getElementType() == IJavaElement.FIELD) {
                // This is simplified - would need to check field types
                String fieldName = element.getElementName();
                if (fieldName.contains("User") || fieldName.contains("Role")) {
                    entities.add(fieldName);
                }
            }
        }

        return entities.isEmpty() ? null : String.join(",", entities);
    }
}
