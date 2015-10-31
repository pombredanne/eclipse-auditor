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
package net.ossindex.eclipse.auditor;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;

import net.ossindex.common.ResourceFactory;
import net.ossindex.common.resource.FileResource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.PlatformUI;

/** Manage the creation of OSS Index resources for Eclipse IResources. Caches
 * OSS Index resources to reduce bandwidth consumption.
 * 
 * The resource manager prevents hitting the API by caching information and
 * restricting accessing on restarts closer than 12 hours apart.
 * 
 * Future versions will reduce the query rate for refreshes, so that initial
 * access is fast but subsequent access is nicer.
 * 
 * @author Ken Duck
 *
 */
public class OssIndexResourceManager extends JobChangeAdapter
{
	private static OssIndexResourceManager instance = null;

	private static final long ONE_HOUR = 3600000;
	/**
	 * Request a refresh on new (un-cached) visits after 12 hours.
	 */
	private static final long UPDATE_REQUIRED = ONE_HOUR * 12;
	// Enable for debug purposes only. Its not nice to the server.
	// private static final long UPDATE_REQUIRED = 0;

	/**
	 * Qualified name for saving properties to IFiles
	 */
	public static QualifiedName RESOURCE_NAME = new QualifiedName(Activator.PLUGIN_ID, "OssIndexResource");
	public static QualifiedName ID_NAME = new QualifiedName(Activator.PLUGIN_ID, "OssIndexResource.Id");
	public static QualifiedName TIMESTAMP = new QualifiedName(Activator.PLUGIN_ID, "OssIndexResource.Timestamp");

	/**
	 * Buffer of files to cache.
	 * 
	 * WARNING: Only use in updateBuffer method.
	 */
	private Set<IFile> buffer = new HashSet<IFile>();

	/**
	 * Job for caching OssIndexResources for IFiles
	 */
	private OssIndexCacheJob job = null;

	private OssIndexResourceManager()
	{
	}

	/** Get the manager instance.
	 * 
	 * @return
	 */
	public static OssIndexResourceManager getInstance()
	{
		if(instance == null)
		{
			instance = new OssIndexResourceManager();
		}
		return instance;
	}

	/** Get the OSS Index File Resource associated with a particular file, if there
	 * is one.
	 * 
	 * Note that this blocks on web access.
	 * 
	 * @param ifile
	 * @return
	 * @throws OssIndexConnectionException 
	 */
	public FileResource getFileResource(IFile ifile) throws OssIndexConnectionException
	{
		FileResource fresource = null;
		try
		{
			fresource = getCachedResource(ifile);

			if(fresource == null)
			{
				File file = ifile.getLocation().toFile();
				try
				{
					fresource = ResourceFactory.getResourceFactory().findFileResource(file);
					return fresource;
				}
				catch (ConnectException e)
				{
					// Failed connection. Don't try any more.
					throw new OssIndexConnectionException(e);
				}
				catch (IOException e)
				{
					// Something went wrong.
					e.printStackTrace();
				}
			}
		}
		catch(CoreException e)
		{
			// Something went wrong.
			e.printStackTrace();
		}
		return fresource;
	}

	/** Get the file resource in a non-blocking way. If the resource has not
	 * been loaded yet then return null, but schedule the loading of the resource.
	 * 
	 * @param ifile
	 * @return
	 * @throws OssIndexConnectionException 
	 */
	public FileResource getNonBlockingFileResource(IFile ifile) throws OssIndexConnectionException
	{
		try
		{
			FileResource fresource = getCachedResource(ifile);
			if(fresource != null) return fresource;
		}
		catch(CoreException e)
		{
			// Something went wrong.
			e.printStackTrace();
		}

		updateBuffer(ifile);
		return null;
	}
	
	/** Get a resource from the session cache. Barring that use the persistent cache.
	 * This will allow for offline viewing.
	 * 
	 * @param ifile
	 * @return
	 * @throws CoreException
	 */
	public FileResource getCachedResource(IFile ifile) throws CoreException
	{
		FileResource resource = (FileResource) ifile.getSessionProperty(RESOURCE_NAME);
		
		if(resource == null)
		{
			String id = ifile.getPersistentProperty(ID_NAME);
			if(id != null)
			{
				try
				{
					long lid = Long.parseLong(id);
					resource = new FileResource(lid);
					
					// Check to see if an update is required. We do this to ensure
					// that repeated restarts do not hit the database too often.
					try
					{
						long now = System.currentTimeMillis();
						long timestamp = Long.parseLong(ifile.getPersistentProperty(TIMESTAMP));
						if(now - timestamp > UPDATE_REQUIRED)
						{
							updateBuffer(ifile);
						}
					}
					catch(NumberFormatException e)
					{
						updateBuffer(ifile);
					}
				}
				catch(OssIndexConnectionException e)
				{
					// Ignore for now. It will be detected elsewhere.
				}
				catch(NumberFormatException e) {}
			}
		}
		return resource;
	}

	
	/** Called to update the buffer and possibly run the cache job on the buffer.
	 * 
	 * Synchronized to protect the buffer.
	 * 
	 * @param ifile File to add to the buffer, null if nothing to add.
	 * @throws OssIndexConnectionException
	 */
	private synchronized void updateBuffer(IFile ifile) throws OssIndexConnectionException
	{
		if(ifile != null)
		{
			// Make sure this isn't already buffered and in progress
			if(job != null && !job.contains(ifile))
			{
				synchronized(buffer)
				{
					buffer.add(ifile);
				}
			}
		}
		if((buffer != null && !buffer.isEmpty()) && (job == null || job.getState() == Job.NONE))
		{
			// (Re) Create the job
			System.err.println("NEW JOB");
			job = new OssIndexCacheJob(buffer);
			// Start the job
			if(PlatformUI.isWorkbenchRunning())
			{
				job.addJobChangeListener(this);
				job.setPriority(Job.BUILD);
				job.schedule();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void done(IJobChangeEvent event)
	{
		// Give the queue a kick
		try
		{
			updateBuffer(null);
		}
		catch (OssIndexConnectionException e)
		{
			// Something went wrong
			e.printStackTrace();
		}
	}
}
