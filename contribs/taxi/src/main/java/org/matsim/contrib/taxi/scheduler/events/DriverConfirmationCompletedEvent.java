package org.matsim.contrib.taxi.scheduler.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;

public class DriverConfirmationCompletedEvent extends DriverConfirmationBaseEvent {
	public static String EVENT_TYPE = "driverConfirmationCompleted";

	private final boolean isAccepted;

	public DriverConfirmationCompletedEvent(double time, Id<Request> requestId, Id<Person> passengerId, Id<DvrpVehicle> vehicleId, boolean isAccepted) {
		super(time, requestId, passengerId, vehicleId);
		this.isAccepted = isAccepted;
	}

	public boolean isAccepted() {
		return isAccepted;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
