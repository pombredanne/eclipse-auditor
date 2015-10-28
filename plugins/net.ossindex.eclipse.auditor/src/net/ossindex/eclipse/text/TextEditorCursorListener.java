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
package net.ossindex.eclipse.text;

import java.util.HashSet;
import java.util.Set;

import net.ossindex.eclipse.views.DependencyView;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.ITextEditor;

/** Listen for cursor events in text editors.
 * 
 * @author Ken Duck
 *
 */
public class TextEditorCursorListener implements CaretListener
{

	private Set<IWorkbenchPart> activeEditors = new HashSet<IWorkbenchPart>();

	private ICursorListener listener;

	public TextEditorCursorListener(IWorkbenchPage page, ICursorListener listener)
	{
		this.listener = listener;
		
		page.addPartListener(new IPartListener() {
			@Override
			public void partActivated(IWorkbenchPart part) {
				addCaretListener(part);
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				addCaretListener(part);
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				activeEditors.remove(part);
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
			}

			@Override
			public void partOpened(IWorkbenchPart part) {
				addCaretListener(part);
			}
		});

	}
	
	/**
	 * 
	 * @param site
	 * @param listener
	 */
	public TextEditorCursorListener(IWorkbenchPartSite site, ICursorListener listener)
	{
		this(site.getWorkbenchWindow().getActivePage(), listener);
	}

	/**
	 * 
	 * @param part
	 */
	protected void addCaretListener(IWorkbenchPart part)
	{
		if(!activeEditors.contains(part))
		{
			ITextEditor text = null;

			if(part instanceof FormEditor) {
				//	        //Check if this is an editor and its input is what I need
				//	        AbstractTextEditor e =
				//	            (AbstractTextEditor)((IEditorReference) partRef).getEditor(false);
				//	        //((StyledText)e.getAdapter(Control.class)).addCaretListener(l);
				IEditorPart apart = ((FormEditor)part).getActiveEditor();
				if(apart instanceof ITextEditor)
				{
					text = (ITextEditor)apart;
				}
			}

			else if(part instanceof ITextEditor)
			{
				text = (ITextEditor)part;
			}

			if(text != null)
			{
				System.err.println("WHOO");
				Control control = (Control) (text.getAdapter(Control.class));
				if(control instanceof StyledText)
				{
					StyledText styledText = (StyledText)control;
					styledText.addCaretListener(this);
					activeEditors.add(part);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.custom.CaretListener#caretMoved(org.eclipse.swt.custom.CaretEvent)
	 */
	@Override
	public void caretMoved(CaretEvent event)
	{
		StyledText stext = (StyledText)event.getSource();
		if(stext != null)
		{
			int offset = stext.getCaretOffset();
			StyledTextContent content = stext.getContent();

			// +1 because file lines start at 1
			int line = content.getLineAtOffset(offset) + 1;

			// Now find the file
			IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			ITextEditor textEditor = null;

			if(editorPart instanceof FormEditor)
			{
				FormEditor editor = (FormEditor)editorPart;
				if(editor.getActiveEditor() instanceof ITextEditor)
				{
					textEditor = (ITextEditor) editor.getActiveEditor();
				}
			}
			if(editorPart instanceof ITextEditor)
			{
				ITextEditor editor = (ITextEditor)editorPart;
				textEditor = (ITextEditor) editor;
				//					    IDocumentProvider provider = editor.getDocumentProvider();
				//					    IDocument document = provider.getDocument(input);
			}

			IFile file = null;
			if(textEditor != null)
			{
				IEditorInput input = textEditor.getEditorInput();
				file = ((IFileEditorInput)input).getFile();
			}

			if(file != null)
			{
				listener.cursorEvent(new CursorEvent(file, line));
			}
		}
	}
}
