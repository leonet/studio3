package com.aptana.editor.js.text;

import java.net.URI;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.junit.Test;

import com.aptana.editor.js.JSSourceEditor;
import com.aptana.editor.js.contentassist.index.IJSIndexConstants;
import com.aptana.editor.js.tests.JSEditorBasedTests;

public class JSTextHoverTest extends JSEditorBasedTests
{

	private JSTextHover hover;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		hover = new JSTextHover();
	}

	@Override
	protected void tearDown() throws Exception
	{
		hover = null;
		super.tearDown();
	}

	@Test
	public void test1236()
	{
		setupTestContext("hover/1236.js");
		getIndex()
				.addEntry(
						IJSIndexConstants.PROPERTY,
						"Window win2 {\"name\":\"win2\",\"description\":\"\",\"since\":[],\"userAgents\":null,\"owningType\":\"Window\",\"isClassProperty\":false,\"isInstanceProperty\":false,\"isInternal\":false,\"types\":[],\"examples\":[]}",
						URI.create("file:/Users/cwilliams/workspaces/runtime-Titanium/something/Resources/app.js"));

		IRegion hoverRegion = hover.getHoverRegion(getSourceViewer(), this.cursorOffsets.get(0));
		assertNotNull(hoverRegion);
		assertEquals("Incorrect hover region returned", new Region(4, 4), hoverRegion);

		Object info = hover.getHoverInfo2(getSourceViewer(), hoverRegion);
		assertNotNull("Should have gotten docs on the 'win2' variable we're assigning to!", info);
	}

	@Test
	public void testVariableReference()
	{
		setupTestContext("hover/var_ref.js");
		getIndex()
				.addEntry(
						IJSIndexConstants.PROPERTY,
						"Window win2 {\"name\":\"win2\",\"description\":\"\",\"since\":[],\"userAgents\":null,\"owningType\":\"Window\",\"isClassProperty\":false,\"isInstanceProperty\":false,\"isInternal\":false,\"types\":[],\"examples\":[]}",
						URI.create("file:/Users/cwilliams/workspaces/runtime-Titanium/something/Resources/app.js"));

		IRegion hoverRegion = hover.getHoverRegion(getSourceViewer(), this.cursorOffsets.get(0));
		assertNotNull(hoverRegion);
		assertEquals("Incorrect hover region returned", new Region(91, 4), hoverRegion);

		Object info = hover.getHoverInfo2(getSourceViewer(), hoverRegion);
		assertNotNull("Should have gotten docs on the 'win2' variable we're assigning to!", info);
	}

	protected ISourceViewer getSourceViewer()
	{
		return getJSEditor().getISourceViewer();
	}

	protected JSSourceEditor getJSEditor()
	{
		return (JSSourceEditor) this.editor;
	}

}
