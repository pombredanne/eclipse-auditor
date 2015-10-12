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

import java.util.LinkedList;
import java.util.List;

import net.ossindex.eclipse.builder.depends.IDependencyPlugin;
import net.ossindex.eclipse.builder.depends.MavenDependencyPlugin;
import net.ossindex.eclipse.builder.depends.NpmDependencyPlugin;
import net.ossindex.eclipse.common.builder.CommonBuilder;

import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.IProgressMonitor;

/** Identify files with known dependencies. Using this information attempt to find
 * vulnerability information.
 * 
 * @author Ken Duck
 *
 */
public class DependencyBuilder extends CommonBuilder
{
	public static final String BUILDER_ID = "net.ossindex.eclipse.DependencyBuilder";
	private List<IDependencyPlugin> plugins = new LinkedList<IDependencyPlugin>();
	
	private DependencyBuilderVisiter visitor;
	
	/**
	 * Initialize the builder plugins
	 */
	public DependencyBuilder()
	{
		// FIXME: Eventually this should likely be configurable
		plugins.add(new NpmDependencyPlugin());
		plugins.add(new MavenDependencyPlugin());
		
		visitor = new DependencyBuilderVisiter(plugins, null);
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuilder#getBuildVisitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IResourceVisitor getBuildVisitor(IProgressMonitor monitor)
	{
		return visitor;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuilder#getDeltaVisitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IResourceDeltaVisitor getDeltaVisitor(IProgressMonitor monitor)
	{
		return visitor;
	}


	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuilder#getName()
	 */
	@Override
	protected String getName()
	{
		return "Dependency builder";
	}
}
