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
package net.ossindex.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;

import net.ossindex.common.resource.FileResource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/** Manage the creation of OSS Index resources for Eclipse IResources. Caches
 * OSS Index resources to reduce bandwidth consumption.
 * 
 * @author Ken Duck
 *
 */
public class OssIndexResourceManager extends JobChangeAdapter
{
	private static OssIndexResourceManager instance = null;

	/**
	 * Qualified name for saving properties to IFiles
	 */
	public static QualifiedName qname = new QualifiedName(Activator.PLUGIN_ID, "OssIndexResource");

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
			fresource = (FileResource) ifile.getSessionProperty(qname);

			if(fresource == null)
			{
				File file = ifile.getLocation().toFile();
				try
				{
					fresource = FileResource.find(file);
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
			FileResource fresource = (FileResource) ifile.getSessionProperty(qname);
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
	
	/** Called to update the buffer and possibly run the cache job on the buffer.
	 * 
	 * Synchronized to protect the buffer.
	 * 
	 * @param ifile File to add to the buffer, null if nothing to add.
	 * @throws OssIndexConnectionException
	 */
	private synchronized void updateBuffer(IFile ifile) throws OssIndexConnectionException
	{
		if(ifile != null) buffer.add(ifile);
		if(job == null || job.getState() == Job.NONE)
		{
			// (Re) Create the job
			job = new OssIndexCacheJob(buffer);
			// Detach the buffer
			buffer = new HashSet<IFile>();
			// Start the job
			job.addJobChangeListener(this);
			job.setPriority(Job.DECORATE);
			job.schedule();
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
