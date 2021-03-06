/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.samples;

import com.aptana.samples.model.SamplesReference;

public interface ISampleListener
{

	/**
	 * Fired when a sample is added.
	 * 
	 * @param sample
	 *            the added sample
	 */
	public void sampleAdded(SamplesReference sample);

	/**
	 * Fired when a sample is removed.
	 * 
	 * @param sample
	 *            the removed sample
	 */
	public void sampleRemoved(SamplesReference sample);
}
