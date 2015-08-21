/**
 *	Copyright (c) 2015 V�r Security Inc.
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
package net.ossindex.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Set;

import net.ossindex.common.resource.FileResource;
import net.ossindex.eclipse.decorators.OssIndexDecorator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/** A job that performs caching of OSS Index resources. This is used to avoid slowing
 * down the UI waiting for responses from the web.
 * 
 * @author Ken Duck
 *
 */
public class OssIndexCacheJob extends Job
{
	/**
	 * File buffer to process
	 */
	private Set<IFile> buffer;

	/**
	 * Set to true when the connection is bad, so we don't waste time trying
	 * to connect any further.
	 */
	private static Exception failedConnection = null;

	public OssIndexCacheJob(Set<IFile> buffer) throws OssIndexConnectionException
	{
		super("OSS Index");

		if(failedConnection != null)
		{
			throw new OssIndexConnectionException(failedConnection);
		}
		this.buffer = buffer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		if(buffer != null && !buffer.isEmpty())
		{
			for(IFile ifile: buffer)
			{
				File file = ifile.getLocation().toFile();
				try
				{
					FileResource fresource = FileResource.find(file);
					if(fresource != null)
					{
						ifile.setSessionProperty(OssIndexResourceManager.qname, fresource);
					}
					else
					{
						ifile.setSessionProperty(OssIndexResourceManager.qname, new FileResource(-1));
					}
				}
				catch (ConnectException e)
				{
					// Failed connection. Don't try any more.
					failedConnection = e;
				}
				catch (IOException e)
				{
					// Something went wrong.
					e.printStackTrace();
				}
				catch (CoreException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			
			// Decorate using current UI thread
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					IWorkbench workbench = PlatformUI.getWorkbench();
					IDecoratorManager manager = workbench.getDecoratorManager();
					manager.update(OssIndexDecorator.ID);
				}
			});
		}
		return Status.OK_STATUS;
	}
}