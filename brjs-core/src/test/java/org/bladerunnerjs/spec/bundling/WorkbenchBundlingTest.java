package org.bladerunnerjs.spec.bundling;

import org.bladerunnerjs.model.App;
import org.bladerunnerjs.model.Aspect;
import org.bladerunnerjs.model.Blade;
import org.bladerunnerjs.model.Bladeset;
import org.bladerunnerjs.model.NamedDirNode;
import org.bladerunnerjs.model.Theme;
import org.bladerunnerjs.model.Workbench;
import org.bladerunnerjs.specutil.engine.SpecTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class WorkbenchBundlingTest extends SpecTest {
	private App app;
	private Aspect aspect;
	private Theme standardAspectTheme, standardBladesetTheme, standardBladeTheme;
	private Bladeset bladeset;
	private Blade blade;
	private Workbench workbench;
	private NamedDirNode workbenchTemplate;
	private StringBuffer response;
	
	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs).automaticallyFindsBundlers()
			.and(brjs).automaticallyFindsMinifiers()
			.and(brjs).hasBeenCreated();

		app = brjs.app("app1");
		aspect = app.aspect("default");
		standardAspectTheme = aspect.theme("standard");
		bladeset = app.bladeset("bs");
		standardBladesetTheme = bladeset.theme("standard");
		blade = bladeset.blade("b1");
		standardBladeTheme = blade.theme("standard");
		workbench = blade.workbench();
		
		response = new StringBuffer();

		given(workbenchTemplate).containsFileWithContents("index.html", "'<html>hello world</html>'")
			.and(workbenchTemplate).containsFolder("resources")
			.and(workbenchTemplate).containsFolder("src");
	}

	// ----------------------------------- C S S  -------------------------------------
	// TODO enable when we work on CSS Bundler
	@Ignore 
 	@Test
 	public void aspectCssFilesAreBundledInTheWorkbench() throws Exception {
		given(standardAspectTheme).containsFileWithContents("style.css", "ASPECT theme content");
 		when(app).requestReceived("/bs-bladeset/blades/b1/workbench/css/standard_css.bundle", response);
 		then(response).containsText("ASPECT theme content");
 	}
	
	@Ignore 
 	@Test
 	public void bladesetCssFilesAreBundledWhenReferencedInTheWorkbench() throws Exception {
		given(bladeset).hasPackageStyle("src/novox/bs", "caplin-js")
			.and(bladeset).hasClass("novox.bs.Class1")
			.and(standardBladesetTheme).containsFileWithContents("style.css", "BLADESET theme content")
			.and(workbench).indexPageRefersTo("novox.bs.Class1");
		when(app).requestReceived("/bs-bladeset/blades/b1/workbench/css/standard_css.bundle", response);
 		then(response).containsText("BLADESET theme content");
 	}
	
	@Ignore 
 	@Test
 	public void bladeCssFilesAreBundledWhenReferencedInTheWorkbench() throws Exception {
		given(blade).hasPackageStyle("src/novox/bs/b1", "caplin-js")
			.and(blade).hasClass("novox.bs.b1.Class1")
			.and(standardBladeTheme).containsFileWithContents("style.css", "BLADE theme content")
			.and(workbench).indexPageRefersTo("novox.bs.b1.Class1");
		when(app).requestReceived("/bs-bladeset/blades/b1/workbench/css/standard_css.bundle", response);
 		then(response).containsText("BLADE theme content");
 	}
	
	// TODO This was the previous behaviour for bladerunner - this will now be opt-in?
	@Ignore
	@Test
	public void sdkLibCssFilesAreNotBundledAsCommonCssInTheWorkbenchWhenNotReferenced() throws Exception {

	}
	
}