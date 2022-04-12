package org.matsim.contrib.taxi.rides;

import org.apache.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.rides.util.GridNetworkGenerator;
import org.matsim.contrib.taxi.rides.util.PartialEvent;
import org.matsim.contrib.taxi.rides.util.TestScenarioGenerator;
import org.matsim.contrib.taxi.rides.util.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class RidesTests {
	private static final Logger log = Logger.getLogger(RidesTests.class);

	private final String taxiOptimizerParamsSetName;
	private final AbstractTaxiOptimizerParams taxiOptimizerParams;

	public RidesTests(String taxiOptimizerParamsSetName) {
		this.taxiOptimizerParamsSetName = taxiOptimizerParamsSetName;
		switch (taxiOptimizerParamsSetName) {
			case RuleBasedTaxiOptimizerParams.SET_NAME:
				taxiOptimizerParams = new RuleBasedTaxiOptimizerParams();
				break;
			case AssignmentTaxiOptimizerParams.SET_NAME:
				taxiOptimizerParams = new AssignmentTaxiOptimizerParams();
				break;
			default:
				throw new RuntimeException("Invalid taxiOptimizerParamsSetName: " + taxiOptimizerParamsSetName);
		}
	}

	// Each parameter here will trigger a new RidesTest(...param) which runs all tests.
	@Parameterized.Parameters(name = "{index}: taxiOptimizer={0}")
	public static Collection taxiOptimizers() {
		return Arrays.asList(new Object[][] {
				{ RuleBasedTaxiOptimizerParams.SET_NAME },
				{ AssignmentTaxiOptimizerParams.SET_NAME },
		});
	}

	@Test
	public void orderExpiresDueToNoAvailableVehicle() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator(taxiOptimizerParams);
		testScenario.getTaxiCfg().setMaxSearchDuration(60.0); // order issued at: 00:65 and should expire in 60 sec => 125 sec

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger("passenger_1", gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addPassenger("passenger_2", gn.linkId(0, 1, 0, 2), gn.linkId(2, 2, 2, 1), 65.0);
		testScenario.addVehicle("taxi_vehicle_1", gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(null, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(null, PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(null, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(Utils.matcherAproxTime(125.0), PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));
	}

	@Test
	public void orderScheduledAfterDriverConfirmationDelay() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator(taxiOptimizerParams);
		testScenario.getTaxiCfg().setMaxSearchDuration(125.0); // order should expire in 125 seconds
		final double driverAcceptanceDelay = 60; // it takes driver 60 seconds to accept the order
		testScenario.getTaxiCfg().setDriverConfirmationDelay(driverAcceptanceDelay);

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger("passenger_1", gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addVehicle("taxi_vehicle_1", gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(0.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(Utils.matcherAproxTime(driverAcceptanceDelay), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(null, PassengerPickedUpEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));
	}

	@Test
	public void orderIsSentToAnotherDriverWhenNearbyDriverAlreadyHasAPendingConfirmation() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator(taxiOptimizerParams);
		final double driverAcceptanceDelay = 15; // it takes driver 15 seconds to accept the order
		testScenario.getTaxiCfg().setDriverConfirmationDelay(driverAcceptanceDelay);

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger("passenger_1", gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addPassenger("passenger_2", gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 5.0);
		testScenario.addVehicle("taxi_vehicle_near", gn.linkId(1, 1, 1, 0), 0.0, 1000.0);
		testScenario.addVehicle("taxi_vehicle_far", gn.linkId(2, 1, 2, 0), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(0.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(Matchers.is(5.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(Utils.matcherAproxTime(driverAcceptanceDelay), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_near"),
				new PartialEvent(Utils.matcherAproxTime(5 + driverAcceptanceDelay), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_2","taxi_vehicle_far"),
				new PartialEvent(null, PassengerPickedUpEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_near"),
				new PartialEvent(null, PassengerPickedUpEvent.EVENT_TYPE, "passenger_2","taxi_vehicle_far"),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_near"),
		 		new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_2","taxi_vehicle_far")
		));
	}

	@Test
	public void orderExpiresBecauseDriverAcceptanceDelayIsTooHigh() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator(taxiOptimizerParams);
		final double orderExpiresSec = 25.0;
		testScenario.getTaxiCfg().setMaxSearchDuration(orderExpiresSec);
		final double driverAcceptanceDelay = 35; // order should expire while waiting for driver confirmation
		testScenario.getTaxiCfg().setDriverConfirmationDelay(driverAcceptanceDelay);

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger("passenger_1", gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addVehicle("taxi_vehicle_1", gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(0.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(Utils.matcherAproxTime(orderExpiresSec), PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_1",null)
		));
	}

	@Test
	public void batchedDispatchingSelectsNearbySoonFinishingInsteadOfFartherFreeVehicle() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator(taxiOptimizerParams);
		testScenario.getTaxiCfg().setDriverConfirmationDelay(0.0);
		final int batchDuration = 15; // batch size in seconds
		testScenario.getTaxiOptimizerParams().setReoptimizationTimeStep(batchDuration);

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		// passenger_1 should take vehicle_1 and complete the ride near passenger_2
		final double passenger1OrderSent = 0;
		final double passenger1OrderScheduled = batchDuration;
		testScenario.addPassenger("passenger_1", gn.linkId(0, 0, 0, 1), gn.linkId(0, 1, 1, 1), passenger1OrderSent);
		testScenario.addVehicle("taxi_vehicle_1", gn.linkId(2, 0, 2, 1), 0.0, 1000.0);
		final double passenger1DropoffComplete = 237;

		// passenger_2 departs just before vehicle_1 completes ride, but due to batching passenger_2 should be assigned to vehicle_1 (nearby) instead of the further away vehicle_2
		final double passenger2OrderSent = passenger1DropoffComplete - 3;
		final double passenger2OrderScheduled = Utils.nextBatchedDispatchingTime(batchDuration, passenger2OrderSent);
		testScenario.addPassenger("passenger_2", gn.linkId(0, 1, 0, 2), gn.linkId(2, 2, 2, 1), passenger2OrderSent);
		testScenario.addVehicle("taxi_vehicle_2", gn.linkId(2, 1, 2, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(passenger1OrderSent), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(Utils.matcherAproxTime(passenger1OrderScheduled), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(Matchers.is(passenger2OrderSent), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(Utils.matcherAproxTime(passenger2OrderScheduled), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_2","taxi_vehicle_1"),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_2","taxi_vehicle_1")
		));
	}

	@Test
	public void batchedDispatchingAssignment() {
		// Scenario: 2 passengers, 2 vehicles.
		// - p1 away from: v1 = 1 sec, v2 = 2 sec
		// - p2 away from: v1 = 7 sec, v2 = 100 (due to one-way)
		// Greedy assignment:  p1-v1, p2-v2 => total pickup time: 1 + 100.
		// Optimum assignment: p1-v2, p2-v1 => total pickup time: 2 + 7

		TestScenarioGenerator testScenario = new TestScenarioGenerator(taxiOptimizerParams);
		final int batchDuration = 15; // batch size in seconds
		testScenario.getTaxiOptimizerParams().setReoptimizationTimeStep(batchDuration);

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 6, 6, false);
		gn.addOneWayLink(gn.getNode(2, 1), gn.getNode(2, 2));
		gn.addDoubleLink(gn.getNode(2, 2), gn.getNode(2, 3));
		gn.addOneWayLink(gn.getNode(2, 3), gn.getNode(2, 4));
		gn.addOneWayLink(gn.getNode(2, 4), gn.getNode(3, 4));
		gn.addOneWayLink(gn.getNode(2, 4), gn.getNode(2, 5));
		gn.addOneWayLink(gn.getNode(3, 3), gn.getNode(3, 2));
		gn.addOneWayLink(gn.getNode(3, 2), gn.getNode(2, 2));
		gn.addOneWayLink(gn.getNode(3, 2), gn.getNode(3, 1));
		gn.addOneWayLink(gn.getNode(3, 1), gn.getNode(2, 1));

		gn.addOneWayLink(gn.getNode(0, 3), gn.getNode(0, 2));
		gn.addOneWayLink(gn.getNode(0, 2), gn.getNode(1, 2));
		gn.addOneWayLink(gn.getNode(1, 2), gn.getNode(2, 2));
		gn.addOneWayLink(gn.getNode(0, 2), gn.getNode(0, 1));
		gn.addOneWayLink(gn.getNode(0, 1), gn.getNode(0, 0));
		gn.addOneWayLink(gn.getNode(0, 0), gn.getNode(1, 0));
		gn.addOneWayLink(gn.getNode(1, 0), gn.getNode(2, 0));
		gn.addOneWayLink(gn.getNode(2, 0), gn.getNode(3, 0));
		gn.addOneWayLink(gn.getNode(3, 0), gn.getNode(4, 0));
		gn.addOneWayLink(gn.getNode(4, 0), gn.getNode(5, 0));
		gn.addOneWayLink(gn.getNode(5, 0), gn.getNode(5, 1));
		gn.addOneWayLink(gn.getNode(5, 1), gn.getNode(4, 1));
		gn.addOneWayLink(gn.getNode(4, 1), gn.getNode(3, 1));

		final double passenger1OrderSent = 0;
		final double passenger2OrderSent = batchDuration / 2;
		final double bothPassengersScheduled = batchDuration;
		testScenario.addPassenger("passenger_1", gn.linkId(2, 2, 2, 3), gn.linkId(2, 4, 3, 4), passenger1OrderSent);
		testScenario.addPassenger("passenger_2", gn.linkId(2, 1, 2, 2), gn.linkId(2, 4, 2, 5), passenger2OrderSent);
		testScenario.addVehicle("taxi_vehicle_1", gn.linkId(3, 3, 3, 2), 0.0, 1000.0);
		testScenario.addVehicle("taxi_vehicle_2", gn.linkId(0, 3, 0, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		final boolean isOptimalAssignment = taxiOptimizerParams.getName() == AssignmentTaxiOptimizerParams.SET_NAME;
		final String vehicleForPassenger1 = isOptimalAssignment ? "taxi_vehicle_2" : "taxi_vehicle_1";
		final String vehicleForPassenger2 = isOptimalAssignment ? "taxi_vehicle_1" : "taxi_vehicle_2";
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(passenger1OrderSent), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1", null),
				new PartialEvent(Matchers.is(passenger2OrderSent), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2", null),
				new PartialEvent(Utils.matcherAproxTime(bothPassengersScheduled), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1", vehicleForPassenger1),
				new PartialEvent(Utils.matcherAproxTime(bothPassengersScheduled), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_2", vehicleForPassenger2),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1", vehicleForPassenger1),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_2", vehicleForPassenger2)
		));

	}
}
