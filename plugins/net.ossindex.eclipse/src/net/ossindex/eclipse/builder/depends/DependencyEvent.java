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
package net.ossindex.eclipse.builder.depends;

import java.io.IOException;

import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.resource.VulnerabilityResource;
import net.ossindex.common.utils.PackageDependency;

import org.eclipse.core.resources.IFile;

/** Simple implementation of the dependency event interface.
 * 
 * @author Ken Duck
 *
 */
public class DependencyEvent implements IDependencyEvent
{
	private DependencyEventType type;
	
	/**
	 * Source side of the dependency
	 */
	private IFile source;
	
	/**
	 * If this is a package dependency, this contains the details.
	 */
	private PackageDependency pkg;
	
	/** Create a dependency of the specified type
	 * 
	 * @param file
	 * @param type
	 */
	public DependencyEvent(IFile file, DependencyEventType type)
	{
		this.source = file;
		this.type = type;
	}

	/**
	 * 
	 * @param file
	 * @param pkg
	 */
	public DependencyEvent(IFile file, PackageDependency pkg)
	{
		type = DependencyEventType.ADD;
		this.source = file;
		this.pkg = pkg;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getType()
	 */
	@Override
	public DependencyEventType getType()
	{
		return type;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getSource()
	 */
	@Override
	public IFile getSource()
	{
		return source;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getSourceLine()
	 */
	@Override
	public int getSourceLine()
	{
		return pkg.getLine();
	}
	
	public String getDependencyName()
	{
		return pkg.getName();
	}
	
	public String getDependencyVersion()
	{
		return pkg.getVersion();
	}
	
	/** Get a full descriptive string for the dependency
	 * 
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getDescription()
	 */
	@Override
	public String getDescription()
	{
		return pkg.getDescription();
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getCharBegin()
	 */
	@Override
	public int getOffset()
	{
		return pkg.getOffset();
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getCharEnd()
	 */
	@Override
	public int getLength()
	{
		return pkg.getLength();
	}

	@Override
	public String getName() {
		return pkg.getName();
	}

	@Override
	public String getVersion() {
		return pkg.getVersion();
	}
	
	@Override
	public VulnerabilityResource[] getVulnerabilities() throws IOException
	{
		return pkg.getVulnerabilities();
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getArtifact()
	 */
	@Override
	public ArtifactResource getArtifact()
	{
		if(pkg != null) return pkg.getArtifact();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getScm()
	 */
	@Override
	public ScmResource getScm()
	{
		if(pkg != null) return pkg.getScm();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#getOptional()
	 */
	@Override
	public boolean getOptional()
	{
		if(pkg != null) return pkg.getOptional();
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyEvent#isRoot()
	 */
	@Override
	public boolean isRoot()
	{
		if(pkg != null) return pkg.isRoot();
		return false;
	}
}
