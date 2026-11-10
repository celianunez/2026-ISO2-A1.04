package es.uclm.sescam.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Architecture rules checked on every build (DECISIONS.md D-03, D-05, D-14).
 *
 * <p>A module is the first package segment after {@code es.uclm.sescam}
 * ({@code es.uclm.sescam.scheduling..} is module {@code scheduling}). The rules are generic, so they
 * keep working when the {@code radio-*} modules replace the configuration-test modules.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT = "es.uclm.sescam";

    /** D-03: another module may only be used through its {@code api} package. */
    @ArchTest
    static final ArchRule modulesTalkOnlyThroughApi = classes()
            .that().resideInAPackage(ROOT + "..")
            .should(useOtherModulesOnlyThroughTheirApi())
            .allowEmptyShould(true);

    /** D-05: no cycles between modules. */
    @ArchTest
    static final ArchRule noCyclesBetweenModules = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    private static ArchCondition<JavaClass> useOtherModulesOnlyThroughTheirApi() {
        return new ArchCondition<>("use other modules only through their api package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originModule = moduleOf(origin.getPackageName());
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String targetModule = moduleOf(targetPackage);
                    if (targetModule == null || targetModule.equals(originModule)) {
                        continue;
                    }
                    String allowed = ROOT + "." + targetModule + ".api";
                    if (!targetPackage.equals(allowed) && !targetPackage.startsWith(allowed + ".")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** Returns the module of a package, or {@code null} if the package is outside the project. */
    private static String moduleOf(String packageName) {
        if (!packageName.startsWith(ROOT + ".")) {
            return null;
        }
        String rest = packageName.substring(ROOT.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
