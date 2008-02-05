/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricing.java
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

package org.matsim.controler.corelisteners;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScoringFunctionFactory;
import org.matsim.roadpricing.TollTravelCostCalculator;

/**
 * Integrates the RoadPricing functionality into the MATSim Controler.
 *
 * @author mrieser
 */
public class RoadPricing implements StartupListener, IterationEndsListener{

	private RoadPricingScheme scheme = null;
	private CalcPaidToll tollCalc = null;
	private CalcAverageTolledTripLength cattl = null;
	
	final static private Logger log = Logger.getLogger(RoadPricing.class);

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		// read the road pricing scheme from file
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(controler.getNetwork());
		try {
			rpReader.parse(controler.getConfig().roadpricing().getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.scheme = rpReader.getScheme();

		// add the events handler to calculate the tolls paid by agents
		this.tollCalc = new CalcPaidToll(controler.getNetwork(), this.scheme);
		controler.getEvents().addHandler(this.tollCalc);

		// add the toll-score to the existing scoring function
		controler.setScoringFunctionFactory(
				new RoadPricingScoringFunctionFactory(this.tollCalc, controler.getScoringFunctionFactory()));

		// replace the travelCostCalculator with a toll-dependent one if required
		if ("distance".equals(this.scheme.getType()) || "cordon".equals(this.scheme.getType())) {
			controler.setTravelCostCalculator(new TollTravelCostCalculator(controler.getTravelCostCalculator(), this.scheme));
		}
		
		cattl = new CalcAverageTolledTripLength(controler.getNetwork(), scheme);
		controler.getEvents().addHandler(cattl);
		
		// TODO [MR] I think that the Area-Router is not yet loaded (never was, neither in this nor in the old controler)

	}
	
/*/	public void notifyIterationStarts(IterationStartsEvent event) {
//		int it = event.getIteration();
//		cas.reset(it);
//		ctpf.reset(it);
//		cattl.reset(it);
//	}*/

	public void notifyIterationEnds(IterationEndsEvent event) {
		int it = event.getIteration();
		if (it % 10 == 0) {
			log.info("The sum of all paid tolls : "
					+ tollCalc.getAllAgentsToll() + " �.");
			log.info("The number of people, who paid toll : "
					+ tollCalc.getDraweesNr());
			log.info("The average paid trip length : "
					+ cattl.getAverageTripLength() + " m.");
		}
	}

	public RoadPricingScheme getRoadPricingScheme() {
		return this.scheme;
	}

	public CalcPaidToll getPaidTolls() {
		return this.tollCalc;
	}

	public double getAllAgentsToll(){
		return tollCalc.getAllAgentsToll();
	}
	public int getDraweesNr(){
		return tollCalc.getDraweesNr();
	}
	public double getAvgPaidTripLength(){
		return cattl.getAverageTripLength();
	}
}
