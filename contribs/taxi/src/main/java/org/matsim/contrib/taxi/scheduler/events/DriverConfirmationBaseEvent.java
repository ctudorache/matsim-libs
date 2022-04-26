package org.matsim.contrib.taxi.scheduler.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.internal.HasPersonId;

import java.util.Map;

public abstract class DriverConfirmationBaseEvent extends Event implements HasPersonId {

	public static final String ATTRIBUTE_REQUEST = "request";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

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

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_REQUEST, requestId.toString());
		attr.put(ATTRIBUTE_VEHICLE, vehicleId.toString());
		return attr;
	}
}
