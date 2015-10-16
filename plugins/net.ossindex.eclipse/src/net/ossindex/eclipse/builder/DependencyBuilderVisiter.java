package net.ossindex.eclipse.builder;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.resource.VulnerabilityResource;
import net.ossindex.eclipse.Activator;
import net.ossindex.eclipse.builder.depends.IDependencyEvent;
import net.ossindex.eclipse.builder.depends.IDependencyListener;
import net.ossindex.eclipse.builder.depends.IDependencyPlugin;
import net.ossindex.eclipse.common.builder.CommonBuildVisitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.statushandlers.StatusManager;

public class DependencyBuilderVisiter extends CommonBuildVisitor implements IDependencyListener
{
	public static final String DEPENDENCY_NAME = "net.ossindex.eclipse.marker.name";
	public static final String DEPENDENCY_VERSION = "net.ossindex.eclipse.marker.version";
	public static final String DEPENDENCY_OPTIONAL = "net.ossindex.eclipse.marker.optional";
	public static final String DEPENDENCY_MARKER = "net.ossindex.eclipse.marker.DependencyMarker";
	public static final String ROOT_DEPENDENCY_MARKER = "net.ossindex.eclipse.marker.RootDependencyMarker";
	public static final String OPTIONAL_DEPENDENCY_MARKER = "net.ossindex.eclipse.marker.OptionalDependencyMarker";
	public static final String WARNING_DEPENDENCY_MARKER = "net.ossindex.eclipse.marker.WarningDependencyMarker";
	public static final String DEPENDENCY_URL = "net.ossindex.eclipse.marker.url";
	public static final String DEPENDENCY_ARTIFACT = "net.ossindex.eclipse.marker.artifactId";
	public static final String DEPENDENCY_SCM = "net.ossindex.eclipse.marker.scmId";
	
	public static final String VULNERABILITY_ID = "net.ossindex.eclipse.marker.id";
	public static final String VULNERABILITY_MARKER = "net.ossindex.eclipse.marker.VulnerabilityMarker";
	public static final String VULNERABILITY_SUMMARY = "net.ossindex.eclipse.marker.summary";

	private List<IDependencyPlugin> plugins;
	
	/**
	 * Files/Folders to ignore.
	 * 
	 * FIXME: We should query plugins for these.
	 */
	private static Set<String> ignore = new HashSet<String>();
	static
	{
		ignore.add("node_modules");
	}

	/**
	 * Progress monitor
	 */
	private SubMonitor progress;

	public DependencyBuilderVisiter(List<IDependencyPlugin> plugins, IProgressMonitor monitor)
	{
		super(DependencyBuilder.BUILDER_ID);
		progress = SubMonitor.convert(monitor);
		this.plugins = plugins;
		for (IDependencyPlugin plugin : plugins)
		{
			plugin.addListener(this);
		}
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
		// Handle cancellation
		if(progress.isCanceled()) return false;
		
		// FIXME: Perhaps make this optional
		if(ignore.contains(resource.getName())) return false;

		// Don't run if this file is not considered dirty at this time.
		if(isDirty((IFile)resource))
		{
			// Regardless of the amount of progress reported so far,
			// use 2% of the space remaining in the monitor to process the next node.
			progress.setWorkRemaining(10);
			// Clear dependency markers if we are going to build new ones.
			resource.deleteMarkers(DEPENDENCY_MARKER, true, IResource.DEPTH_ZERO);
			resource.deleteMarkers(VULNERABILITY_MARKER, true, IResource.DEPTH_ZERO);
			try
			{
				for (IDependencyPlugin plugin : plugins)
				{
					if(plugin.accepts(resource))
					{
						plugin.run(resource);
					}
				}
				markBuilt((IFile)resource);
			}
			catch(Exception e)
			{
				StatusManager.getManager().handle(new Status(Status.INFO, Activator.PLUGIN_ID, Status.OK, e.getMessage(), e), StatusManager.SHOW);
			}
			progress.worked(1);
		}
		//System.err.println("VISIT: " + resource);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuildVisitor#accepts(org.eclipse.core.resources.IFile)
	 */
	@Override
	protected boolean accepts(IFile resource)
	{
		for (IDependencyPlugin plugin : plugins)
		{
			if(plugin.accepts(resource))
			{
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuildVisitor#acceptsContainer(org.eclipse.core.resources.IContainer)
	 */
	@Override
	protected boolean acceptsContainer(IContainer resource)
	{
		// FIXME: Perhaps make this optional
		return !ignore.contains(resource.getName());
	}

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.common.builder.CommonBuildVisitor#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void setProgressMonitor(IProgressMonitor monitor)
	{
		progress = SubMonitor.convert(monitor);
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
		ScmResource scm = event.getScm();
		ArtifactResource artifact = event.getArtifact();
		
		if(event.isRoot()) System.err.println("ADD DEPENDENCY: [ROOT] " + source + ": " + line);
		else System.err.println("ADD DEPENDENCY: " + source + ": " + line);

		try
		{
			IMarker m = null;
			if(event.isRoot())
			{
				if(event.getOptional() || event.getArtifact() == null)
				{
					m = source.createMarker(OPTIONAL_DEPENDENCY_MARKER);
				}
				else
				{
					m = source.createMarker(ROOT_DEPENDENCY_MARKER);
				}
			}
			else
			{
				m = source.createMarker(DEPENDENCY_MARKER);
			}
			m.setAttribute(IMarker.LINE_NUMBER, line);
			m.setAttribute(IMarker.CHAR_START, charBegin);
			m.setAttribute(IMarker.CHAR_END, charEnd);
			m.setAttribute(IMarker.MESSAGE, description);
			m.setAttribute(DEPENDENCY_NAME, name);
			m.setAttribute(DEPENDENCY_VERSION, version);
			m.setAttribute(DEPENDENCY_OPTIONAL, event.getOptional());
			if(scm != null)
			{
				m.setAttribute(DEPENDENCY_SCM, Long.toString(scm.getId()));
			}
			if(artifact != null)
			{
				m.setAttribute(DEPENDENCY_ARTIFACT, Long.toString(artifact.getId()));
			}
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
					m.setAttribute(IMarker.MESSAGE, vulnerability.getTitle());
					m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);

					m.setAttribute(DEPENDENCY_URL, vulnerability.getUri().toString());
					long id = vulnerability.getId();
					m.setAttribute(VULNERABILITY_ID, Long.toString(id));
					m.setAttribute(VULNERABILITY_SUMMARY, vulnerability.getDescription());

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
	
	/** We may want to get new data from the server when possible, so we will
	 * want to talk to the server even when data has not changed.
	 * 
	 * @see net.ossindex.eclipse.common.builder.CommonBuildVisitor#useTimestamp()
	 */
	protected boolean useTimestamp()
	{
		return false;
	}
}
