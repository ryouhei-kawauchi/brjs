package org.bladerunnerjs.spec.model;

import org.bladerunnerjs.api.App;
import org.bladerunnerjs.api.Bladeset;
import org.bladerunnerjs.api.model.events.NodeReadyEvent;
import org.bladerunnerjs.api.model.exception.DuplicateAssetContainerException;
import org.bladerunnerjs.api.model.exception.name.InvalidDirectoryNameException;
import org.bladerunnerjs.api.model.exception.name.InvalidPackageNameException;
import org.bladerunnerjs.api.spec.engine.SpecTest;
import org.bladerunnerjs.model.NamedDirNode;
import org.bladerunnerjs.model.engine.AbstractNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class BladesetTest extends SpecTest {
	App app;
	Bladeset bladeset;
	Bladeset invalidBladesetName;
	Bladeset JSKeywordBladesetName;
	Bladeset invalidPackageName;
	private NamedDirNode bladesetTemplate;
	
	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs).hasBeenCreated();
			app = brjs.app("app");
			bladesetTemplate = brjs.sdkTemplateGroup("default").template("bladeset");
			bladeset = app.bladeset("bs");
			invalidBladesetName = app.bladeset("#Invalid");
			JSKeywordBladesetName = app.bladeset("else");
			invalidPackageName = app.bladeset("_invalid");
	}
	
	@Test
	public void parentGivesTheCorrectNode() throws Exception {
		given(bladeset).hasBeenCreated();
		then(bladeset.parent()).isSameAs(app);
	}
	
	@Test
	public void dashBladesetIsApendedToBladeSetNode() throws Exception {
		when(bladeset).create();
		then(app).hasDir("bs-bladeset");
	}
	@Ignore //waiting for change to default appConf values, app namespace will be set to app name
	@Test
	public void bladesetIsBaselinedDuringPopulation() throws Exception {
		given(bladesetTemplate).containsFolder("@bladeset")
			.and(bladesetTemplate).containsFileWithContents("class1.js", "@appns.@bladeset = function() {};");
		when(bladeset).populate("default");
		then(bladeset).hasDir(bladeset.getName())
			.and(bladeset).doesNotHaveDir("@bladeset")
			.and(bladeset).fileHasContents("class1.js", "app.bs = function() {};");
	}
	
	@Test
	public void populatingABladesetCausesAppObserversToBeNotified() throws Exception
	{
		given(brjs.sdkTemplateGroup("default")).templateGroupCreated()
			.and(app).hasBeenPopulated("default")
			.and(observer).observing(app)
			.and(observer).allNotificationsHandled();
		when(bladeset).populate("default");
		then(observer).notified(NodeReadyEvent.class, bladeset)
			.and(observer).notified(NodeReadyEvent.class, bladeset.testType("unit").defaultTestTech());
	}
	
	@Test
	public void invalidBladesetDirectoryNameSpaceThrowsException() throws Exception {
		when(invalidBladesetName).populate("default");
		then(logging).errorMessageReceived(AbstractNode.Messages.NODE_CREATION_FAILED_LOG_MSG, "Bladeset", invalidBladesetName.dir())
			.and(exceptions).verifyException(InvalidDirectoryNameException.class,invalidBladesetName.dir(), "#Invalid");
	}
	
	@Test
	public void usingJSKeywordAsBladesetNameSpaceThrowsException() throws Exception {
		when(JSKeywordBladesetName).populate("default");
		then(logging).errorMessageReceived(AbstractNode.Messages.NODE_CREATION_FAILED_LOG_MSG, "Bladeset", JSKeywordBladesetName.dir())
			.and(exceptions).verifyException(InvalidPackageNameException.class,JSKeywordBladesetName.dir(), "else");
	}
	
	@Test
	public void invalidBladesetPackageNameSpaceThrowsException() throws Exception {
		when(invalidPackageName).populate("default");
		then(logging).errorMessageReceived(AbstractNode.Messages.NODE_CREATION_FAILED_LOG_MSG, "Bladeset", invalidPackageName.dir())
			.and(exceptions).verifyException(InvalidPackageNameException.class,invalidPackageName.dir(), "_invalid");
	}
	
	@Test
	public void defaultBladesetIsCorrectlyIdentified() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).hasDir("blades/myBlade");
		then(app.defaultBladeset()).dirExists()
			.and(app.defaultBladeset().blade("myBlade")).dirExists();
	}
	
	@Test
	public void defaultBladesetCanHaveItsOwnDirectory() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).hasDir("default-bladeset/blades/myBlade");
		then(app.defaultBladeset()).dirExists()
			.and(app.defaultBladeset().blade("myBlade")).dirExists();
	}
	
	@Test
	public void exceptionIsThrownIfThereAreTwoDefaultBladesets() throws Exception
	{
		given(app).hasBeenCreated()
			.and(app).hasDir("default-bladeset/blades/myBlade")
    		.and(app).hasDir("blades/myBlade");
		when(app).bladesetsListed();
		then(exceptions).verifyException(DuplicateAssetContainerException.class, "default Bladeset", "apps/app", "apps/app/default-bladeset");
	}
	
}