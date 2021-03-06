/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.index;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.index.JSCAFileIndexingParticipant;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.tests.JSEditorBasedTests;
import com.aptana.index.core.FileStoreBuildContext;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;
import com.aptana.index.core.IndexPlugin;

/**
 * JSCAIndexingTests
 */
public class JSCAIndexingTests extends JSEditorBasedTests
{

	private JSIndexQueryHelper queryHelper;
	private URI uri;

	protected void assertProperties(Index index, String typeName, String... propertyNames)
	{
		for (String propertyName : propertyNames)
		{
			List<PropertyElement> property = queryHelper.getTypeMembers(index, typeName, propertyName);

			assertNotNull(typeName + "." + propertyName + " does not exist", property);
			assertFalse(typeName + "." + propertyName + " does not exist", property.isEmpty());
		}
	}

	protected void assertTypes(Index index, String... typeNames)
	{
		for (String typeName : typeNames)
		{
			List<TypeElement> type = queryHelper.getTypes(index, typeName, false);

			assertNotNull(type);
			assertFalse(type.isEmpty());
		}
	}

	protected Index indexResource(String resource) throws CoreException
	{
		IFileStore fileToIndex = getFileStore(resource);
		uri = fileToIndex.toURI();
		Index index = getIndexManager().getIndex(uri);
		JSCAFileIndexingParticipant indexer = new JSCAFileIndexingParticipant();

		indexer.index(new FileStoreBuildContext(fileToIndex), index, new NullProgressMonitor());

		return index;
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		queryHelper = new JSIndexQueryHelper();
		uri = null;
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		if (uri != null)
		{
			getIndexManager().removeIndex(uri);
			uri = null;
		}

		queryHelper = null;

		super.tearDown();
	}

	public void testSimpleType() throws Exception
	{
		Index index = indexResource("metadata/typeOnly.jsca");

		// check type
		assertTypes(index, "SimpleType");

		// check for global
		List<PropertyElement> global = queryHelper.getGlobals(index, "SimpleType");
		assertNotNull(global);
		assertFalse(global.isEmpty());
	}

	public void testSimpleInternalType() throws Exception
	{
		Index index = indexResource("metadata/typeInternal.jsca");

		// check type
		assertTypes(index, "SimpleType");

		// check for global
		List<PropertyElement> global = queryHelper.getGlobals(index, "SimpleType");
		assertNotNull(global);
		assertTrue(global.isEmpty());
	}

	public void testNamespacedType() throws Exception
	{
		Index index = indexResource("metadata/namespacedType.jsca");

		// check types
		assertTypes(index, "com", "com.aptana", "com.aptana.SimpleType");

		// check for properties
		assertProperties(index, "Window", "com");
		assertProperties(index, "com", "aptana");
		assertProperties(index, "com.aptana", "SimpleType");
	}

	public void testNamespacedTypeInternal() throws Exception
	{
		Index index = indexResource("metadata/namespacedTypeInternal.jsca");

		// check types
		assertTypes(index, "com", "com.aptana", "com.aptana.SimpleType");

		// check for global
		List<PropertyElement> global = queryHelper.getGlobals(index, "com");
		assertNotNull(global);
		assertTrue(global.isEmpty());
	}

	public void testNamespacedTypeMixed() throws Exception
	{
		Index index = indexResource("metadata/namespacedTypeMixed.jsca");

		// check types
		assertTypes(index, "com", "com.aptana", "com.aptana.SimpleType", "com.aptana.SimpleType2");

		// check for properties
		assertProperties(index, "Window", "com");
		assertProperties(index, "com", "aptana");
		assertProperties(index, "com.aptana", "SimpleType2");
	}

	public void testIsInternalProposals() throws Exception
	{
		// grab source file URI
		IFileStore sourceFile = getFileStore("metadata/isInternalProperty.js");
		uri = sourceFile.toURI();

		// index jsca file
		IFileStore fileToIndex = getFileStore("metadata/namespacedTypeMixed.jsca");
		Index index = getIndexManager().getIndex(uri);
		JSCAFileIndexingParticipant indexer = new JSCAFileIndexingParticipant();
		indexer.index(new FileStoreBuildContext(fileToIndex), index, new NullProgressMonitor());

		// setup editor and CA context
		setupTestContext(sourceFile);

		// get proposals
		int offset = cursorOffsets.get(0);
		ITextViewer viewer = new TextViewer(new Shell(), SWT.NONE);
		viewer.setDocument(this.document);
		ICompletionProposal[] proposals = this.processor.computeCompletionProposals(viewer, offset, '\0', false);

		// build a list of display names
		ArrayList<String> names = new ArrayList<String>();

		for (ICompletionProposal proposal : proposals)
		{
			// we need to check if it is a valid proposal given the context
			if (proposal instanceof ICompletionProposalExtension2)
			{
				ICompletionProposalExtension2 p = (ICompletionProposalExtension2) proposal;
				if (p.validate(document, offset, null))
				{
					names.add(proposal.getDisplayString());
				}
			}
			else
			{
				names.add(proposal.getDisplayString());
			}
		}

		assertFalse("SimpleType should not exist in the proposal list", names.contains("SimpleType"));
		assertTrue("SimpleType2 does not exist in the proposal list", names.contains("SimpleType2"));
	}

	/**
	 * Test for TISTUD-1327
	 * 
	 * @throws CoreException
	 */
	public void testTypeUserAgentsOnProperty() throws CoreException
	{
		Index index = indexResource("metadata/userAgentOnType.jsca");

		// make sure target type exists
		List<TypeElement> tiAPI = queryHelper.getTypes(index, "Titanium.API", false);
		assertNotNull("There should be at least one type for Titanium.API", tiAPI);
		assertTrue("There should be one type for Titanium.API", tiAPI.size() == 1);

		// confirm parent type exists
		List<TypeElement> ti = queryHelper.getTypes(index, "Titanium", true);
		assertNotNull("There should be at least one type for Titanium", ti);
		assertTrue("There should be one type for Titanium", ti.size() == 1);

		// grab property for Titanium.API
		TypeElement t = ti.get(0);
		PropertyElement p = t.getProperty("API");
		assertNotNull(p);

		Set<String> uas = CollectionsUtil.newSet("android", "iphone", "ipad", "mobileweb");
		List<String> missing = new ArrayList<String>();
		for (String ua : p.getUserAgentNames())
		{
			if (!uas.contains(ua))
			{
				missing.add(ua);
			}
		}
		assertTrue("The following user agents were missing: " + StringUtil.join(", ", missing), missing.isEmpty());
	}

	protected IndexManager getIndexManager()
	{
		return IndexPlugin.getDefault().getIndexManager();
	}
}
