/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.utils.misc.NetworkUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.IdFactory;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFScenarioGenerator {


	private static final Logger log = Logger
			.getLogger(CMCFScenarioGenerator.class);

	private static final String networkFileOld = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoNetworkOldRenamed.xml";

	public static final String networkFileNew = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoNetwork.xml";

//	public static final String networkFile 	= networkFileOld;

	public static final String networkFile 	= networkFileNew;

	private static final String plans1Out = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoPlans.xml";

	private static final String plans2Out = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoPlansAltRoute.xml";

//	public static final String config1Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfig.xml";
//
//	public static final String config2Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";

	public static final String config1Out = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoConfig.xml";

	public static final String config2Out = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";


	public static String configOut, plansOut;

	private static final boolean isAlternativeRouteEnabled = false;

	private static final int iterations = 500;

	private static final int iterations2 = 0;

//	private static final int iterations = 1;

	private Population plans;

	private Config config;

	private NetworkLayer network;

	public CMCFScenarioGenerator() throws Exception {
		init();
		createPlans();
		MatsimIo.writePlans(this.plans, plansOut);
		//set scenario
		this.config.network().setInputFile(networkFile);
		this.config.plans().setInputFile(plansOut);
		//configure scoring for plans
		this.config.charyparNagelScoring().setLateArrival(0.0);
		this.config.charyparNagelScoring().setPerforming(6.0);
		//this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
//		homeParams.setOpeningTime(0);
		this.config.charyparNagelScoring().addActivityParams(homeParams);
		//set it with f. strings
		this.config.charyparNagelScoring().addParam("activityType_0", "h");
		this.config.charyparNagelScoring().addParam("activityTypicalDuration_0", "24:00:00");

		//configure controler
	// configure controler
		this.config.travelTimeCalculator().setTraveltimeBinSize(1);

		this.config.controler().setLastIteration(iterations + iterations2);
		if (isAlternativeRouteEnabled)
			this.config.controler().setOutputDirectory(DgPaths.WSBASE + "testData/output/cmcfAltRoute");
		else
			this.config.controler().setOutputDirectory(DgPaths.WSBASE + "testData/output/cmcf");

		//configure simulation and snapshot writing
		this.config.simulation().setSnapshotFormat("otfvis");
		this.config.simulation().setSnapshotFile("cmcf.mvi");
		this.config.simulation().setSnapshotPeriod(60.0);
		//configure strategies for replanning
		this.config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		selectExp.setProbability(0.9);
		selectExp.setModuleName("SelectExpBeta");
		this.config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(IdFactory.get(2));
		reRoute.setProbability(0.1);
		reRoute.setModuleName("ReRoute");
		reRoute.setDisableAfter(iterations);
		this.config.strategy().addStrategySettings(reRoute);

		MatsimIo.writerConfig(this.config, configOut);

		log.info("scenario written!");
	}

	private void init() {
		if (isAlternativeRouteEnabled) {
			plansOut = plans2Out;
			configOut = config2Out;
		}
		else {
			plansOut = plans1Out;
			configOut = config1Out;
		}
		this.config = new Config();
		Gbl.setConfig(this.config);
		this.config.addCoreModules();

		this.config.plans().setOutputVersion("v4");
		this.config.plans().setOutputFile(plansOut);

		this.network = MatsimIo.loadNetwork(networkFile);
	}

	private void createPlans() throws Exception {
		this.plans = new PopulationImpl(false);
		int firstHomeEndTime = 0;//6 * 3600;
		int homeEndTime = firstHomeEndTime;
		Link l1 = this.network.getLink(IdFactory.get(1));
		Link l6 = this.network.getLink(IdFactory.get(6));

		for (int i = 1; i <= 3600; i++) {
			Person p = new PersonImpl(new IdImpl(i));
			Plan plan = new org.matsim.core.population.PlanImpl(p);
			p.addPlan(plan);
			//home
//			homeEndTime = homeEndTime +  ((i - 1) % 3);
			if ((i-1) % 3 == 0){
				homeEndTime++;
			}
			Activity act1 = plan.createActivity("h", l1.getCoord());
			act1.setLink(l1);
			act1.setEndTime(homeEndTime);
			//leg to home
			Leg leg = plan.createLeg(TransportMode.car);
			NetworkRoute route = new NodeNetworkRoute(l1, l6);
			if (isAlternativeRouteEnabled) {
				route.setNodes(l1, NetworkUtils.getNodes(this.network, "2 3 4 5 6"), l6);
			}
			else {
				route.setNodes(l1, NetworkUtils.getNodes(this.network, "2 3 5 6"), l6);
			}
			leg.setRoute(route);
			Activity act2 = plan.createActivity("h", l6.getCoord());
			act2.setLink(l6);
			this.plans.addPerson(p);
		}
	}









	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new CMCFScenarioGenerator();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
