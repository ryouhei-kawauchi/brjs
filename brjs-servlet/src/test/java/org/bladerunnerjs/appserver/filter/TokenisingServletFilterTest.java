package org.bladerunnerjs.appserver.filter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.bladerunnerjs.appserver.util.JndiTokenFinder;
import org.bladerunnerjs.appserver.util.TokenReplacingReader;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TokenisingServletFilterTest extends ServletFilterTest
{
	private Context mockJndiContext;
	private Server appServer;
	private DummyServlet dummyServlet;
	private File tempFile = null;

	@Before
	public void setup() throws Exception
	{
		mockJndiContext = TestContextFactory.getTestContext();
		dummyServlet = new DummyServlet();
		appServer = createAndStartAppServer(dummyServlet, new TokenisingServletFilter(new JndiTokenFinder(mockJndiContext)));
	}

	@After
	public void teardown() throws Exception
	{
		verifyNoMoreInteractions(mockJndiContext);
		appServer.stop();
		if (tempFile != null) {
			FileUtils.deleteQuietly(tempFile);
		}
	}

	@Test
	public void basicTestForDummyServlet() throws Exception
	{
		dummyServlet.setResponseText("OK");
		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.xml");
		
		assertEquals("200", response.get("responseCode"));
		assertEquals("OK", response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}

	@Test
	public void testTextWithNoTokenIsUnchanged() throws Exception
	{
		String servletText = "I am some text, and I don't contain any tokens.";
		dummyServlet.setResponseText(servletText);

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.xml");
		assertEquals("200", response.get("responseCode"));
		assertEquals(servletText, response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}

	@Test
	public void testServletResponseCanContainHtml() throws Exception
	{
		String servletText = "<div><h1><a href='.'>This is some html<a></h1></div>";
		dummyServlet.setResponseText(servletText);
		dummyServlet.setContentType("text/html");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.xml");
		assertEquals("200", response.get("responseCode"));
		assertEquals(servletText, response.get("responseText"));
		assertEquals("text/html", response.get("responseContentType"));
	}

	@Test
	public void testJndiIsLookupPerformedForToken() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.xml");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}
	
	@Test
	public void testJndiIsLookupPerformedForTokenInLocalePage() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/en/");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}
	
	@Test
	public void testJndiIsLookupPerformedForTokenInLocaleAndLanguagePage() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/en_GB/");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test500ResponseCodeIfTokenCannotBeReplaced() throws Exception
	{
		dummyServlet.setResponseText("@A.NONEXISTANT.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.NONEXISTANT.TOKEN")).thenThrow(NamingException.class);

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.xml");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.NONEXISTANT.TOKEN");
		assertEquals("500", response.get("responseCode"));
	}
	
	@Test
	public void testTokenisingFilterProcessesJsFiles() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.js");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
	}
	
	@Test
	public void testTokenisingFilterProcessesJsonFiles() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.json");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
	}
	
	@Test
	public void tokenReplacementWorksForIndexPages() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}
	
	@Test
	public void tokenReplacementWorksForLocalizedIndexPages() throws Exception
	{
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/en_GB");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
		assertEquals("text/plain", response.get("responseContentType"));
	}

	@Test
	public void tokenReplacementDoesntHappenForThingsThatLooksLikeLocalizedIndexPages() throws Exception
	{
		dummyServlet.setResponseText("this token @A.TOKEN@ should not be processed");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/en_gb");
		verify(mockJndiContext, never()).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("this token @A.TOKEN@ should not be processed", response.get("responseText"));
	}
	
	@Test
    public void testFilterDoesNotChokeOnAStreamOnNonTextBits() throws Exception
    {
		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/br-logo.png");
		assertEquals("200", response.get("responseCode"));
    }
	
	@Test
	public void testFilterDoesNotBreakBinaryResponses() throws Exception
	{
		File logoFile = new File("src/test/resources/br-logo.png");
		tempFile = File.createTempFile(this.getClass().getSimpleName(), "temp");
		Map<String, String> response = makeBinaryRequest("http://localhost:"+serverPort+"/br-logo.png", new FileOutputStream(tempFile));
		assertEquals("200", response.get("responseCode"));
		assertTrue( "file contents wasnt equal", org.apache.commons.io.FileUtils.contentEquals(logoFile, tempFile) );
	}
	
	@Test
	public void tokenReplacementCantBePerformedForBrjsTokens() throws Exception
	{
		dummyServlet.setResponseText("@BRJS.TOKEN@");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/");
		assertEquals("500", response.get("responseCode"));
		assertTrue( response.get("responseText").contains(TokenReplacingReader.NO_BRJS_TOKEN_CONFIGURED_MESSAGE) );
	}

	@Test
	public void testFilteredExtentionsCanBeConfigured() throws Exception
	{
		appServer.stop();
		
		Map<String,String> filterInitParams = new HashMap<String,String>();
		filterInitParams.put("extensionRegex", "1234");
		
		appServer = createAndStartAppServer(dummyServlet, new TokenisingServletFilter(new JndiTokenFinder(mockJndiContext)), filterInitParams);
		
		dummyServlet.setResponseText("@A.TOKEN@");
		when(mockJndiContext.lookup("java:comp/env/A.TOKEN")).thenReturn("token replacement");

		Map<String, String> response = makeRequest("http://localhost:"+serverPort+"/file.1234");
		verify(mockJndiContext, times(1)).lookup("java:comp/env/A.TOKEN");
		assertEquals("200", response.get("responseCode"));
		assertEquals("token replacement", response.get("responseText"));
	}
	
}
