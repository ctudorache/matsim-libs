/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.taxi.run;

import org.apache.log4j.Logger;
import org.matsim.contrib.taxi.analysis.TaxiModeAnalysisModule;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;
import org.matsim.core.utils.misc.DiagnosticLog;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModeTaxiModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(MultiModeTaxiModule.class);

	@Inject
	private MultiModeTaxiConfigGroup multiModeTaxiCfg;

	@Override
	public void install() {
		log.log(DiagnosticLog.info, "CTudorache MultiModeTaxiModule.install: #" + multiModeTaxiCfg.getModalElements().size());
		for (TaxiConfigGroup taxiCfg : multiModeTaxiCfg.getModalElements()) {
			log.log(DiagnosticLog.info, "CTudorache MultiModeTaxiModule.install: >> taxiCfg: " + taxiCfg);
			install(new TaxiModeModule(taxiCfg));
			installQSimModule(new TaxiModeQSimModule(taxiCfg));
			install(new TaxiModeAnalysisModule(taxiCfg));
		}
	}
}
