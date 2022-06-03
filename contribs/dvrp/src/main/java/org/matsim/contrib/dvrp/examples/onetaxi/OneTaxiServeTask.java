/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import static org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiOptimizer.OneTaxiTaskType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.core.utils.misc.DiagnosticLog;

/**
 * @author michalm
 */
public class OneTaxiServeTask extends DefaultStayTask {
    private static final Logger log = Logger.getLogger(OneTaxiServeTask.class);
	private final OneTaxiRequest request;

	public OneTaxiServeTask(OneTaxiTaskType taskType, double beginTime, double endTime, Link link,
			OneTaxiRequest request) {
		super(taskType, beginTime, endTime, link);
		this.request = request;
	    log.log(DiagnosticLog.debug, "CTudorache new OneTaxiServerTask: " + this);
	}

	public OneTaxiRequest getRequest() {
		return request;
	}
}
