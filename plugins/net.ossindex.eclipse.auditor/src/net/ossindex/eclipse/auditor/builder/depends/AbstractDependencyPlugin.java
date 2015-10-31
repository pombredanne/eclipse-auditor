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
package net.ossindex.eclipse.auditor.builder.depends;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResource;

/** Abstract class that provides common functionality for plugins that identify
 * dependencies. These could be plugins that scan package files, import/require
 * statements in source files, etc.
 * 
 * @author Ken Duck
 *
 */
public abstract class AbstractDependencyPlugin implements IDependencyPlugin
{

	/**
	 * Event listeners
	 */
	private List<IDependencyListener> listeners = new LinkedList<IDependencyListener>();

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyPlugin#accepts(org.eclipse.core.resources.IResource)
	 */
	@Override
	public abstract boolean accepts(IResource resource);

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyPlugin#run(org.eclipse.core.resources.IResource)
	 */
	@Override
	public abstract void run(IResource resource);

	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyPlugin#addListener(net.ossindex.eclipse.builder.depends.IDependencyListener)
	 */
	@Override
	public void addListener(IDependencyListener listener)
	{
		listeners.add(listener);
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.ossindex.eclipse.builder.depends.IDependencyPlugin#removeListener(net.ossindex.eclipse.builder.depends.IDependencyListener)
	 */
	@Override
	public void removeListener(IDependencyListener listener)
	{
		listeners.remove(listener);
	}

	/** Tell all listeners of a new dependency event
	 * 
	 * @param event
	 */
	public void fireDependencyEvent(IDependencyEvent event)
	{
		for (IDependencyListener listener : listeners)
		{
			listener.dependencyEvent(event);
		}
	}
}
