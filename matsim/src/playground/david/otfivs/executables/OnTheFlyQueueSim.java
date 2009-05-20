/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.david.otfivs.executables;

import java.rmi.RemoteException;

import org.matsim.analysis.LegHistogram;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.vis.otfvis.server.OTFQuadFileHandler;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSim extends QueueSimulation{
	protected OnTheFlyServer myOTFServer = null;
	protected LegHistogram hist = null;

	public static void runnIt() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		OnTheFlyClientQuad.main(new String []{"rmi:127.0.0.1:4019:AName1"});
	}

	@Override
	protected void prepareSim() {
		this.myOTFServer = OnTheFlyServer.createInstance("AName1", this.network, this.plans, getEvents(), false);
		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		super.prepareSim();

		this.hist = new LegHistogram(300);
		getEvents().addHandler(this.hist);

		// FOR TESTING ONLY!
		new Thread(){@Override
		public void run(){runnIt();}}.start();
	}

	@Override
	protected void cleanupSim() {

		this.myOTFServer.cleanup();
		super.cleanupSim();
	}

	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);

		this.myOTFServer.updateStatus(time);
	}

	public OnTheFlyQueueSim(final NetworkLayer net, final Population plans, final Events events) {
		super(net, plans, events);
	}


	public static void main(final String[] args) {

		String studiesRoot = "../";
		String localDtdBase = "../matsimJ/dtd/";


		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
//		String netFileName = "examples/equil/network.xml";
//		String popFileName = "examples/equil/plans100.xml"; // 15931 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";

		Config config = Gbl.createConfig(args);

		config.global().setLocalDtdBase(localDtdBase);
		config.simulation().setFlowCapFactor(0.02);
		config.simulation().setStorageCapFactor(0.02);

		if(args.length >= 1) {
			netFileName = config.network().getInputFile();
			popFileName = config.plans().getInputFile();
			worldFileName = config.world().getInputFile();
		}

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);

		Population population = new PopulationImpl();
		// Read plans file with special Reader Implementation
		PopulationReader plansReader = new MatsimPopulationReader(population, net);
		plansReader.readFile(popFileName);

		Events events = new Events();

		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().removeStuckVehicles(true);
		config.simulation().setStuckTime(100);
		//config.simulation().setEndTime(Time.parseTime("07:02:00"));
		config.network().setInputFile(netFileName);

		config.simulation().setSnapshotFormat("none");
		config.simulation().setSnapshotPeriod(10);
		config.simulation().setSnapshotFile("./output/remove_thisB");


		OnTheFlyQueueSim sim = new OnTheFlyQueueSim(net, population, events);


		sim.run();

		Gbl.printElapsedTime();


	}

	public void setOtfwriter(final OTFQuadFileHandler.Writer  otfwriter) {
		//this.otfwriter = otfwriter;
	}


}
