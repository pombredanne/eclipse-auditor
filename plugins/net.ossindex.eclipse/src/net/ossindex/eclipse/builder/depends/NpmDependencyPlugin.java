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
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ossindex.common.ResourceFactory;
import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.utils.FilePosition;
import net.ossindex.common.utils.LineIndexer;
import net.ossindex.common.utils.PackageDependency;
import net.ossindex.version.IVersion;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Identify and find dependencies in the NPM "package.json" file.
 * 
 * @author Ken Duck
 *
 */
public class NpmDependencyPlugin extends AbstractDependencyPlugin
{

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.AbstractDependencyPlugin#accepts(org.eclipse.core.resources.IResource)
	 */
	@Override
	public boolean accepts(IResource resource)
	{
		if(resource instanceof IFile)
		{
			if(resource.getName().equals("package.json")) return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.AbstractDependencyPlugin#run(org.eclipse.core.resources.IResource)
	 */
	@Override
	public void run(IResource resource)
	{
		IFile file = (IFile)resource;

		if("package.json".equals(file.getName()))
		{
			// Load the file contents intp a string
			InputStream is = null;
			try
			{
				is = file.getContents();
				StringWriter writer = new StringWriter();
				IOUtils.copy(is, writer, "UTF-8");
				String json = writer.toString();
				LineIndexer indexer = new LineIndexer(json);

				GsonBuilder builder = new GsonBuilder();
				Gson gson = builder.create();
				Reader reader = new StringReader(json);
				PackageJson pkg = gson.fromJson(reader, PackageJson.class);
				//List<Dependency> deps = processDependencies(file);

				Map<String, String> deps = new HashMap<String,String>();
				if(pkg != null)
				{
					Map<String, String> tmp = pkg.getDependencies();
					if(tmp != null) deps.putAll(tmp);
					tmp = pkg.getOptionalDependencies();
					if(tmp != null) deps.putAll(tmp);
					tmp = pkg.getDevDependencies();
					if(tmp != null) deps.putAll(tmp);
				}

				for(Map.Entry<String, String> entry: deps.entrySet())
				{
					getDependencies(file, indexer, entry.getKey(), entry.getValue());
				}

				reader.close();
			}
			catch(IOException | CoreException e)
			{
				System.err.println("Exception parsing " + file + ": " + e.getMessage());
				e.printStackTrace();
			}
		}

	}


	private void getDependencies(IFile file, LineIndexer indexer, String pkgName, String version) throws IOException
	{
		FilePosition position = indexer.getFirst("\"" + pkgName + "\"");

		// Local system dependencies should be converted to checksums
		if(version.startsWith("file:"))
		{
		}
		else if(version.indexOf("://") > 0)
		{
		}
		else if(version.indexOf('/') > 0)
		{
		}
		else
		{
			PackageDependency pkg = new PackageDependency(position, "npm", pkgName, version);
			pkg.setIsRoot(true);
			ArtifactResource[] artifactMatches = ResourceFactory.getResourceFactory().findArtifactResources(new PackageDependency[] {pkg});
			if(artifactMatches != null)
			{
				ArtifactResource match = null;
				for (ArtifactResource artifact : artifactMatches)
				{
					IVersion ver = artifact.getVersion();
					if(ver.satisfies(version))
					{
						if(match == null || artifact.compareTo(match) < 0)
						{
							match = artifact;
						}
					}
				}

				reportDependencyInformation(file, position, pkg, match);
			}
			//			packageDependency.add(new PackageDependency(position, "npm", pkgName, version));
			//				config.addDependency(file, "npm", pkgName, version, comment);
		}

	}

	/**
	 * 
	 * @param file
	 * @param position
	 * @param rootPkg 
	 * @param artifact
	 * @throws IOException 
	 */
	private void reportDependencyInformation(IFile file, FilePosition position, PackageDependency rootPkg, ArtifactResource artifact) throws IOException
	{
		List<PackageDependency> allPackages = new LinkedList<PackageDependency>();
		if(artifact != null)
		{
			ArtifactResource[] deps = artifact.getDependencyGraph();
			if(deps != null)
			{
				for (ArtifactResource dep : deps)
				{
					PackageDependency pkg = new PackageDependency(position, "npm", dep.getPackageName(), dep.getVersionString());
					pkg.setArtifact(dep);
					if(rootPkg.getName().equals(dep.getPackageName()))
					{
						pkg.setIsRoot(true);
					}
					allPackages.add(pkg);
				}
			}
		}
		else
		{
			allPackages.add(rootPkg);
		}

		List<Long> scmIds = new LinkedList<Long>();
		List<PackageDependency> packages = new LinkedList<PackageDependency>();
		for(PackageDependency pkg: allPackages)
		{
			ArtifactResource art = pkg.getArtifact();
			if(art != null)
			{
				long scmId = art.getScmId();
				// only continue with the artifact if it has a known SCM id.
				if(scmId > 0)
				{
					packages.add(pkg);
					scmIds.add(art.getScmId());
				}
			}
		}

		Long[] tmp = scmIds.toArray(new Long[scmIds.size()]);
		ScmResource[] scmResources = ResourceFactory.getResourceFactory().findScmResources(ArrayUtils.toPrimitive(tmp));

		// Add SCMs to the appropriate packages
		if(scmResources != null)
		{
			for(int i = 0; i < packages.size(); i++)
			{
				PackageDependency pkg = packages.get(i);
				pkg.setScm(scmResources[i]);
			}
		}


		// ALL packages are dependencies, not just the ones with SCMs
		for (PackageDependency pkg : allPackages)
		{
			fireDependencyEvent(new DependencyEvent(file, pkg));
		}

	}
}


/** Simple file for getting the contents of a package.xml file using Gson.
 * 
 * @author Ken Duck
 *
 */
class PackageJson
{
	private Map<String,String> dependencies;
	private Map<String,String> devDependencies;
	private Map<String,String> optionalDependencies;

	public Map<String,String> getDependencies()
	{
		return dependencies;
	}

	public Map<String,String> getOptionalDependencies()
	{
		return optionalDependencies;
	}

	public Map<String,String> getDevDependencies()
	{
		return devDependencies;
	}
}
