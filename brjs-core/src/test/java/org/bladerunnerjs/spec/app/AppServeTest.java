package org.bladerunnerjs.spec.app;

import org.bladerunnerjs.model.App;
import org.bladerunnerjs.model.Aspect;
import org.bladerunnerjs.model.DirNode;
import org.bladerunnerjs.model.Workbench;
import org.bladerunnerjs.model.exception.request.ResourceNotFoundException;
import org.bladerunnerjs.spec.brjs.appserver.MockTagHandler;
import org.bladerunnerjs.testing.specutility.engine.SpecTest;
import org.bladerunnerjs.testing.utility.MockContentPlugin;
import org.junit.Before;
import org.junit.Test;

public class AppServeTest extends SpecTest {
	private App app;
	private Aspect defaultAspect;
	private Aspect alternateAspect;
	private Workbench workbench;
	private StringBuffer response = new StringBuffer();
	private DirNode sdkLibsDir;
	
	@Before
	public void initTestObjects() throws Exception
	{
		given(brjs).automaticallyFindsAssetLocationPlugins()
			.and(brjs).automaticallyFindsAssetPlugins()
			.and(brjs).automaticallyFindsContentPlugins()
			.and(brjs).hasTagHandlerPlugins(new MockTagHandler("tagToken", "dev replacement", "prod replacement", false), new MockTagHandler("localeToken", "", "", true))
			.and(brjs).hasContentPlugins(new MockContentPlugin())
			.and(brjs).hasBeenCreated();
			app = brjs.app("app1");
			defaultAspect = app.aspect("default");
			alternateAspect = app.aspect("alternate");
			workbench = app.bladeset("bs").blade("b1").workbench();
			sdkLibsDir = brjs.sdkLibsDir();
	}
	
	@Test
	public void localeForwardingPageIsReturnedIfNoLocaleIsSpecified() throws Exception {
		given(defaultAspect).indexPageHasContent("index page")
			.and(sdkLibsDir).containsFileWithContents("locale-forwarder.js", "locale forwarding page");
		when(app).requestReceived("zz/", response);
		then(exceptions).verifyException(ResourceNotFoundException.class, "zz");
	}
	
	@Test
	public void indexPageCanBeAccessed() throws Exception {
		given(defaultAspect).indexPageHasContent("index page")
			.and(sdkLibsDir).containsFile("locale-forwarder.js");
		when(app).requestReceived("en/", response);
		then(response).textEquals("index page");
	}
	
	@Test
	public void invalidLocalePagesThrowAnException() throws Exception {
		given(defaultAspect).indexPageHasContent("index page")
			.and(sdkLibsDir).containsFile("locale-forwarder.js");
		when(app).requestReceived("en/", response);
		then(response).textEquals("index page");
	}
	
	@Test
	public void tagsWithinIndexPagesAreProcessed() throws Exception {
		given(defaultAspect).indexPageHasContent("<@tagToken @/>")
			.and(sdkLibsDir).containsFile("locale-forwarder.js");
		when(app).requestReceived("en/", response);
		then(response).textEquals("dev replacement");
	}
	
	@Test
	public void localesCanBeUsedInTagHandlers() throws Exception {
		given(defaultAspect).indexPageHasContent("<@localeToken @/>")
			.and(sdkLibsDir).containsFile("locale-forwarder.js")
			.and(app).hasSupportedLocales("en_GB");
		when(app).requestReceived("en_GB/", response);
		then(response).textEquals("- en_GB");
	}
	
	@Test
	public void workbenchPageCanBeAccessed() throws Exception {
		given(workbench).indexPageHasContent("workbench index page")
			.and(sdkLibsDir).containsFile("locale-forwarder.js");
		when(app).requestReceived("workbench/bs/b1/en/", response);
		then(response).textEquals("workbench index page");
	}
	
	@Test
	public void defaultAspectBundlesCanBeRequested() throws Exception {
		given(defaultAspect).indexPageRequires("appns/SomeClass")
			.and(defaultAspect).hasClass("appns/SomeClass")
			.and(defaultAspect).containsFileWithContents("src/appns/template.html", "<div id='template-id'>template file</div>");
		when(app).requestReceived("v/dev/html/bundle.html", response);
		then(response).containsText("template file");
	}
	
	@Test
	public void alternateAspectBundlesCanBeRequested() throws Exception {
		given(alternateAspect).indexPageRequires("appns/SomeClass")
			.and(alternateAspect).hasClass("appns/SomeClass")
			.and(alternateAspect).containsFileWithContents("src/appns/template.html", "<div id='template-id'>template file</div>");
		when(app).requestReceived("alternate/v/dev/html/bundle.html", response);
		then(response).containsText("template file");
	}
	
	@Test
	public void workbenchBundlesCanBeRequested() throws Exception {
		given(workbench).indexPageRequires("appns/SomeClass")
			.and(workbench).hasClass("appns/SomeClass")
			.and(workbench).containsFileWithContents("src/appns/template.html", "<div id='template-id'>workbench template file</div>");
		when(app).requestReceived("workbench/bs/b1/v/dev/html/bundle.html", response);
		then(response).containsText("workbench template file");
	}
	
	@Test
	public void contentPluginsCanDefineNonVersionedUrls() throws Exception
	{
		given(app).hasBeenPopulated();
		when(app).requestReceived("static/mock-content-plugin/unversioned/url", response);
		then(response).containsText(MockContentPlugin.class.getCanonicalName());
	}
}
