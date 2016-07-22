/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Test;

/**
 * @author nagel
 *
 */
public class RunConfigurableQNetworkFactoryExampleTest {

	/**
	 * Test method for {@link org.matsim.core.mobsim.qsim.qnetsimengine.RunConfigurableQNetworkFactoryExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		try {
			RunConfigurableQNetworkFactoryExample.main(null);
		} catch ( Exception ee ) {
			throw new RuntimeException("something went wrong") ;
		}
		
		
	}

}
