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
import java.io.InputStream;
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
import net.ossindex.eclipse.builder.depends.AbstractDependencyPlugin;
import net.ossindex.eclipse.builder.depends.DependencyEvent;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/** Identify dependencies in a pom.xml file and find any related
 * vulnerabilities.
 * 
 * @author Ken Duck
 *
 */
public class MavenDependencyPlugin extends AbstractDependencyPlugin
{
	private RepositorySystem repoSystem;
	private RepositorySystemSession session;
	private RemoteRepository central;
	
	public MavenDependencyPlugin()
	{
		repoSystem = newRepositorySystem();
		session = newSession( repoSystem );
		central = new RemoteRepository.Builder( "central", "default", "http://repo1.maven.org/maven2/" ).build();

	}

	@Override
	public boolean accepts(IResource resource)
	{
		if(resource instanceof IFile)
		{
			if(resource.getName().equals("pom.xml")) return true;
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
		System.err.println("WAT: " + resource);
		
		
		InputStream is = null;
		try
		{
			is = ((IFile)resource).getContents();
			StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer, "UTF-8");
			String pom = writer.toString();
			LineIndexer indexer = new LineIndexer(pom);
			
			Reader reader = new StringReader(pom);
			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			Model model = xpp3Reader.read(reader);
			reader.close();
			List<org.apache.maven.model.Dependency> deps = model.getDependencies();
			if(deps != null)
			{
				for (org.apache.maven.model.Dependency dep : deps)
				{
					getDependencies((IFile)resource, indexer, dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
				}
			}
		}
		catch (CoreException | IOException | XmlPullParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if(is != null)
			{
				try
				{
					is.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 
	 * @param resource 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	private void getDependencies(IFile resource, LineIndexer indexer, String groupId, String artifactId, String version)
	{
		FilePosition position = indexer.getFirst(artifactId);
		String aid = groupId + ":" + artifactId + ":";
		if(version != null) aid += version;
		Dependency dependency = new Dependency( new DefaultArtifact( aid ), "compile" );

		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot( dependency );
		collectRequest.addRepository( central );
		try
		{
			DependencyNode node = repoSystem.collectDependencies( session, collectRequest ).getRoot();

			DependencyRequest dependencyRequest = new DependencyRequest();
			dependencyRequest.setRoot( node );

			repoSystem.resolveDependencies( session, dependencyRequest  );

			PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
			node.accept( nlg );
			
			List<Artifact> artifacts = nlg.getArtifacts(false);
			List<PackageDependency> packageDependency = new LinkedList<PackageDependency>();
			for (Artifact artifact : artifacts)
			{
				PackageDependency pkgDep = new PackageDependency(position, "maven", artifact.getArtifactId(), artifact.getVersion());
				packageDependency.add(pkgDep);
			}
			reportDependencyInformation(resource, position, packageDependency.toArray(new PackageDependency[packageDependency.size()]));
		}
		catch(DependencyCollectionException | DependencyResolutionException | IOException e)
		{
			e.printStackTrace();
		}
	}

	/** 
	 * 
	 * @param file 
	 * @param position
	 * @param packageDependencies
	 * @throws IOException 
	 */
	private void reportDependencyInformation(IFile file, FilePosition position, PackageDependency[] pkgs) throws IOException
	{
//		AbstractRemoteResource.setDebug(true);
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
	}

	/** http://wiki.eclipse.org/Aether/Setting_Aether_Up
	 * 
	 * @return
	 */
	private static RepositorySystem newRepositorySystem()
	{
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
		locator.addService( TransporterFactory.class, FileTransporterFactory.class );
		locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

		return locator.getService( RepositorySystem.class );
	}

	/** http://wiki.eclipse.org/Aether/Creating_a_Repository_System_Session
	 * 
	 * @param system
	 * @return
	 */
	private static RepositorySystemSession newSession( RepositorySystem system )
	{
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

		String home = System.getProperty("user.home");
		LocalRepository localRepo = new LocalRepository( home + "/.m2/repository" );
		session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

		return session;
	}

}
