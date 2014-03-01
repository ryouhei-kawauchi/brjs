package org.bladerunnerjs.appserver;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bladerunnerjs.model.App;
import org.bladerunnerjs.model.BRJS;
import org.bladerunnerjs.model.BladerunnerUri;
import org.bladerunnerjs.model.exception.ConfigException;
import org.bladerunnerjs.model.exception.request.ContentProcessingException;
import org.bladerunnerjs.model.exception.request.MalformedRequestException;
import org.bladerunnerjs.model.exception.request.ResourceNotFoundException;


public class BRJSServlet extends HttpServlet
{
	private static final long serialVersionUID = 1964608537461568895L;
	
	private App app;
	private ServletContext servletContext;
	private BRJS brjs;
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		
		servletContext = config.getServletContext();
		ServletModelAccessor.initializeModel(servletContext);
		
		try {
			brjs = ServletModelAccessor.aquireModel();
			app = brjs.locateAncestorNodeOfClass(new File(servletContext.getRealPath(".")), App.class);
		}
		finally {
			ServletModelAccessor.releaseModel();
		}
	}
	
	@Override
	public void destroy()
	{
		ServletModelAccessor.destroy();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try {
			BRJS brjs = ServletModelAccessor.aquireModel();
			String contentType = servletContext.getMimeType(request.getRequestURI());
			
			if((contentType != null) && contentType.startsWith("text/") && !contentType.contains("charset")) {
				contentType += ";charset=" + brjs.bladerunnerConf().getBrowserCharacterEncoding();
			}
			
			response.setContentType(contentType);
			BladerunnerUri bladerunnerUri = new BladerunnerUri(brjs, servletContext, request);
			app.getBundlableNode(bladerunnerUri).handleLogicalRequest(bladerunnerUri.logicalPath, response.getOutputStream());
		} catch (MalformedRequestException | ResourceNotFoundException | ContentProcessingException | ConfigException e) {
			throw new ServletException(e);
		}
		finally {
			ServletModelAccessor.releaseModel();
		}
	}
}
