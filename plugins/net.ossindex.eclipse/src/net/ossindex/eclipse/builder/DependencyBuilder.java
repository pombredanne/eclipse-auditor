/**
 *	Copyright (c) 2015 Vör Security Inc.
 *	All rights reserved.
 *	
 *	Redistribution and use in source and binary forms, with or without
 *	modification, are permitted provided that the following conditions are met:
 *	    * Redistributions of source code must retain the above copyright
 *	      notice, this list of conditions and the following disclaimer.
 *	    * Redistributions in binary form must reproduce the above copyright
 *	      notice, this list of conditions and the following disclaimer in the
 *	      documentation and/or other materials provided with the distribution.
 *	    * Neither the name of the <organization> nor the
 *	      names of its contributors may be used to endorse or promote products
 *	      derived from this software without specific prior written permission.
 *	
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *	ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *	WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *	DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 *	DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *	(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *	ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *	(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ossindex.eclipse.builder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.ossindex.common.resource.VulnerabilityResource;
import net.ossindex.eclipse.builder.depends.IDependencyEvent;
import net.ossindex.eclipse.builder.depends.IDependencyListener;
import net.ossindex.eclipse.builder.depends.IDependencyPlugin;
import net.ossindex.eclipse.builder.depends.NpmDependencyPlugin;
import net.ossindex.eclipse.common.builder.CommonBuilder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/** Identify files with known dependencies. Using this information attempt to find
 * vulnerability information.
 * 
 * @author Ken Duck
 *
 */
public class DependencyBuilder extends CommonBuilder implements IResourceVisitor, IResourceDeltaVisitor, IDependencyListener
{
	public static final String BUILDER_ID = "net.ossindex.eclipse.DependencyBuilder";
	private static final String MARKER_TYPE = "net.ossindex.eclipse.xmlProblem";
	private static final String DEPENDENCY_MARKER = "net.ossindex.eclipse.marker.DependencyMarker";
	private static final String DEPENDENCY_NAME = "net.ossindex.eclipse.marker.name";
	private static final String DEPENDENCY_VERSION = "net.ossindex.eclipse.marker.version";
	private static final String VULNERABILITY_MARKER = "net.ossindex.eclipse.marker.VulnerabilityMarker";
	private List<IDependencyPlugin> plugins = new LinkedList<IDependencyPlugin>();

	/**
	 * Initialize the builder plugins
	 */
	public DependencyBuilder()
	{
		// FIXME: Eventually this should likely be configurable
		NpmDependencyPlugin plugin = new NpmDependencyPlugin();
		plugin.addListener(this);
		plugins.add(plugin);
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuilder#getBuildVisitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IResourceVisitor getBuildVisitor(IProgressMonitor monitor)
	{
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuilder#getDeltaVisitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IResourceDeltaVisitor getDeltaVisitor(IProgressMonitor monitor)
	{
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException
	{
		IResource resource = delta.getResource();
		return visit(resource);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
	 */
	@Override
	public boolean visit(IResource resource) throws CoreException
	{
		// Clear dependency markers if we are going to build new ones.
		resource.deleteMarkers(DEPENDENCY_MARKER, true, IResource.DEPTH_ZERO);
		resource.deleteMarkers(VULNERABILITY_MARKER, true, IResource.DEPTH_ZERO);
		for (IDependencyPlugin plugin : plugins)
		{
			if(plugin.accepts(resource))
			{
				plugin.run(resource);
			}
		}
		//System.err.println("VISIT: " + resource);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyListener#dependencyEvent(net.ossindex.eclipse.builder.depends.IDependencyEvent)
	 */
	@Override
	public void dependencyEvent(IDependencyEvent event)
	{
		switch(event.getType())
		{
		case ADD:
			handleAddDependency(event);
			break;
		case CLEAR:
			break;
		default:
			break;
		}
	}

	/** Adding a new dependency
	 * 
	 * @param event
	 */
	private void handleAddDependency(IDependencyEvent event)
	{
		IFile source = event.getSource();
		int line = event.getSourceLine();
		int charBegin = event.getOffset();
		int charEnd = event.getLength();
		String name = event.getName();
		String version = event.getVersion();
		String description = event.getDescription();

		try
		{
			IMarker m = source.createMarker(DEPENDENCY_MARKER);
			m.setAttribute(IMarker.LINE_NUMBER, line);
			m.setAttribute(IMarker.CHAR_START, charBegin);
			m.setAttribute(IMarker.CHAR_END, charEnd);
			m.setAttribute(IMarker.MESSAGE, description);
			m.setAttribute(DEPENDENCY_NAME, name);
			m.setAttribute(DEPENDENCY_VERSION, version);
		}
		catch(CoreException e)
		{
			e.printStackTrace();
		}

		try
		{
			VulnerabilityResource[] vulnerabilities = event.getVulnerabilities();
			if(vulnerabilities != null)
			{
				for (VulnerabilityResource vulnerability : vulnerabilities)
				{
					IMarker m = source.createMarker(VULNERABILITY_MARKER);
					m.setAttribute(IMarker.LINE_NUMBER, line);
					m.setAttribute(IMarker.MESSAGE, vulnerability.getDescription());
					m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
					
					// If it does not affect the installed version it should be stored as info only
					if(vulnerability.affects(name, version))
					{
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					}
					else
					{
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
					}
				}
			}
		}
		catch (IOException | CoreException e)
		{
			e.printStackTrace();
		}

	}
}
