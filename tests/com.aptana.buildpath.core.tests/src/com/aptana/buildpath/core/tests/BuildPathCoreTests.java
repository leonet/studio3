package com.aptana.buildpath.core.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.aptana.buildpath.core.BuildPathEntryTest;
import com.aptana.core.build.CoreBuildTests;
import com.aptana.core.internal.build.InternalBuildTests;

public class BuildPathCoreTests extends TestCase
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite(BuildPathCoreTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTest(CoreBuildTests.suite());
		suite.addTest(InternalBuildTests.suite());
		suite.addTestSuite(BuildPathEntryTest.class);
		// $JUnit-END$
		return suite;
	}

}
