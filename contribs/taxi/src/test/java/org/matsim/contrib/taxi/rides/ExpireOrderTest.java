package org.matsim.contrib.taxi.rides;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.rides.util.GridNetworkGenerator;
import org.matsim.contrib.taxi.rides.util.PartialEvent;
import org.matsim.contrib.taxi.rides.util.TestScenarioGenerator;
import org.matsim.contrib.taxi.rides.util.Utils;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.net.URL;
import java.util.List;

public class ExpireOrderTest {
	private static final Logger log = Logger.getLogger(ExpireOrderTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testExpireOrderGen() {
		TestScenarioGenerator testScenario = new TestScenarioGenerator();
		testScenario.getTaxiCfg().setMaxSearchDuration(65.0); // order should expire in 65 seconds

		GridNetworkGenerator gn = testScenario.buildGridNetwork( 3, 3);

		testScenario.addPassenger(1, gn.linkId(0, 1, 0, 0), gn.linkId(2, 0, 2, 1), 0.0);
		testScenario.addPassenger(2, gn.linkId(0, 1, 0, 2), gn.linkId(2, 2, 2, 1), 5.0);
		testScenario.addVehicle(1, gn.linkId(1, 2, 2, 2), 0.0, 1000.0);

		Controler controler = testScenario.createController();

		EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();
		log.warn("allEvents: #" + allEvents.size());
		for (Event ev : allEvents) {
			log.warn(" - " + ev);
		}
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(0.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(1.0, PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(5.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(71.0, PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));

	}

	@Test
	public void testExpireOrderByStaticConfigExample() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("taxi-rides-test-base"), "config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
		taxiCfg.setMaxSearchDuration(65.0); // order should expire in 65 seconds
		taxiCfg.setRequestAcceptanceDelay(0.0);
		RuleBasedTaxiOptimizerParams ruleParams = ((RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams());
		ruleParams.setReoptimizationTimeStep(1);

		// NOTE: These are already set in config.xml
		//config.plans().setInputFile("population_1.xml");
		//taxiCfg.setTaxisFile("vehicles_1.xml");

		config.controler().setOutputDirectory("test/output/abcdef");

		Controler controler = RunTaxiBenchmark.createControler(config, 1);

		EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);

		controler.run();

		List<Event> allEvents = collector.getEvents();
		Utils.expectEvents(allEvents, List.of(
				new PartialEvent(0.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_1",null),
				new PartialEvent(1.0, PassengerRequestScheduledEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1"),
				new PartialEvent(5.0, PassengerRequestSubmittedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(71.0, PassengerRequestRejectedEvent.EVENT_TYPE, "passenger_2",null),
				new PartialEvent(null, PassengerDroppedOffEvent.EVENT_TYPE, "passenger_1","taxi_vehicle_1")
		));
	}
}
