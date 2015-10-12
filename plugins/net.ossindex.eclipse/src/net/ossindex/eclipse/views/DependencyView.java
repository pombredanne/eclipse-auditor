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


import java.util.LinkedList;
import java.util.List;

import net.ossindex.common.resource.PackageResource;
import net.ossindex.eclipse.builder.DependencyBuilderVisiter;
import net.ossindex.eclipse.text.CursorEvent;
import net.ossindex.eclipse.text.ICursorListener;
import net.ossindex.eclipse.text.TextEditorCursorListener;

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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;


/** A view for exploring dependencies for a project
 * 
 * @author Ken Duck
 *
 */
public class DependencyView extends ViewPart implements ICursorListener
{
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "net.ossindex.eclipse.views.DependencyView";

	private TableViewer dependencyViewer;
	private TableViewer versionViewer;

	private Action action1;
	private Action action2;
	private Action doubleClickAction;

	/**
	 * Indicate the name of the selected package
	 */
	private Label selectedPackageLabel;

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
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		composite.setLayout(layout);

		selectedPackageLabel = new Label(composite, SWT.NONE);
		selectedPackageLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		selectedPackageLabel.setText("<Select package...>");
		selectedPackageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));

		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		SashForm form = new SashForm(composite, SWT.HORIZONTAL);
		form.setLayout(new FillLayout());
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		form.setBackground(form.getDisplay().getSystemColor( SWT.COLOR_GRAY));

		buildDependencyPanel(form);
		buildVersionPanel(form);

		form.setWeights(new int[] {75, 25});



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

		// Listen for cursor events
		new TextEditorCursorListener(getSite(), this);

		//		makeActions();
		//		hookContextMenu();
		//		hookDoubleClickAction();
		//		contributeToActionBars();
	}

	private void buildVersionPanel(SashForm form)
	{
		Composite composite = new Composite(form, SWT.NONE);
		composite.setBackground(form.getDisplay().getSystemColor( SWT.COLOR_WHITE));
		GridLayout layout = new GridLayout(1, true);
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		label.setText("Available versions");
		label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));

		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		versionViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		versionViewer.setContentProvider(new PackageContentProvider());
		versionViewer.setLabelProvider(new PackageLabelProvider());
		versionViewer.setComparator(new ArtifactComparator());
		versionViewer.setInput(getViewSite());

		Table table = versionViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
	}

	private void buildDependencyPanel(SashForm form)
	{
		//		Composite composite = new Composite(form, SWT.NONE);
		//		composite.setBackground(form.getDisplay().getSystemColor( SWT.COLOR_WHITE));
		//		GridLayout layout = new GridLayout(1, true);
		//		composite.setLayout(layout);
		//		
		//		Label label = new Label(composite, SWT.NONE);
		//		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		//		label.setText("Package dependencies");
		//		label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
		//		
		//		Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		//	    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		dependencyViewer = new TableViewer(form, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		dependencyViewer.setContentProvider(new ArtifactContentProvider());
		//		dependencyViewer.setLabelProvider(new ArtifactLabelProvider());
		//		dependencyViewer.setSorter(new NameSorter());
		dependencyViewer.setInput(getViewSite());

		//		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		Table table = dependencyViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		//		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		createTableViewerColumn(dependencyViewer, "Package dependencies", 200, 0, true);
		createTableViewerColumn(dependencyViewer, "Version", null, 1, true);
		createTableViewerColumn(dependencyViewer, "Description", 400, 1, true);
	}

	private TableViewerColumn createTableViewerColumn(TableViewer viewer, final String title, final Integer bound, final int colNumber, boolean enableSorting)
	{
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.LEFT);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);
		if (enableSorting == true)
		{
			//column.addSelectionListener(getSelectionAdapter(viewer, column, colNumber));
		}
		if(bound != null) column.setWidth(bound);
		else column.pack();
		viewerColumn.setLabelProvider(new ArtifactCellLabelProvider());
		return viewerColumn;
	}


	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DependencyView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(dependencyViewer.getControl());
		dependencyViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, dependencyViewer);
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
				ISelection selection = dependencyViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
	}

	private void hookDoubleClickAction() {
		dependencyViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
				dependencyViewer.getControl().getShell(),
				"Dependencies",
				message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		dependencyViewer.getControl().setFocus();
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
	 * @see net.ossindex.eclipse.views.ICursorListener#cursorEvent(net.ossindex.eclipse.views.CursorEvent)
	 */
	@Override
	public void cursorEvent(CursorEvent event)
	{
		IFile file = event.getFile();
		int line = event.getLine();

		// We have a file and a line number now. Find the dependencies...
		List<Dependency> deps = new LinkedList<Dependency>();
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

		if(!deps.isEmpty()) {
			// The top dependency is the selected dependency (?)
			Dependency dep = deps.get(0);
			selectedPackageLabel.setText(dep.getName() + " " + dep.getVersion());
			dependencyViewer.setInput(deps);

			PackageResource pkg = dep.getPackage();
			versionViewer.setInput(pkg);
		}
	}

}