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
package net.ossindex.eclipse.auditor.builder.depends;

import java.io.IOException;

import net.ossindex.common.resource.ArtifactResource;
import net.ossindex.common.resource.ScmResource;
import net.ossindex.common.resource.VulnerabilityResource;

import org.eclipse.core.resources.IFile;

/** An event indicating something to do with dependencies
 * 
 * @author Ken Duck
 *
 */
public interface IDependencyEvent
{
	public enum DependencyEventType {
	    CLEAR,
	    ADD
	}

	/** Get the event type
	 * 
	 * @return
	 */
	DependencyEventType getType();

	/** Get the source of the dependency
	 * 
	 * @return
	 */
	IFile getSource();

	/** Get the line number the dependency appears on
	 * 
	 * @return
	 */
	int getSourceLine();
	int getOffset();
	int getLength();

	/** Get a string description of the dependency
	 * 
	 * @return
	 */
	String getDescription();

	String getName();

	String getVersion();
	
	/** Get the artifact
	 * 
	 * @return
	 */
	ArtifactResource getArtifact();
	
	/** Get the SCM
	 * 
	 * @return
	 */
	ScmResource getScm();

	/** Get related vulnerabilities
	 * 
	 * @return
	 * @throws IOException
	 */
	VulnerabilityResource[] getVulnerabilities() throws IOException;

	/** Is this an optional dependency?
	 * 
	 * @return
	 */
	boolean getOptional();

	/** Is this a root dependency (which has an annotation?)
	 * 
	 * @return
	 */
	boolean isRoot();
}
