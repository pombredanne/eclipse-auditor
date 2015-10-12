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
package net.ossindex.eclipse.views;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.ossindex.eclipse.builder.DependencyBuilderVisiter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;


/** A view for exploring dependencies for a project
 * 
 * @author Ken Duck
 *
 */
public class DependencyView extends ViewPart implements CaretListener
{
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "net.ossindex.eclipse.views.DependencyView";

	private TableViewer viewer;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;

	private Set<IWorkbenchPart> activeEditors = new HashSet<IWorkbenchPart>();

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public DependencyView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new DependencyContentProvider());
		viewer.setLabelProvider(new DependencyLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		//		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(new ISelectionListener()
		//		{
		//
		//			/*
		//			 * (non-Javadoc)
		//			 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
		//			 */
		//			@Override
		//			public void selectionChanged(IWorkbenchPart part, ISelection selection)
		//			{
		//				System.err.println("WAT: " + selection.getClass().getCanonicalName());
		//
		//				if(selection instanceof ITextSelection)
		//				{
		//					ITextSelection textSel = (ITextSelection) selection;
		//					int begin = textSel.getStartLine() + 1; // +1 to match with markers
		//					int end = textSel.getEndLine() + 1; // +1 to match with markers
		//					IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		//
		//					IEditorInput input = null;
		//
		//					if(editorPart instanceof FormEditor)
		//					{
		//						FormEditor editor = (FormEditor)editorPart;
		//						input = editor.getEditorInput();
		//					}
		//					if(editorPart instanceof ITextEditor)
		//					{
		//						ITextEditor editor = (ITextEditor)part;
		//						input = editor.getEditorInput();
		//						//					    IDocumentProvider provider = editor.getDocumentProvider();
		//						//					    IDocument document = provider.getDocument(input);
		//					}
		//
		//					if(input != null)
		//					{
		//						if(input instanceof IFileEditorInput)
		//						{
		//							IFile file = ((IFileEditorInput)input).getFile();
		//							try
		//							{
		//								IMarker[] markers = file.findMarkers(DependencyBuilderVisiter.DEPENDENCY_MARKER, true, IResource.DEPTH_INFINITE);
		//								for (IMarker marker : markers)
		//								{
		//									Integer myLine = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
		//									if(myLine != null && (myLine >= begin && myLine <= end))
		//									{
		//										showDeps(marker);
		//									}
		//								}
		//							}
		//							catch (CoreException e)
		//							{
		//								// TODO Auto-generated catch block
		//								e.printStackTrace();
		//							}
		//						}
		//					}
		//				}
		//			}
		//
		//		});

		getSite().getWorkbenchWindow().getActivePage().addPartListener(new IPartListener() {
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
		//		makeActions();
		//		hookContextMenu();
		//		hookDoubleClickAction();
		//		contributeToActionBars();
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

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DependencyView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
	}

	private void makeActions() {
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
				viewer.getControl().getShell(),
				"Dependencies",
				message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/** Show the dependency information for the selected dependency
	 * 
	 * @param marker
	 */
	private Dependency getDep(IMarker marker)
	{
		try
		{
			if(DependencyBuilderVisiter.DEPENDENCY_MARKER.equals(marker.getType()))
			{
				return new Dependency(marker);
			}
		}
		catch (CoreException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.custom.CaretListener#caretMoved(org.eclipse.swt.custom.CaretEvent)
	 */
	@Override
	public void caretMoved(CaretEvent event)
	{
		System.err.println("CARET: " + event.getSource());

		StyledText stext = (StyledText)event.getSource();
		if(stext != null)
		{
			int offset = stext.getCaretOffset();
			System.err.println("  OFFSET: " + offset);
			StyledTextContent content = stext.getContent();

			// +1 because file lines start at 1
			int line = content.getLineAtOffset(offset) + 1;
			System.err.println("  LINE: " + line);



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
				System.err.println("WUT: " + editor.getActiveEditor().getClass().getSimpleName());
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


			// We have a file and a line number now. Find the dependencies...
			List<Dependency> deps = new LinkedList<Dependency>();
			if(file != null)
			{
				try
				{
					IMarker[] markers = file.findMarkers(DependencyBuilderVisiter.DEPENDENCY_MARKER, true, IResource.DEPTH_INFINITE);
					for (IMarker marker : markers)
					{
						Integer myLine = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
						if(myLine != null && (myLine == line))
						{
							Dependency dep = getDep(marker);
							if(dep != null)
							{
								deps.add(dep);
							}
						}
					}
				}
				catch (CoreException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(!deps.isEmpty()) viewer.setInput(deps);
		}
	}
}