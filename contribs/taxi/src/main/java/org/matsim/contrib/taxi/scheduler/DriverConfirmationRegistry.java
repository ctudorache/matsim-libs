package org.matsim.contrib.taxi.scheduler;

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.events.DriverConfirmationCompletedEvent;
import org.matsim.contrib.taxi.scheduler.events.DriverConfirmationCreatedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.utils.misc.DiagnosticLog;

import java.util.ArrayList;
import java.util.List;

public class DriverConfirmationRegistry {
	private static final Logger log = Logger.getLogger(DriverConfirmationRegistry.class);
	private final TaxiConfigGroup taxiCfg;
	private final MobsimTimer timer;
	private final EventsManager eventsManager;

	// TODO(CTudorache): performance
	private final List<DriverConfirmation> confirmations = new ArrayList<>();

	public DriverConfirmationRegistry(TaxiConfigGroup taxiCfg, MobsimTimer timer, EventsManager eventsManager) {
		this.taxiCfg = taxiCfg;
		this.timer = timer;
		this.eventsManager = eventsManager;
	}

	public DriverConfirmation addDriverConfirmation(TaxiRequest req, DvrpVehicle vehicle, VrpPathWithTravelData pathToPickup) {
		DriverConfirmation dc = new DriverConfirmation(req, vehicle, pathToPickup, timer.getTimeOfDay() + taxiCfg.getDriverConfirmationDelay());
		updateDriverConfirmation(dc);
		if (!dc.isComplete()) {
			log.log(DiagnosticLog.info, "CTudorache addDriverConfirmation: " + dc);
			confirmations.add(dc);
		}
		eventsManager.processEvent(new DriverConfirmationCreatedEvent(timer.getTimeOfDay(), req.getId(), req.getPassengerId(), vehicle.getId()));
		return dc;
	}

	public boolean removeDriverConfirmation(TaxiRequest req) {
		DriverConfirmation dc = getDriverConfirmation(req);
		if (dc == null) {
			return false;
		}
		removeDriverConfirmation(dc);
		return true;
	}

	public void removeDriverConfirmation(DriverConfirmation dc) {
		log.log(DiagnosticLog.info, "CTudorache removeDriverConfirmation: " + dc);
		confirmations.remove(dc);
		eventsManager.processEvent(new DriverConfirmationCompletedEvent(
				timer.getTimeOfDay(), dc.request.getId(), dc.request.getPassengerId(), dc.vehicle.getId(), dc.isAccepted()));
	}

	public DriverConfirmation getDriverConfirmation(TaxiRequest req) {
		for (var dc : confirmations) {
			if (dc.request == req) {
				return dc;
			}
		}
		return null;
	}
	public DriverConfirmation getDriverConfirmation(DvrpVehicle v) {
		for (var dc : confirmations) {
			if (dc.vehicle == v) {
				return dc;
			}
		}
		return null;
	}

	public boolean isWaitingDriverConfirmation(DvrpVehicle v) {
		return getDriverConfirmation(v) != null;
	}
	public boolean isWaitingDriverConfirmation(TaxiRequest r) {
		return getDriverConfirmation(r) != null;
	}

	// set decision for those that are due
	public void updateForCurrentTime() {
		for (DriverConfirmation dc : confirmations) {
			updateDriverConfirmation(dc);
		}
	}

	private void updateDriverConfirmation(DriverConfirmation dc) {
		if (!dc.isComplete() && dc.endTime <= timer.getTimeOfDay()) {
			dc.setComplete(true); // auto-accept
			log.log(DiagnosticLog.info, "CTudorache DriverConfirmation complete: " + dc);
		}
	}
}
