/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.taxi.scheduler.DriverConfirmation;
import org.matsim.contrib.taxi.scheduler.DriverConfirmationRegistry;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class AssignmentRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = Logger.getLogger(AssignmentRequestInserter.class);
	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final MobsimTimer timer;
	private final AssignmentTaxiOptimizerParams params;

	private final VehicleAssignmentProblem<TaxiRequest> assignmentProblem;
	private final TaxiToRequestAssignmentCostProvider assignmentCostProvider;

	public AssignmentRequestInserter(Fleet fleet, Network network, MobsimTimer timer, TravelTime travelTime,
									 TravelDisutility travelDisutility, TaxiScheduler scheduler,
									 AssignmentTaxiOptimizerParams params) {
		this(fleet, timer, network, travelTime, travelDisutility, scheduler, params,
				new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime));
	}

	public AssignmentRequestInserter(Fleet fleet, MobsimTimer timer, Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, TaxiScheduler scheduler, AssignmentTaxiOptimizerParams params,
			LeastCostPathCalculator router) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.timer = timer;
		this.params = params;

		assignmentProblem = new VehicleAssignmentProblem<>(network, travelTime, travelDisutility, router,
				params.getNearestRequestsLimit(), params.getNearestVehiclesLimit());

		assignmentCostProvider = new TaxiToRequestAssignmentCostProvider(params);
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		log.debug("CTudorachescheduleUnplannedRequests #" + unplannedRequests.size());

		// schedule requests which are confirmed
		List<DriverConfirmation> requestsToSchedule = new ArrayList<>();
		List<TaxiRequest> requestsToPlan = new ArrayList<>();
		for (TaxiRequest r : unplannedRequests) {
			DriverConfirmation dc = driverConfirmationRegistry().getDriverConfirmation(r);
			if (dc == null) {
				requestsToPlan.add(r);
				continue;
			}
			if (!dc.isComplete()) {
				continue;
			}
			assert dc.isAccepted();
			requestsToSchedule.add(dc);
		}

		// advance request not considered => horizon==0
		AssignmentRequestData rData = AssignmentRequestData.create(timer.getTimeOfDay(), 0, requestsToPlan);
		VehicleData vData = initVehicleData(rData);

		double vehPlanningHorizonSec;
		String vehPlanningHorizonName;
		long idleVehs = fleet.getVehicles().values().stream().filter(scheduler.getScheduleInquiry()::isIdle).count();
		if (idleVehs < rData.getUrgentReqCount()) {
			vehPlanningHorizonSec = params.getVehPlanningHorizonUndersupply();
			vehPlanningHorizonName = "undersupply (veh < req)";
		} else {
			vehPlanningHorizonSec = params.getVehPlanningHorizonOversupply();
			vehPlanningHorizonName = "oversupply (veh >= req)";
		}
		log.debug("CTudorachescheduleUnplannedRequests"
				+ ", req urgent/all: " + rData.getSize() + "/" + rData.getUrgentReqCount()
				+ ", taxi idle/all: " + vData.getSize() + "/" + vData.getIdleCount()
				+ ", horizon: " + vehPlanningHorizonSec + " (" + vehPlanningHorizonName + ")");

		AssignmentCost<TaxiRequest> cost = assignmentCostProvider.getCost(rData, vData);
		List<Dispatch<TaxiRequest>> assignments = assignmentProblem.findAssignments(vData, rData, cost);

		log.debug("CTudorachescheduleUnplannedRequests dispatching: #" + assignments.size());
		for (Dispatch<TaxiRequest> a : assignments) {
			log.warn(" - " + a);
		}

		// create DriverConfirmation for all the requests. If the driver confirmation is instant, then proceed with schedule.
		for (Dispatch<TaxiRequest> a : assignments) {
			DriverConfirmation dc = driverConfirmationRegistry().addDriverConfirmation(a.destination, a.vehicle, a.path);
			if (dc.isComplete()) {
				assert dc.isAccepted();
				requestsToSchedule.add(dc);
			}
		}

		// schedule the confirmed requests
		for (DriverConfirmation dc : requestsToSchedule) {
			assert dc.isAccepted();
			scheduler.scheduleRequest(dc.vehicle, dc.request, dc.getPathToPickup(timer.getTimeOfDay()));
			unplannedRequests.remove(dc.request);
		}

	}

	private VehicleData initVehicleData(AssignmentRequestData rData) {
		long idleVehs = fleet.getVehicles().values().stream().filter(scheduler.getScheduleInquiry()::isIdle).count();
		double vehPlanningHorizon = idleVehs < rData.getUrgentReqCount() ?
				params.getVehPlanningHorizonUndersupply() :
				params.getVehPlanningHorizonOversupply();
		return new VehicleData(timer.getTimeOfDay(), scheduler.getScheduleInquiry(),
				fleet.getVehicles().values().stream(), vehPlanningHorizon, driverConfirmationRegistry());
	}

	private DriverConfirmationRegistry driverConfirmationRegistry() {
		return scheduler.getDriverConfirmationRegistry();
	}
}
