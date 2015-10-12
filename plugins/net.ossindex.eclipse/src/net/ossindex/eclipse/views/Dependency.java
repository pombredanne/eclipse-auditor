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
package net.ossindex.eclipse.views;

import java.io.IOException;

import net.ossindex.common.ResourceFactory;
import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.PackageResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.eclipse.builder.DependencyBuilderVisiter;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

/** Simple class representing a dependency.
 * 
 * @author Ken Duck
 *
 */
public class Dependency
{

	private String name;
	private String version;
	private ArtifactResource artifact;
	private ScmResource scm;

	public Dependency(IMarker marker) throws CoreException
	{
		name = (String)marker.getAttribute(DependencyBuilderVisiter.DEPENDENCY_NAME);
		version = (String)marker.getAttribute(DependencyBuilderVisiter.DEPENDENCY_VERSION);
		String ids = marker.getAttribute(DependencyBuilderVisiter.DEPENDENCY_ARTIFACT, null);
		if(ids != null)
		{
			try
			{
				long id = Long.parseLong(ids);
				artifact = ResourceFactory.getResourceFactory().createResource(ArtifactResource.class, id);
			}
			catch(NumberFormatException e)
			{
				// Ignore
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		ids = marker.getAttribute(DependencyBuilderVisiter.DEPENDENCY_SCM, null);
		if(ids != null)
		{
			try
			{
				long id = Long.parseLong(ids);
				scm = ResourceFactory.getResourceFactory().createResource(ScmResource.class, id);
			}
			catch(NumberFormatException e)
			{
				// Ignore
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return name + " " + version;
	}

	public String getName()
	{
		return name;
	}
	
	public String getVersion()
	{
		return version;
	}

	/** Get a description for the dependency if possible
	 * 
	 * @return
	 */
	public String getDescription()
	{
		if(artifact != null)
		{
			String desc = artifact.getDescription();
			if(desc != null && !desc.trim().isEmpty())
			{
				return desc;
			}
		}
		if(scm != null)
		{
			String desc = scm.getDescription();
			if(desc != null && !desc.trim().isEmpty())
			{
				return desc;
			}
		}

		return null;
	}

	/** Get the package for the dependency, if it exists.
	 * 
	 * @return
	 */
	public PackageResource getPackage()
	{
		if(artifact != null)
		{
			try
			{
				return artifact.getPackage();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}
}
