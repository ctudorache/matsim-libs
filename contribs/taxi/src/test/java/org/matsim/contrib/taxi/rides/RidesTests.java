package org.matsim.contrib.taxi.rides;

import org.apache.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.taxi.rides.util.GridNetworkGenerator;
import org.matsim.contrib.taxi.rides.util.PartialEvent;
import org.matsim.contrib.taxi.rides.util.TestScenarioGenerator;
import org.matsim.contrib.taxi.rides.util.Utils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

public class RidesTests {
	private static final Logger log = Logger.getLogger(RidesTests.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testOrderExpires() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		testScenario.getTaxiCfg().setMaxSearchDuration(65.0); // order issued at: 00:05 and should expire in 65 sec => 70 sec

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger("passenger_1", gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addPassenger("passenger_2", gn.linkId(0, 1, 0, 2), gn.linkId(2, 2, 2, 1), 5.0);
		testScenario.addVehicle("taxi_vehicle_1", gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(0.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(Matchers.is(1.0), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(Matchers.is(5.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(Utils.matcherAproxTime(70.0), PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));
	}

	@Test
	public void testDriverAcceptanceDelay() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		testScenario.getTaxiCfg().setMaxSearchDuration(65.0); // order should expire in 65 seconds
		final double driverAcceptanceDelay = 15; // it takes driver 15 seconds to accept the order
		testScenario.getTaxiCfg().setRequestAcceptanceDelay(driverAcceptanceDelay);

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
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		final double driverAcceptanceDelay = 15; // it takes driver 15 seconds to accept the order
		testScenario.getTaxiCfg().setRequestAcceptanceDelay(driverAcceptanceDelay);

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
	public void testOrderExpiresDuringDriverAcceptanceDelay() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		final double orderExpiresSec = 25.0;
		testScenario.getTaxiCfg().setMaxSearchDuration(orderExpiresSec);
		final double driverAcceptanceDelay = 35; // order should expire while waiting for driver confirmation
		testScenario.getTaxiCfg().setRequestAcceptanceDelay(driverAcceptanceDelay);

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
	public void testBatchedDispatching() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		testScenario.getTaxiCfg().setRequestAcceptanceDelay(0.0);
		final int batchDuration = 15; // batch size in seconds
		testScenario.getRuleBasedTaxiOptimizerParams().setReoptimizationTimeStep(batchDuration);

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
}
