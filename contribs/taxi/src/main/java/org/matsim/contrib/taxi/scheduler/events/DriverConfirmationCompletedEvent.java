package org.matsim.contrib.taxi.scheduler.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.Map;

public class DriverConfirmationCompletedEvent extends DriverConfirmationBaseEvent {
	public static String EVENT_TYPE = "driverConfirmationCompleted";

	private static String ATTRIBUTE_ACCEPTED = "accepted";

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

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_ACCEPTED, String.valueOf(isAccepted));
		return attr;
	}
}
