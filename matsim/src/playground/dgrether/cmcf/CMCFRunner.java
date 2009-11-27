/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.cmcf;


import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.trafficmonitoring.LinkSensorManager;
import org.matsim.run.OTFVis;

import playground.dgrether.linkanalysis.InOutGraphWriter;
import playground.dgrether.linkanalysis.TTGraphWriter;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFRunner {
	
	private static final Logger log = Logger.getLogger(CMCFRunner.class);
	
	private static final boolean visualizationOnly = true ;
	
	
	public CMCFRunner() {
		Config config = null;
		CMCFScenarioGenerator.main(null);
		final TTInOutflowEventHandler myHandler = new TTInOutflowEventHandler(new IdImpl("4"));
		
		if (!visualizationOnly) {			
			Controler controler = new Controler(CMCFScenarioGenerator.configOut);
			final LinkSensorManager lsm = new LinkSensorManager();
			lsm.addLinkSensor(new IdImpl("3"));
			lsm.addLinkSensor(new IdImpl("4"));
			controler.setOverwriteFiles(true);
//			controler.addControlerListener(new StartupListener() {
//				public void notifyStartup(StartupEvent e) {
//					e.getControler().getEvents().addHandler(lsm);
//					LinkTravelTimeCounter.init(e.getControler().getEvents(), e.getControler().getNetwork().getLinks().size());
//					
////					e.getControler().getEvents().addHandler(myHandler);
//				}});
			
			

			final TTInOutflowEventHandler handler3 = new TTInOutflowEventHandler(new IdImpl("3"), new IdImpl("5"));
			final TTInOutflowEventHandler handler4 = new TTInOutflowEventHandler(new IdImpl("4"));
			
			
			
			controler.addControlerListener(new StartupListener() {
				public void notifyStartup(StartupEvent e) {
					e.getControler().getEvents().addHandler(handler3);
					e.getControler().getEvents().addHandler(handler4);
					
//					e.getControler().getEvents().addHandler(myHandler);
				}});
			
			 
			controler.addControlerListener(new IterationEndsListener() {

				public void notifyIterationEnds(IterationEndsEvent event) {
					TTGraphWriter ttWriter = new TTGraphWriter();
					ttWriter.addTTEventHandler(handler3);
					ttWriter.addTTEventHandler(handler4);
					ttWriter.writeTTChart(event.getControler().getIterationPath(event.getIteration()), event.getIteration());
					InOutGraphWriter inoutWriter = new InOutGraphWriter();
					inoutWriter.addInOutEventHandler(handler3);
					inoutWriter.addInOutEventHandler(handler4);
					inoutWriter.writeInOutChart(event.getControler().getIterationPath(event.getIteration()), event.getIteration());
				}
				
			});
	
			
			controler.run();
			NetworkLayer net = controler.getNetwork();
			LinkImpl link2 = net.getLink(new IdImpl("2"));
			LinkImpl link3 = net.getLink(new IdImpl("3"));
			LinkImpl link4 = net.getLink(new IdImpl("4"));
			LinkImpl link5 = net.getLink(new IdImpl("5"));

//			double tt2 = controler.getTravelTimeCalculator().getLinkTravelTime(link2, 7.0 * 3600.0);
//			double tt3 = controler.getTravelTimeCalculator().getLinkTravelTime(link3, 7.0 * 3600.0);
//			double tt4 = controler.getTravelTimeCalculator().getLinkTravelTime(link4, 7.0 * 3600.0);
//			
//			log.info("avg tt on link 2 at 7:00: " + tt2);
//			log.info("avg tt on link 3 at 7:00: " + tt3);
//			log.info("avg tt on link 4 at 7:00: " + tt4);
//			
//			
//			log.info("traffic on link 3: " + lsm.getLinkTraffic("3"));		
//			log.info("traffic on link 4: " + lsm.getLinkTraffic("4"));
//			
//			log.info("Last travel time on 2: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("2"));
//			log.info("Last travel time on 3: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("3"));
//			log.info("Last travel time on 4: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("4"));
			for (int i = 1; i <= 3600; i++) {
				double tt = controler.getTravelTimeCalculator().getLinkTravelTime(link4, i);
				double tt3 = controler.getTravelTimeCalculator().getLinkTravelTime(link3, i);
				double tt5 = controler.getTravelTimeCalculator().getLinkTravelTime(link5, i);
				log.info("tt on link 4 at time: " + i + " " + tt);
				log.info("tt on link 3 at time: " + i + " " + tt3);
				log.info("tt on link 5 at time: " + i + " " + tt5);
				
			}
			
			config = controler.getConfig();
		  
//			writeGraphs(myHandler, config.controler().getOutputDirectory());
		}
		if (config == null) {
			config = new Config();
			config.addCoreModules();
			config = MatsimIo.loadConfig(config, CMCFScenarioGenerator.configOut);
		}
		
		System.out.println(config.controler());
		
		String[] args = {config.controler().getOutputDirectory() + "/ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".otfvis.mvi"};
		
		OTFVis.main(args);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CMCFRunner();
		
	}

}
