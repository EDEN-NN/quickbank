package com.quickbank.bootstrap;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ModuleBoundariesTest {

    private static final String[] MODULES = {
            "com.quickbank.account..",
            "com.quickbank.ledger..",
            "com.quickbank.payment..",
            "com.quickbank.notification..",
            "com.quickbank.observability..",
            "com.quickbank.bootstrap.."
    };

    @Test
    void domainModulesDoNotDependOnEachOther() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.quickbank");
        for (String source : Arrays.copyOf(MODULES, MODULES.length - 1)) {
            String[] forbidden = Arrays.stream(MODULES).filter(pkg -> !pkg.equals(source)).toArray(String[]::new);
            classes().that().resideInAPackage(source)
                    .should().onlyDependOnClassesThat().resideOutsideOfPackages(forbidden)
                    .check(classes);
        }
        classes().that().resideInAPackage("com.quickbank.common..")
                .should().onlyDependOnClassesThat().resideOutsideOfPackages(MODULES)
                .check(classes);
    }
}
