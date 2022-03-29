package org.matsim.contrib.taxi.rides.util;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarks;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.taxi.analysis.TaxiEventSequenceCollector;
import org.matsim.contrib.taxi.benchmark.TaxiBenchmarkStats;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;
import java.util.Map;

public class TestScenarioGenerator {
	private static final Logger log = Logger.getLogger(TestScenarioGenerator.class);

	private Config config;
	private Scenario scenario;
	private Controler controler;

	private VehicleType taxiVehicleType;

	public TestScenarioGenerator() {
		buildConfig();
	}

	private void buildConfig() {
		TaxiConfigGroup taxiCfgGen = new TaxiConfigGroup();
		taxiCfgGen.setBreakSimulationIfNotAllRequestsServed(false);
		taxiCfgGen.setDestinationKnown(false);
		taxiCfgGen.setVehicleDiversion(false);
		taxiCfgGen.setPickupDuration(120);
		taxiCfgGen.setDropoffDuration(60);
		taxiCfgGen.setTimeProfiles(true);
		taxiCfgGen.setDetailedStats(true);

		RuleBasedTaxiOptimizerParams ruleParams = new RuleBasedTaxiOptimizerParams();
		ruleParams.setReoptimizationTimeStep(1);
		ruleParams.setNearestVehiclesLimit(30);
		taxiCfgGen.addParameterSet(ruleParams);
		log.warn("CTudorache taxiCfgGen: " + taxiCfgGen);

		MultiModeTaxiConfigGroup multiModeTaxiConfigGroup = new MultiModeTaxiConfigGroup();
		multiModeTaxiConfigGroup.addParameterSet(taxiCfgGen);

		config = ConfigUtils.createConfig(multiModeTaxiConfigGroup, new DvrpConfigGroup());
		log.warn("CTudorache modules: " + config.getModules().keySet());


		config.controler().setOutputDirectory("test/output/abcdef");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.controler().setDumpDataAtEnd(false);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
		config.planCalcScore().addParam("activityType_0", "dummy");
		config.planCalcScore().addParam("activityTypicalDuration_0", "24:00:00");
		config.planCalcScore().addParam("traveling_taxi", "-6");
		config.planCalcScore().addParam("activityTypicalDuration_1", "28800");

		DvrpBenchmarks.adjustConfig(config);

		// create scenario
		scenario = ScenarioUtils.createScenario(config);

		// create taxiVehicleType
		taxiVehicleType = VehicleUtils.createVehicleType(Id.create("taxiType", VehicleType.class));
		taxiVehicleType.getCapacity().setSeats(4);
		scenario.getVehicles().addVehicleType(taxiVehicleType);

		// sanity check
		log.warn("CTudorache resultTaxiCfg: " + getTaxiCfg());
		log.warn("CTudorache resultRuleParams: " + getRuleBasedTaxiOptimizerParams());
	}

	// TODO(CTudorache): could be moved to buildConfig(), after scenario creation
	public GridNetworkGenerator buildGridNetwork(int xNodes, int yNodes) {
		return new GridNetworkGenerator(scenario.getNetwork(), xNodes, yNodes, Map.of());
	}

	public Config getConfig() {
		return config;
	}
	public TaxiConfigGroup getTaxiCfg() {
		return TaxiConfigGroup.getSingleModeTaxiConfig(config);
	}
	public RuleBasedTaxiOptimizerParams getRuleBasedTaxiOptimizerParams() {
		TaxiConfigGroup taxiCfg = getTaxiCfg();
		return (RuleBasedTaxiOptimizerParams) taxiCfg.getTaxiOptimizerParams();
	}

	public void addPassenger(String passengerId, Id<Link> fromLink, Id<Link> toLink, Double departureTime) {
		// TODO(CTudorache): utility method for generating passenger id: "passenger_%d"
		Person person = PopulationUtils.getFactory().createPerson(Id.create(passengerId, Person.class));
		PersonUtils.setEmployed(person, false);

		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "dummy", fromLink);
		a.setEndTime(departureTime);

		Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.taxi);

		PopulationUtils.createAndAddActivityFromLinkId(plan, "dummy", toLink);
		scenario.getPopulation().addPerson(person);
	}

	public void addVehicle(String vehicleId, Id<Link> startLink, Double t0, Double t1) {
		Vehicle v = VehicleUtils.createVehicle(Id.create(vehicleId, Vehicle.class), taxiVehicleType);
		v.getAttributes().putAttribute("dvrpMode", "taxi");
		v.getAttributes().putAttribute("startLink", startLink.toString());
		v.getAttributes().putAttribute("serviceBeginTime", t0);
		v.getAttributes().putAttribute("serviceEndTime", t1);
		scenario.getVehicles().addVehicle(v);
	}

	public TestScenarioGenerator createController() {
        Assert.assertNull(controler);
		controler = new Controler(scenario);
		DvrpBenchmarks.initController(controler);

		final String mode = TransportMode.taxi;
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(mode));

		controler.addOverridingModule(new MultiModeTaxiModule());

		controler.addOverridingModule(new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				bindModal(TaxiBenchmarkStats.class).toProvider(modalProvider(
						getter -> new TaxiBenchmarkStats(getter.get(OutputDirectoryHierarchy.class),
								getter.getModal(ExecutedScheduleCollector.class),
								getter.getModal(TaxiEventSequenceCollector.class)))).asEagerSingleton();
				addControlerListenerBinding().to(modalKey(TaxiBenchmarkStats.class));
			}
		});

		return this;
	}

	public List<Event> run() {
	  EventsCollector collector = new EventsCollector();
	  controler.getEvents().addHandler(collector);
	  controler.run();
	  return collector.getEvents();
	}
}
