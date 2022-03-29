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

public class ExpireOrderTest {
	private static final Logger log = Logger.getLogger(ExpireOrderTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testExpireOrder() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		testScenario.getTaxiCfg().setMaxSearchDuration(65.0); // order issued at: 00:05 and should expire in 65 sec => 70 sec

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger(1, gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addPassenger(2, gn.linkId(0, 1, 0, 2), gn.linkId(2, 2, 2, 1), 5.0);
		testScenario.addVehicle(1, gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

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

		testScenario.addPassenger(1, gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addVehicle(1, gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

		List<Event> allEvents = testScenario.createController().run();

		Utils.logEvents(log, allEvents);
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(Matchers.is(0.0), PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(Utils.matcherAproxTime(driverAcceptanceDelay), PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(null, PassengerPickedUpEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));
	}
}
