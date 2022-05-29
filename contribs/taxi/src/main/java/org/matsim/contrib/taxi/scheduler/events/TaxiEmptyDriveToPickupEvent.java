/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.taxi.scheduler.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.AbstractPassengerEvent;
import org.matsim.contrib.taxi.passenger.TaxiRequest;

import java.util.Map;

public class TaxiEmptyDriveToPickupEvent extends AbstractPassengerEvent {
	public static final String EVENT_TYPE = "taxi empty drive to pickup";

	public TaxiEmptyDriveToPickupEvent(double time, Id<Request> requestId, Id<Person> personId,
									   Id<DvrpVehicle> vehicleId) {
		super(time, TransportMode.taxi, requestId, personId, vehicleId);
	}

	public TaxiEmptyDriveToPickupEvent(double time, TaxiRequest request, Id<DvrpVehicle> vehicleId) {
		this(time, request.getId(), request.getPassengerId(), vehicleId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static TaxiEmptyDriveToPickupEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		Id<Person> personId = Id.createPersonId(attributes.get(ATTRIBUTE_PERSON));
		Id<DvrpVehicle> vehicleId = Id.create(attributes.get(ATTRIBUTE_VEHICLE), DvrpVehicle.class);
		return new TaxiEmptyDriveToPickupEvent(time, requestId, personId, vehicleId);
	}
}
