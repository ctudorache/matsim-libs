package org.matsim.contrib.taxi.scheduler.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.internal.HasPersonId;

public abstract class DriverConfirmationBaseEvent extends Event implements HasPersonId {

	private final Id<Request> requestId;
	private final Id<Person> passengerId;
	private final Id<DvrpVehicle> vehicleId;

	public DriverConfirmationBaseEvent(double time, Id<Request> requestId, Id<Person> passengerId, Id<DvrpVehicle> vehicleId) {
		super(time);
		this.requestId = requestId;
		this.passengerId = passengerId;
		this.vehicleId = vehicleId;
	}

	@Override
	public Id<Person> getPersonId() {
		return passengerId;
	}

	public Id<Request> getRequestId() {
		return requestId;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}
}
