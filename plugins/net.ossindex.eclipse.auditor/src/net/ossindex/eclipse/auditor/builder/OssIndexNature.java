package net.ossindex.eclipse.auditor.builder;

import java.util.LinkedList;
import java.util.List;

import net.ossindex.eclipse.common.Utils;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class OssIndexNature implements IProjectNature {

	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "net.ossindex.eclipse.auditor.OssIndexNature";

	private IProject project;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(DependencyBuilder.BUILDER_ID)) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(DependencyBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
//		IProjectDescription description = getProject().getDescription();
//		ICommand[] commands = description.getBuildSpec();
//		for (int i = 0; i < commands.length; ++i) {
//			if (commands[i].getBuilderName().equals(DependencyBuilder.BUILDER_ID)) {
//				ICommand[] newCommands = new ICommand[commands.length - 1];
//				System.arraycopy(commands, 0, newCommands, 0, i);
//				System.arraycopy(commands, i + 1, newCommands, i,
//						commands.length - i - 1);
//				description.setBuildSpec(newCommands);
//				project.setDescription(description, null);			
//				return;
//			}
//		}
		
		// Remove OSS Index builders
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		List<ICommand> newCommands = new LinkedList<ICommand>();
		for (int i = 0; i < commands.length; ++i)
		{
			String builderId = commands[i].getBuilderName();
			switch(builderId)
			{
			case DependencyBuilder.BUILDER_ID:
				break;
			default:
			    newCommands.add(commands[i]);
				break;
			}
		}
        description.setBuildSpec(newCommands.toArray(new ICommand[newCommands.size()]));
        project.setDescription(description, null);
		
		// Clear build information
		Utils.getCUtils().clean(new String[] {DependencyBuilder.BUILDER_ID});
		project.deleteMarkers(DependencyBuilderVisiter.DEPENDENCY_MARKER, true, IResource.DEPTH_INFINITE);
		project.deleteMarkers(DependencyBuilderVisiter.VULNERABILITY_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

}
