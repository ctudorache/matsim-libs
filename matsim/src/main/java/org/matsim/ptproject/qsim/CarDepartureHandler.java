/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.routes.NetworkRoute;

public class CarDepartureHandler implements DepartureHandler {

	private static Logger log = Logger.getLogger(CarDepartureHandler.class);

	private final QSim queueSimulation;

	private boolean teleportVehicles = true;

	private int cntTeleportVehicle = 0;

	public CarDepartureHandler(QSim queueSimulation) {
		this.queueSimulation = queueSimulation;
	}

	@Override
	public void handleDeparture(double now, PersonAgent agent, Id linkId,
			Leg leg) {
		if (leg.getMode().equals(TransportMode.car)) {
			if ( agent instanceof PersonDriverAgent ) {
				handleCarDeparture(now, (PersonDriverAgent)agent, linkId, leg);
			} else {
				throw new UnsupportedOperationException("wrong agent type to use a car") ;
			}
		}
	}

	private void handleCarDeparture(double now, PersonDriverAgent agent, Id linkId,
			Leg leg) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		Id vehicleId = route.getVehicleId();
		if (vehicleId == null) {
			vehicleId = agent.getPerson().getId(); // backwards-compatibility
		}
		QLink qlink = queueSimulation.getQNetwork().getQLink(linkId);
		QVehicle vehicle = qlink.removeParkedVehicle(vehicleId);
		if ((vehicle == null) && (teleportVehicles) && (agent instanceof QPersonAgent)) {
			// try to fix it somehow
			vehicle = ((QPersonAgent) agent).getVehicle();
			if (vehicle.getCurrentLink() != null) {
				if (cntTeleportVehicle < 9) {
					cntTeleportVehicle++;
					log.info("teleport vehicle " + vehicle.getId() + " from link " + vehicle.getCurrentLink().getId() + " to link " + linkId);
					if (cntTeleportVehicle == 9) {
						log.info("No more occurrences of teleported vehicles will be reported.");
					}
				}
				QLink qlinkOld = queueSimulation.getQNetwork().getQLink(vehicle.getCurrentLink().getId());
				qlinkOld.removeParkedVehicle(vehicle.getId());
			}
		}
		if (vehicle == null) {
			throw new RuntimeException("vehicle not available for agent " + agent.getPerson().getId() + " on link " + linkId);
		}
		vehicle.setDriver(agent);

		if ((route.getEndLinkId().equals(linkId)) && (agent.chooseNextLinkId() == null)) {
			// yyyy this should be handled at person level, not vehicle level.  kai, feb'10
			agent.legEnds(now);
			qlink.addParkedVehicle(vehicle);
		} else {
			qlink.addDepartingVehicle(vehicle);
		}
	}

	/** Specifies whether the simulation should track vehicle usage and throw an Exception
	 * if an agent tries to use a car on a link where the car is not available, or not.
	 * Set <code>teleportVehicles</code> to <code>true</code> if agents always have a
	 * vehicle available. If the requested vehicle is parked somewhere else, the vehicle
	 * will be teleported to wherever it is requested to for usage. Set to <code>false</code>
	 * will generate an Exception in the case when an tries to depart with a car on link
	 * where the car is not parked.
	 *
	 * @param teleportVehicles
	 */
	public void setTeleportVehicles(final boolean teleportVehicles) {
		this.teleportVehicles = teleportVehicles;
	}

}
