/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.vsp.congestion.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;

/** 
 * 
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to freespeed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. In this implementation the causing agent for a delay resulting from the storage capacity is assumed to be the agent who caused the spill-back at the bottleneck link.
 * That is, the affected agent keeps the delay caused by the storage capacity constraint until he/she reaches the bottleneck link and (hopefully) identifies there the agent who is causing the spill-backs.
 * 
 * @author ikaddoura
 *
 */
public final class CongestionHandlerImplV3 implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler,
Wait2LinkEventHandler,
PersonArrivalEventHandler,
ActivityEndEventHandler,
CongestionInternalization {

	private final static Logger log = Logger.getLogger(CongestionHandlerImplV3.class);

	private CongestionInfoHandler delegate;

	private final Map<Id<Person>, Double> agentId2storageDelay = new HashMap<Id<Person>, Double>();
	private double delayNotInternalized_spillbackNoCausingAgent = 0.;
	private double totalDelay = 0.;

	private Scenario scenario;
	private EventsManager events;

	public CongestionHandlerImplV3(EventsManager events, Scenario scenario) {
		this.scenario = scenario;
		this.events = events;

		this.delegate = new CongestionInfoHandler(events, scenario);
	}

	@Override
	public final void reset(int iteration) {

		delegate.reset(iteration);

		this.agentId2storageDelay.clear();
		this.delayNotInternalized_spillbackNoCausingAgent = 0.;
		this.totalDelay = 0.;
	}

	@Override
	public final void handleEvent(TransitDriverStartsEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonStuckEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(Wait2LinkEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void writeCongestionStats(String fileName) {
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Total delay [hours];" + this.totalDelay/ 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + this.delegate.getTotalInternalizedDelay() / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (rounding errors) [hours];" + this.delegate.getDelayNotInternalized_roundingErrors() / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (spill-back related delays without identifiying the causing agent) [hours];" + this.delayNotInternalized_spillbackNoCausingAgent / 3600.);
			bw.newLine();

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Congestion statistics written to " + fileName);	
	}

	@Override
	public final double getTotalDelay() {
		return this.totalDelay;
	}
	
	

	public final double getDelayNotInternalizedSpillbackNoCausingAgent() {
		return this.delayNotInternalized_spillbackNoCausingAgent;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// I read the following as: remove non-allocated delays from agents once they are no longer on a leg. kai, sep'15

		if (this.agentId2storageDelay.get(event.getPersonId()) == null) {
			// skip that person

		} else {
			if (this.agentId2storageDelay.get(event.getPersonId()) != 0.) {
				// log.warn("A delay of " + this.agentId2storageDelay.get(event.getPersonId()) + " sec. resulting from spill-back effects was not internalized. Setting the delay to 0.");
				this.delayNotInternalized_spillbackNoCausingAgent += this.agentId2storageDelay.get(event.getPersonId());
			}
			this.agentId2storageDelay.put(event.getPersonId(), 0.);
		}
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {

		if (this.delegate.getPtVehicleIDs().contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else { // car!
			Id<Person> personId = this.delegate.getVehicleId2personId().get( event.getVehicleId() ) ;

			LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo(event.getLinkId(), delegate.getLinkId2congestionInfo(), scenario);

			AgentOnLinkInfo agentInfo = linkInfo.getAgentsOnLink().get( personId ) ;

			DelayInfo delayInfo = new DelayInfo.Builder().setPersonId( personId ).setLinkEnterTime( agentInfo.getEnterTime() )
					.setFreeSpeedLeaveTime(agentInfo.getFreeSpeedLeaveTime()).setLinkLeaveTime( event.getTime() ).build() ;

			delegate.updateFlowAndDelayQueues(event.getTime(), delayInfo, linkInfo );

			calculateCongestion(event, delayInfo);

			linkInfo.getFlowQueue().add( delayInfo ) ;
			linkInfo.getDelayQueue().add( delayInfo ) ;

			linkInfo.memorizeLastLinkLeaveEvent( event );

			linkInfo.getPersonId2freeSpeedLeaveTime().remove( personId ) ;

			linkInfo.getPersonId2linkEnterTime().remove( personId ) ;

			linkInfo.getAgentsOnLink().remove( event.getPersonId() ) ;
		}
	}


	@Override
	public void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {
		LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());

		// global book-keeping:
		this.totalDelay += delayOnThisLink;

		// Check if this (affected) agent was previously delayed without internalizing the delay.

		double agentDelayWithDelaysOnPreviousLinks = 0.;		

		if (this.agentId2storageDelay.get(event.getVehicleId()) == null) {
			agentDelayWithDelaysOnPreviousLinks = delayOnThisLink;
		} else {
			agentDelayWithDelaysOnPreviousLinks = delayOnThisLink + this.agentId2storageDelay.get(event.getVehicleId());
			this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), 0.);
		}

		if (agentDelayWithDelaysOnPreviousLinks < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");

		} else if (agentDelayWithDelaysOnPreviousLinks == 0.) {
			// The agent was leaving the link without a delay.  Nothing to do ...

		} else {
			// The agent was leaving the link with a delay.

			double storageDelay = this.delegate.computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(), delayInfo, agentDelayWithDelaysOnPreviousLinks);

			if (storageDelay < 0.) {
				throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + storageDelay + ") Aborting...");

			} else if (storageDelay == 0.) {
				// The delay resulting from the storage capacity is 0.

			} else if (storageDelay > 0.) {	
				// Saving the delay resulting from the storage capacity constraint for later when reaching the bottleneck link.
				this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), storageDelay);
			} 
		}
	}

	@Override
	public double getTotalInternalizedDelay() {
		return this.delegate.getTotalInternalizedDelay();
	}

	@Override
	public double getTotalRoundingErrorDelay() {
		return this.delegate.getDelayNotInternalized_roundingErrors();
	}
}