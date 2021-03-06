package org.bladerunnerjs.spec.command;

import static org.bladerunnerjs.plugin.bundlers.aliasing.AliasingUtility.aliasDefinitionsFile;
import static org.bladerunnerjs.plugin.bundlers.aliasing.AliasingUtility.aliasesFile;

import org.bladerunnerjs.api.App;
import org.bladerunnerjs.api.Aspect;
import org.bladerunnerjs.api.Blade;
import org.bladerunnerjs.api.JsLib;
import org.bladerunnerjs.api.model.exception.UnresolvableRequirePathException;
import org.bladerunnerjs.api.model.exception.command.ArgumentParsingException;
import org.bladerunnerjs.api.model.exception.command.CommandArgumentsException;
import org.bladerunnerjs.api.model.exception.command.NodeDoesNotExistException;
import org.bladerunnerjs.api.spec.engine.SpecTest;
import org.bladerunnerjs.plugin.commands.standard.DepInsightCommand;
import org.bladerunnerjs.plugin.plugins.require.AliasDataSourceModule;
import org.bladerunnerjs.spec.aliasing.AliasDefinitionsFileBuilder;
import org.bladerunnerjs.spec.aliasing.AliasesFileBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class DepInsightCommandTest extends SpecTest {
	App app;
	Aspect aspect;
	private Blade bladeInDefaultBladeset;
	private Aspect defaultAspect;
	private AliasesFileBuilder aspectAliasesFileBuilder;
	private AliasDefinitionsFileBuilder bladeAliasDefinitionsFileBuilder;
	
	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs).hasCommandPlugins(new DepInsightCommand())
			.and(brjs).automaticallyFindsAssetPlugins()
			.and(brjs).automaticallyFindsRequirePlugins()
			.and(brjs).hasBeenCreated();
			app = brjs.app("app");
			aspect = app.aspect("default");
			defaultAspect = app.defaultAspect();
			bladeInDefaultBladeset = app.defaultBladeset().blade("b1");
			
			aspectAliasesFileBuilder = new AliasesFileBuilder(this, aliasesFile(aspect));
			bladeAliasDefinitionsFileBuilder = new AliasDefinitionsFileBuilder(this, aliasDefinitionsFile(bladeInDefaultBladeset, "src"));
			
			JsLib servicesLib = brjs.sdkLib("ServicesLib");
			given(servicesLib).containsFileWithContents("br-lib.conf", "requirePrefix: br")
				.and(servicesLib).hasClasses("br/AliasRegistry", "br/ServiceRegistry");
	}
	
	@Test
	public void exceptionIsThrownIfTheAppNameIsNotProvided() throws Exception {
		when(brjs).runCommand("dep-insight");
		then(exceptions).verifyException(ArgumentParsingException.class, unquoted("Parameter 'app-name' is required"))
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}
	
	@Test
	public void exceptionIsThrownIfTheRequirePathIsNotProvided() throws Exception {
		when(brjs).runCommand("dep-insight", "app");
		then(exceptions).verifyException(ArgumentParsingException.class, unquoted("Parameter 'require-path' is required"))
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}
	
	@Test
	public void exceptionIsThrownIfThereAreTooManyArguments() throws Exception {
		when(brjs).runCommand("dep-insight", "a", "b", "c", "d");
		then(exceptions).verifyException(ArgumentParsingException.class, unquoted("Unexpected argument: d"))
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}
	
	@Test
	public void exceptionIsThrownIfTheAppDoesntExist() throws Exception {
		when(brjs).runCommand("dep-insight", "app", "require-path");
		then(exceptions).verifyException(NodeDoesNotExistException.class, unquoted(app.getClass().getSimpleName()))
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}
	
	@Test
	public void exceptionIsThrownIfTheDefaultAspectDoesntExist() throws Exception {
		given(app).hasBeenCreated();
		when(brjs).runCommand("dep-insight", "app", "require-path");
		then(exceptions).verifyException(NodeDoesNotExistException.class, unquoted(aspect.getClass().getSimpleName()), "default")
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}
	
	@Test
	public void exceptionIsThrownIfTheNamedAspectDoesntExist() throws Exception {
		given(app).hasBeenCreated();
		when(brjs).runCommand("dep-insight", "app", "require-path", "aspect");
		then(exceptions).verifyException(NodeDoesNotExistException.class, unquoted(aspect.getClass().getSimpleName()), "aspect")
			.whereTopLevelExceptionIs(CommandArgumentsException.class);
	}
	
	@Test
	public void exceptionIsThrownIfPrefixAndAliasSwitchesAreUsedSimultaneously() throws Exception {
		given(aspect).hasBeenCreated();
		when(brjs).runCommand("dep-insight", "app", "require-path", "--prefix", "--alias");
		then(exceptions).verifyException(CommandArgumentsException.class, unquoted("The --prefix and --alias switches can't both be used at the same time"));
	}
	
	@Test
	public void commandIsAutomaticallyLoaded() throws Exception
	{
		given(brjs).hasBeenAuthenticallyCreated()
			.and(aspect).hasClass("appns/Class");
		when(brjs).runCommand("dep-insight", "app", "appns/Class");
		then(exceptions).verifyNoOutstandingExceptions();
	}
	
	@Test
	public void dependenciesAreShownWhenAllArgumentsAreValid() throws Exception {
		given(aspect).indexPageRequires("appns/Class1")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2")
			.and(aspect).classRequires("appns/Class1", "./Class2");
		when(brjs).runCommand("dep-insight", "app", "appns/Class2");
		then(logging).containsConsoleText(
				"Source module 'appns/Class2' dependencies found:",
				"    +--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class1.js'",
				"    +--- 'default-aspect/src/appns/Class2.js'");
	}
	
	@Test
	public void dependenciesAreShownForNamespacedClassesToo() throws Exception {
		given(aspect).hasNamespacedJsPackageStyle()
			.and(aspect).indexPageRequires("appns/Class1")
			.and(aspect).hasClasses("appns.Class1", "appns.Class2")
			.and(aspect).classDependsOn("appns.Class1", "appns.Class2");
		when(brjs).runCommand("dep-insight", "app", "appns/Class2");
		then(logging).containsConsoleText(
				"Source module 'appns/Class2' dependencies found:",
				"    +--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class1.js'",
				"    +--- 'default-aspect/src/appns/Class2.js'");
	}
	
	@Test
	public void onlyDependenciesThatAreToBeBundledAreShown() throws Exception {
		given(aspect).indexPageRequires("appns/Class2")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2")
			.and(aspect).classRequires("appns/Class1", "./Class2");
		when(brjs).runCommand("dep-insight", "app", "appns/Class2");
		then(logging).containsConsoleText(
			"Source module 'appns/Class2' dependencies found:",
			"    +--- 'default-aspect/index.html' (seed file)",
			"    +--- 'default-aspect/src/appns/Class2.js'");

	}
	
	@Test
	public void ifTheSourceModuleBeingInspectedIsntToBeBundledThenAllDependenciesAreShown() throws Exception {
		given(aspect).indexPageRequires("appns/Class3")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2", "appns/Class3")
			.and(aspect).classRequires("appns/Class1", "./Class2");
		when(brjs).runCommand("dep-insight", "app", "appns/Class2");
		then(logging).containsConsoleText(
				"Source module 'appns/Class2' dependencies found:",
				"    +--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class3.js'");
	}
	
	@Test
	public void requestingDependenciesForANonExistentSourceModuleProvidesANiceMessage() throws Exception {
		given(aspect).hasBeenCreated();
		when(brjs).runCommand("dep-insight", "app", "NonExistentClass");
		then(logging).containsConsoleText(
			"Source file 'NonExistentClass' could not be found.");
	}
	
	@Test
	public void resourceDependenciesAreShownAheadOfClassDependenciesSinceTheyReflectUltimateLeafNodesOfGreaterImportanceToTheUser() throws Exception {
		given(aspect).indexPageRequires("appns/Class1")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2")
			.and(aspect).classRequires("appns/Class1", "./Class2")
			.and(aspect).containsResourceFileWithContents("config.xml", "'appns/Class2'");
		when(brjs).runCommand("dep-insight", "app", "appns/Class2");
		then(logging).containsConsoleText(
        	"Source module 'appns/Class2' dependencies found:",
        	"    +--- 'default-aspect/index.html' (seed file)",
        	"    +--- 'default-aspect/src/appns/Class1.js'",
        	"    +--- 'default-aspect/src/appns/Class2.js'",
        	"    |    \\--- 'default-aspect/resources/config.xml' (seed file)");

	}
	
	@Test
	public void byDefaultDependenciesAreOnlyShownTheFirstTimeTheyAreEncountered() throws Exception {
		given(aspect).indexPageRequires("appns/Class1")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2")
			.and(aspect).classRequires("appns/Class1", "./Class2")
			.and(aspect).classRequires("appns/Class2", "./Class3")
			.and(aspect).classRequiresAtUseTime("appns/Class3", "./Class1");
		when(brjs).runCommand("dep-insight", "app", "appns/Class3");
		then(logging).containsConsoleText(
			"Source module 'appns/Class3' dependencies found:",
			"    +--- 'default-aspect/index.html' (seed file)",
			"    +--- 'default-aspect/src/appns/Class1.js'",
			"    |    \\--- 'default-aspect/src/appns/Class3.js' (*)",
			"    |    |    \\--- 'default-aspect/src/appns/Class2.js'",
			"",
			"    (*) - subsequent instances not shown (use -A or --all to show)");
	}
	
	@Test
	public void whenUsingTheAllSwitchIfTheSameAssetIsFoundTwiceThenItsDependenciesAreOnlyShownTheFirstTime() throws Exception {
		given(aspect).indexPageRequires("appns/Class1")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2")
			.and(aspect).classRequires("appns/Class1", "./Class2")
			.and(aspect).classRequires("appns/Class2", "./Class3")
			.and(aspect).classRequiresAtUseTime("appns/Class3", "./Class1");
		when(brjs).runCommand("dep-insight", "app", "appns/Class3", "--all");
		then(logging).containsConsoleText(
				"Source module 'appns/Class3' dependencies found:",
				"    +--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class1.js'",
				"    |    \\--- 'default-aspect/index.html' (seed file) (*)",
				"    |    \\--- 'default-aspect/src/appns/Class3.js'",
				"    |    |    \\--- 'default-aspect/src/appns/Class2.js'",
				"    |    |    |    \\--- 'default-aspect/src/appns/Class1.js' (*)",
				"    +--- 'default-aspect/src/appns/Class2.js' (*)",
				"    +--- 'default-aspect/src/appns/Class3.js' (*)",
				"",
				"    (*) - dependencies omitted (listed previously)");
	}
	
	@Test
	public void dependenciesThatOccurDueToRelatedResourcesAreShownCorrectly() throws Exception {
		given(aspect).indexPageRequires("appns/Class1")
			.and(aspect).hasClasses("appns/Class1", "appns/Class2", "appns/pkg/InnerClass")
			.and(aspect).classRequires("appns/Class1", "./pkg/InnerClass")
			.and(aspect).containsFileWithContents("src/appns/pkg/config.xml", "'appns/Class2'")
			.and(aspect).containsEmptyFile("empty-config.xml");
		when(brjs).runCommand("dep-insight", "app", "appns/Class2");
		then(logging).containsConsoleText(
				"Source module 'appns/Class2' dependencies found:",
				"    +--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class1.js'",
				"    +--- 'default-aspect/src/appns/pkg/InnerClass.js'",
				"    +--- 'default-aspect/src/appns/pkg/config.xml'",
				"    +--- 'default-aspect/src/appns/Class2.js'");
	}
	
	@Test
	public void requirePrefixDependenciesAreCorrectlyShown() throws Exception {
		given(aspect).indexPageRequires("appns/pkg1/ClassA")
			.and(aspect).hasClasses("appns/pkg1/ClassA", "appns/pkg1/ClassB", "appns/pkg1/UnbundledClass", "appns/pkg2/ClassC")
			.and(aspect).classRequiresAtUseTime("appns/pkg1/ClassA", "../pkg2/ClassC")
			.and(aspect).classRequiresAtUseTime("appns/pkg2/ClassC", "../pkg1/ClassB");
		when(brjs).runCommand("dep-insight", "app", "appns/pkg1", "--prefix", "--all");
		then(logging).containsConsoleText(
				"Require path prefix 'appns/pkg1' dependencies found:",
				"    +--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/pkg1/ClassA.js'",
				"    |    \\--- 'default-aspect/index.html' (seed file) (*)",
				"    +--- 'default-aspect/src/appns/pkg2/ClassC.js'",
				"    |    \\--- 'default-aspect/src/appns/pkg1/ClassA.js' (*)",
				"    +--- 'default-aspect/src/appns/pkg1/ClassB.js'",
				"    |    \\--- 'default-aspect/src/appns/pkg2/ClassC.js' (*)",
				"",
				"    (*) - dependencies omitted (listed previously)");
	}
	
	@Test
	public void aliasedDependenciesAreCorrectlyDisplayed() throws Exception {
		given(aspect).indexPageHasAliasReferences("alias-ref")
			.and(aspectAliasesFileBuilder).hasAlias("alias-ref", "appns.Class")
			.and(aspect).hasClass("appns/Class");
		when(brjs).runCommand("dep-insight", "app", "appns/Class");
		then(logging).containsConsoleText(
			"    +--- '../../libs/javascript/ServicesLib/src/br/AliasRegistry.js'",
			"    |    \\--- 'alias!alias-ref' (alias dep.)",
			"    |    |    \\--- 'default-aspect/index.html' (seed file)",
			"    +--- 'default-aspect/src/appns/Class.js'",
			"    +--- '" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "' (alias dep.)");
	}
	
	@Test
	public void weCanShowDependenciesForAnAliasToo() throws Exception {
		given(aspect).indexPageHasAliasReferences("alias-ref")
			.and(aspectAliasesFileBuilder).hasAlias("alias-ref", "appns.Class")
			.and(aspect).hasClass("appns/Class");
		when(brjs).runCommand("dep-insight", "app", "alias-ref", "--alias");
		then(logging).containsConsoleText(
				"Source module 'alias!alias-ref' dependencies found:",
				"    +--- '../../libs/javascript/ServicesLib/src/br/AliasRegistry.js'",
			    "    |    \\--- 'alias!alias-ref' (alias dep.)",
			    "    |    |    \\--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class.js'",
				"    +--- '" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "' (alias dep.)");
	}
	
	@Test
	public void anAliasNameWithASpaceIsntMistakenlyRecognizedAsAnAspect() throws Exception {
		given(aspect).indexPageHasAliasReferences("alias ref")
			.and(aspectAliasesFileBuilder).hasAlias("alias ref", "appns.Class")
			.and(aspect).hasClass("appns/Class");
		when(brjs).runCommand("dep-insight", "app", "alias ref", "--alias");
		then(logging).containsConsoleText(
				"Source module 'alias!alias ref' dependencies found:",
				"    +--- '../../libs/javascript/ServicesLib/src/br/AliasRegistry.js'",
			    "    |    \\--- 'alias!alias ref' (alias dep.)",
				"    |    |    \\--- 'default-aspect/index.html' (seed file)",
				"    +--- 'default-aspect/src/appns/Class.js'",
				"    +--- '" + AliasDataSourceModule.PRIMARY_REQUIRE_PATH + "' (alias dep.)");
	}
	
	@Ignore
	@Test
	public void dependenciesCanBeShownForAnIncompleteAlias() throws Exception {
		given(aspect).indexPageHasAliasReferences("appns.bs.b1.alias-ref")
			.and(aspect).hasClasses("appns.TheClass", "appns.TheInterface")
			.and(bladeAliasDefinitionsFileBuilder).hasAlias("appns.bs.b1.alias-ref", null, "appns.TheInterface");
		when(brjs).runCommand("dep-insight", "app", "appns.bs.b1.alias-ref", "--alias");
		then(logging).containsConsoleText(
			"Alias 'appns.bs.b1.alias-ref' dependencies found:",
			"    +--- 'default-aspect/src/appns/TheInterface.js'",
			"    |    \\--- 'alias!appns.bs.b1.alias-ref' (alias dep.)",
			"    |    |    \\--- 'default-aspect/index.html' (seed file)");
	}
	
	@Test
	public void dependenciesCanBeShownForAnIncompleteAliasThatIsntUsedWithinTheApp() throws Exception {
		given(brjs.sdkLib("br")).hasClasses("br/UnknownClass")
			.and(aspect).hasClass("appns/TheInterface")
			.and(bladeAliasDefinitionsFileBuilder).hasAlias("appns.b1.alias-ref", null, "appns.TheInterface");
		when(brjs).runCommand("dep-insight", "app", "appns.b1.alias-ref", "--alias");
		then(logging).containsConsoleText(
			"Source module 'alias!appns.b1.alias-ref' dependencies found:");
	}
	
	@Test
	public void requestingDependenciesForANonExistentAliasProvidesANiceMessage() throws Exception {
		given(aspect).hasBeenCreated();
		when(brjs).runCommand("dep-insight", "app", "alias-ref", "--alias");
		then(logging).containsConsoleText(
			"Source file 'alias!alias-ref' could not be found.");
	}
	
	@Test
	public void requestingDependenciesForAnAliasThatPointsToANonExistentSourceModuleProvidesANiceMessage() throws Exception {
		given(aspectAliasesFileBuilder).hasAlias("alias-ref", "NonExistentClass");
		when(brjs).runCommand("dep-insight", "app", "alias-ref", "--alias");
		then(exceptions).verifyException(UnresolvableRequirePathException.class, "NonExistentClass");
	}
	
	@Test
	public void optionalPackageStructuresAreShownCorrectly() throws Exception {
		given(aspect).indexPageRequires("appns/bs/b1/Class1")
			.and(app.bladeset("bs").blade("b1")).hasClasses("Class1");
		when(brjs).runCommand("dep-insight", "app", "appns/bs/b1/Class1");
		then(logging).containsConsoleText(
			"Source module 'appns/bs/b1/Class1' dependencies found:",
			"    +--- 'bs-bladeset/blades/b1/src/Class1.js'",
			"    |    \\--- 'default-aspect/index.html'");
	}
	
	@Test
	public void optionalBladesetsAreShownCorrectly() throws Exception {
		given(aspect).indexPageRequires("appns/b1/Class1")
			.and(bladeInDefaultBladeset).hasClasses("appns/b1/Class1");
		when(brjs).runCommand("dep-insight", "app", "appns/b1/Class1");
		then(logging).containsConsoleText(
    		"Source module 'appns/b1/Class1' dependencies found:",
    		"    +--- 'blades/b1/src/appns/b1/Class1.js'",
    		"    |    \\--- 'default-aspect/index.html' (seed file)");
	}

	@Test
	public void optionalAspectAreShownCorrectly() throws Exception {
		given(defaultAspect).indexPageRequires("appns/Class1")
			.and(defaultAspect).hasClasses("appns/Class1");
		when(brjs).runCommand("dep-insight", "app", "appns/Class1");
		then(logging).containsConsoleText(
			"Source module 'appns/Class1' dependencies found:",
			"    +--- 'index.html' (seed file)",
			"    +--- 'src/appns/Class1.js'");
	}
	
}
