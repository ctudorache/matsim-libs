package org.matsim.contrib.taxi.scheduler.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;

public class DriverConfirmationCreatedEvent extends DriverConfirmationBaseEvent {
	public static String EVENT_TYPE = "driverConfirmationCreated";

	public DriverConfirmationCreatedEvent(double time, Id<Request> requestId, Id<Person> passengerId, Id<DvrpVehicle> vehicleId) {
		super(time, requestId, passengerId, vehicleId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
