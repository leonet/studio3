/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license-epl.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.index.core;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.index.core.build.BuildContext;

abstract class IndexRequestJob extends Job
{

	private URI containerURI;

	/**
	 * IndexRequestJob
	 * 
	 * @param name
	 * @param containerURI
	 */
	protected IndexRequestJob(String name, URI containerURI)
	{
		super(name);
		this.containerURI = containerURI;
		setRule(IndexManager.MUTEX_RULE);
		setPriority(Job.BUILD);
		// setSystem(true);
	}

	/**
	 * IndexRequestJob
	 * 
	 * @param containerURI
	 */
	protected IndexRequestJob(URI containerURI)
	{
		this(MessageFormat.format(Messages.IndexRequestJob_Name, containerURI.toString()), containerURI);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	@Override
	public boolean belongsTo(Object family)
	{
		if (getContainerURI() == null)
		{
			return family == null;
		}
		if (family == null)
		{
			return false;
		}
		return getContainerURI().equals(family);
	}

	/**
	 * filterFileStores
	 * 
	 * @return
	 */
	protected Set<IFileStore> filterFileStores(Set<IFileStore> fileStores)
	{
		if (!CollectionsUtil.isEmpty(fileStores))
		{
			for (IIndexFilterParticipant filterParticipant : getIndexManager().getFilterParticipants())
			{
				fileStores = filterParticipant.applyFilter(fileStores);
			}
		}

		return fileStores;
	}

	/**
	 * getContainerURI
	 * 
	 * @return
	 */
	protected URI getContainerURI()
	{
		return containerURI;
	}

	/**
	 * getContributedFiles
	 * 
	 * @param container
	 * @return
	 */
	protected Set<IFileStore> getContributedFiles(URI container)
	{
		Set<IFileStore> result = new HashSet<IFileStore>();

		for (IIndexFileContributor contributor : getIndexManager().getFileContributors())
		{
			Set<IFileStore> files = contributor.getFiles(container);

			if (!CollectionsUtil.isEmpty(files))
			{
				result.addAll(files);
			}
		}

		return result;
	}

	/**
	 * getIndex
	 * 
	 * @return
	 */
	protected Index getIndex()
	{
		return getIndexManager().getIndex(getContainerURI());
	}

	protected IndexManager getIndexManager()
	{
		return IndexPlugin.getDefault().getIndexManager();
	}

	/**
	 * Indexes a set of {@link IFileStore}s with the appropriate {@link IFileStoreIndexingParticipant}s that apply to
	 * the content types (matching is done via filename/extension).
	 * 
	 * @param index
	 * @param fileStores
	 * @param monitor
	 * @throws CoreException
	 */
	protected void indexFileStores(Index index, Set<IFileStore> fileStores, IProgressMonitor monitor)
			throws CoreException
	{
		if (index == null)
		{
			return;
		}

		fileStores = filterFileStores(fileStores);
		if (CollectionsUtil.isEmpty(fileStores))
		{
			return;
		}

		int remaining = fileStores.size();
		SubMonitor sub = SubMonitor.convert(monitor, remaining * 11);
		try
		{
			for (IFileStore file : fileStores)
			{
				if (sub.isCanceled())
				{
					throw new CoreException(Status.CANCEL_STATUS);
				}
				// First cleanup old index entries for file
				index.remove(file.toURI());
				sub.worked(1);

				// Now run indexers on file
				List<IFileStoreIndexingParticipant> indexers = getIndexParticipants(file);
				if (!CollectionsUtil.isEmpty(indexers))
				{
					int work = 10 / indexers.size();
					BuildContext context = new FileStoreBuildContext(file);
					for (IFileStoreIndexingParticipant indexer : indexers)
					{
						if (sub.isCanceled())
						{
							throw new CoreException(Status.CANCEL_STATUS);
						}
						try
						{
							indexer.index(context, index, sub.newChild(work));
						}
						catch (CoreException e)
						{
							IdeLog.logError(IndexPlugin.getDefault(), e);
						}
					}
				}
				// Update remaining units
				remaining--;
				sub.setWorkRemaining(remaining * 11);
			}
		}
		finally
		{
			sub.done();
		}
	}

	protected List<IFileStoreIndexingParticipant> getIndexParticipants(IFileStore file)
	{
		return getIndexManager().getIndexParticipants(file.getName());
	}

}
