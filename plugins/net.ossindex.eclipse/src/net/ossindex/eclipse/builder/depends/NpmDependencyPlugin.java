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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.utils.FilePosition;
import net.ossindex.common.utils.LineIndexer;
import net.ossindex.common.utils.PackageDependency;

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

				if(pkg != null)
				{
					Map<String, String> deps = pkg.getDependencies();
					processDependencies(file, deps, indexer);

					deps = pkg.getOptionalDependencies();
					processDependencies(file, deps, indexer);

					deps = pkg.getDevDependencies();
					processDependencies(file, deps, indexer);
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


	/**
	 * 
	 * @param file
	 * @param deps
	 * @param indexer 
	 * @comment A comment to add to each of these dependencies
	 */
	private void processDependencies(IFile file, Map<String, String> deps, LineIndexer indexer) throws IOException
	{
		if(deps == null) return;

		List<PackageDependency> packageDependency = new LinkedList<PackageDependency>();

		for(Map.Entry<String,String> entry: deps.entrySet())
		{
			String pkgName = entry.getKey();
			String version = entry.getValue();

			// Local system dependencies should be converted to checksums
			if(version.startsWith("file:"))
			{
				//				try
				//				{
				//					version = getChecksum(file, version.substring(5));
				//					if(version != null)
				//					{
				//						config.addDependency(file, "npm", pkgName, version, comment);
				//					}
				//				}
				//				catch(IOException e)
				//				{
				//					System.err.println("Exception handling " + version + ": " + e.getMessage());
				//				}
			}
			else if(version.indexOf("://") > 0)
			{
				//				try
				//				{
				//					config.addDependency(file, "npm", new URI(version), comment);
				//				}
				//				catch (URISyntaxException e)
				//				{
				//					System.err.println("Exception parsing uri " + version + ": " + e.getMessage());
				//				}
			}
			else if(version.indexOf('/') > 0)
			{
				//				try
				//				{
				//					config.addDependency(file, "npm", new URI("git://" + version), comment);
				//				}
				//				catch (URISyntaxException e)
				//				{
				//					System.err.println("Exception parsing uri " + version + ": " + e.getMessage());
				//				}
			}
			else
			{
				FilePosition position = indexer.getFirst("\"" + pkgName + "\"");
				packageDependency.add(new PackageDependency(position, "npm", pkgName, version));
				//				config.addDependency(file, "npm", pkgName, version, comment);
			}
		}

		processPackageDependency(file, packageDependency);
	}

	/** Get the SHA checksum for a file found relative to the specified location
	 * 
	 * @param file File which the path may be relative to
	 * @param path
	 * @return
	 * @throws IOException 
	 * @throws CoreException 
	 */
	private String getChecksum(IFile file, String path) throws IOException, CoreException
	{
		InputStream is = null;
		try
		{
			is = file.getContents();
			return DigestUtils.shaHex(is);
		}
		finally
		{
			if(is != null)
			{
				is.close();
			}
		}
	}

	/**
	 * 
	 * @param packageDependency
	 * @throws IOException 
	 */
	private void processPackageDependency(IFile file, List<PackageDependency> packageDependencies) throws IOException
	{
		// Find matching artifacts for the packages
		PackageDependency[] pkgs = packageDependencies.toArray(new PackageDependency[packageDependencies.size()]);
		ArtifactResource[] artifactMatches = ArtifactResource.find(pkgs);

		Map<String,ArtifactResource> matches = new HashMap<String,ArtifactResource>();
		for (ArtifactResource artifact : artifactMatches)
		{
			if(artifact != null)
			{
				String name = artifact.getPackageName();
				if(!matches.containsKey(name))
				{
					matches.put(name, artifact);
				}
				else
				{
					ArtifactResource ar = matches.get(name);
					if(artifact.compareTo(ar) > 0) matches.put(name, artifact);
				}
			}
		}

		List<PackageDependency> packages = new LinkedList<PackageDependency>();
		List<Long> scmIds = new LinkedList<Long>();
		for(PackageDependency pkg: pkgs)
		{
			if(matches.containsKey(pkg.getName()))
			{
				ArtifactResource artifact = matches.get(pkg.getName());
				long scmId = artifact.getScmId();
				// only continue with the artifact if it has a known SCM id.
				if(scmId > 0)
				{
					pkg.setArtifact(artifact);
					packages.add(pkg);
					scmIds.add(artifact.getScmId());
				}
			}
		}

		Long[] tmp = scmIds.toArray(new Long[scmIds.size()]);
		ScmResource[] scmResources = ScmResource.find(ArrayUtils.toPrimitive(tmp));
		// This should never happen
		if(scmResources == null) return;

		for(int i = 0; i < packages.size(); i++)
		{
			PackageDependency pkg = packages.get(i);
			pkg.setScm(scmResources[i]);

			fireDependencyEvent(new DependencyEvent(file, pkg));
		}
		//		System.err.println("PKGs");
		//		for(int i = 0; i < packages.size(); i++)
		//		{
		//			ScmResource scm = scmResources[i];
		//			System.err.println("  * " + packages.get(i) + " " + scm);
		//			
		//			VulnerabilityResource[] vulnerabilities = scm.getVulnerabilities();
		//			if(vulnerabilities != null)
		//			{
		//				for (VulnerabilityResource vulnerability : vulnerabilities)
		//				{
		//					System.err.println("    + " + vulnerability.getUri());
		//				}
		//			}
		//			
		//		}

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
