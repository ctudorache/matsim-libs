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

package org.matsim.contrib.taxi.schedule;

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.EMPTY_DRIVE;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultDriveTask;

import com.google.common.base.Preconditions;
import org.matsim.contrib.taxi.passenger.TaxiRequest;

import javax.annotation.Nullable;

public class TaxiEmptyDriveTask extends DefaultDriveTask {
	public static final TaxiTaskType TYPE = new TaxiTaskType(EMPTY_DRIVE);
	@Nullable
	private TaxiRequest request;

	public TaxiEmptyDriveTask(TaxiRequest request, VrpPathWithTravelData path, TaxiTaskType taskType) {
		super(taskType, path);
		this.request = request;
		Preconditions.checkArgument(taskType.getBaseType().get() == EMPTY_DRIVE);
	}

	public TaxiRequest getRequest() {
		return request;
	}

	public void setRequest(TaxiRequest request) {
		this.request = request;
	}
}
