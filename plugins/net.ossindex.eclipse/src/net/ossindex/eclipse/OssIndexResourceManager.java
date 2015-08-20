/**
 *	Copyright (c) 2014-2015 Vör Security Inc.
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
import java.util.HashMap;
import java.util.Map;

import net.ossindex.common.resource.FileResource;

import org.eclipse.core.resources.IFile;

/** Manage the creation of OSS Index resources for Eclipse IResources. Caches
 * OSS Index resources to reduce bandwidth consumption.
 * 
 * @author Ken Duck
 *
 */
public class OssIndexResourceManager
{
	private static OssIndexResourceManager instance = null;
	
	/**
	 * Map of IFile to FileResource
	 */
	private Map<IFile,FileResource> map = new HashMap<IFile,FileResource>();
	
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
	 * @param ifile
	 * @return
	 */
	public FileResource getFileResource(IFile ifile)
	{
		if(!map.containsKey(ifile))
		{
			File file = ifile.getLocation().toFile();
			try
			{
				FileResource fresource = FileResource.find(file);
				map.put(ifile, fresource);
				return fresource;
			}
			catch (IOException e)
			{
				// Something went wrong.
				map.put(ifile, null);
			}
		}
		
		return map.get(ifile);
	}
}
