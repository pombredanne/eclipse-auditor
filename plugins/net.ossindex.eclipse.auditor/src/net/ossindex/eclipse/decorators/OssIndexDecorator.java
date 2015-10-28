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
package net.ossindex.eclipse.decorators;

import java.net.URL;

import net.ossindex.common.resource.FileResource;
import net.ossindex.eclipse.OssIndexConnectionException;
import net.ossindex.eclipse.OssIndexResourceManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

/**
 * An example showing how to control when an element is decorated. This example
 * decorates only elements that are instances of IResource and whose attribute
 * is 'Read-only'.
 * 
 * @see ILightweightLabelDecorator
 */
public class OssIndexDecorator implements ILightweightLabelDecorator
{
	public static final String ID = "net.ossindex.eclipse.decorators.OssIndexDecorator";
	/**
	 * String constants for the various icon placement options from the template
	 * wizard.
	 */
	public static final String TOP_RIGHT = "TOP_RIGHT";

	public static final String TOP_LEFT = "TOP_LEFT";

	public static final String BOTTOM_RIGHT = "BOTTOM_RIGHT";

	public static final String BOTTOM_LEFT = "BOTTOM_LEFT";

	public static final String UNDERLAY = "UNDERLAY";

	/** The integer value representing the placement options */
	private int quadrant;

	/** The icon image location in the project folder */
	private String iconPath = "icons/ossi-decorator.png"; //NON-NLS-1
	private String failIconPath = "icons/ossi-decorator-fail.png"; //NON-NLS-1

	/**
	 * The image description used in
	 * <code>addOverlay(ImageDescriptor, int)</code>
	 */
	private ImageDescriptor descriptor;

	/**
	 * Set to true when the connection is bad, so we don't waste time trying
	 * to connect any further.
	 */
	private boolean failedConnection = false;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration)
	{
		/**
		 * Checks that the element is an IResource with the 'Read-only' attribute
		 * and adds the decorator based on the specified image description and the
		 * integer representation of the placement option.
		 */
		IResource resource = (IResource) element;

		if(resource instanceof IFile)
		{
			IFile ifile = (IFile)resource;
			FileResource ossResource = null;
			if(!failedConnection)
			{
				try
				{
					ossResource = OssIndexResourceManager.getInstance().getNonBlockingFileResource(ifile);
				}
				catch(OssIndexConnectionException e)
				{
					failedConnection  = true;
				}
			}
			
			// On a failed connection, check the cache to see if we can work in offline
			// mode, otherwise indicate the connection problem.
			if(failedConnection)
			{
				try
				{
					ossResource = OssIndexResourceManager.getInstance().getCachedResource(ifile);
				}
				catch (CoreException coreE)
				{
					coreE.printStackTrace();
				}
				if(ossResource == null)
				{
					URL url = FileLocator.find(Platform.getBundle("net.ossindex.eclipse"), new Path(failIconPath), null); //NON-NLS-1
					descriptor = ImageDescriptor.createFromURL(url);			
					quadrant = IDecoration.TOP_RIGHT;
					decoration.addOverlay(descriptor,quadrant);
				}
			}
			
			// Set the decorator for the file
			if(ossResource != null && ossResource.exists())
			{
				URL url = FileLocator.find(Platform.getBundle("net.ossindex.eclipse"), new Path(iconPath), null); //NON-NLS-1
				descriptor = ImageDescriptor.createFromURL(url);			
				quadrant = IDecoration.TOP_RIGHT;
				decoration.addOverlay(descriptor,quadrant);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}
}