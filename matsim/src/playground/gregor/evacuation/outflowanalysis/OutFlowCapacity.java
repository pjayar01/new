/* *********************************************************************** *
 * project: org.matsim.*
 * OutFlowCapacity.java
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

package playground.gregor.evacuation.outflowanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

public class OutFlowCapacity {

	private static Logger log = Logger.getLogger(OutFlowCapacity.class);
	private final NetworkLayer network;
	private double outFlowCapcity = 0;
	private final Set<LinkImpl> outFlowLinks = new HashSet<LinkImpl>();

	public OutFlowCapacity(NetworkLayer network) {
		this.network = network;
	}

	public void run() {
		parseOutLinks();
		parseOutNodes();
	}
	
	private void parseOutNodes() {
		NodeImpl node = this.network.getNode("en1");
		double cap = 0;
		ArrayList<NodeImpl> outNode = new ArrayList<NodeImpl>();
		for (LinkImpl l : node.getInLinks().values()) {
			NodeImpl out = l.getFromNode();
			outNode.add(out);
			for (LinkImpl ll : out.getInLinks().values()) {
				cap += ll.getCapacity(0);
			}
		}
		System.err.println("cap:" + cap);
	
	}
	
	private void parseOutLinks() {
		outFlowCapcity = 0;
		for (LinkImpl link : this.network.getLinks().values()) {
			if (this.outFlowLinks.contains(link)) {
				continue;
			}
			if (link.getId().toString().contains("el")) {
				continue;
			}
			
			for (LinkImpl l2 : link.getToNode().getOutLinks().values()) {
				if (l2.getId().toString().contains("el")) {
					this.outFlowLinks.add(link);
					this.outFlowCapcity += link.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
					break;
				}
				
			}
			

		}
		System.out.println("OutFlowCap:" + this.outFlowCapcity);
	}

	public static void main(String [] args) {
		 String netfile = "./networks/padang_net_evac_v20080618.xml";
		 
			log.info("loading network from " + netfile);
			final NetworkLayer network = new NetworkLayer();
			new MatsimNetworkReader(network).readFile(netfile);
//			world.setNetworkLayer(network);
//			world.complete();
			log.info("done.");
			
			new OutFlowCapacity(network).run();
	}
	
}
