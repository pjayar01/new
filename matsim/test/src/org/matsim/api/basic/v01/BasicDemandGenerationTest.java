/* *********************************************************************** *
 * project: org.matsim.*
 * BasicDemandGenerationTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.api.basic.v01;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;
import org.matsim.api.basic.v01.population.BasicRoute;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 *
 */
public class BasicDemandGenerationTest extends MatsimTestCase {

	
	private static final String populationFile = "population.xml";
	private final double homeEndTime = 9*3600.0;
	private final double workEndTime = 19*3600.0;
	private List<Id> ids = new ArrayList<Id>();
	private final BasicScenario sc = new BasicScenarioImpl();
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		for (int i = 1; i <= 6; i++){
			ids.add(sc.createId(Integer.toString(i)));
		}
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	public void testDemandGeneration(){
		Config conf = sc.getConfig();
		assertNotNull(conf);
		
		BasicPopulation pop = sc.getPopulation();
		BasicPopulationBuilder builder = pop.getPopulationBuilder();
		BasicPerson person;
		BasicPlan plan;
		BasicActivity activity;
		BasicLeg leg;
		BasicRoute route;
		
		for (int i = 0; i < ids.size(); i++){
			//create the person and add it to the population
			person = builder.createPerson(ids.get(i));
			//person should be created
			assertNotNull(person);
			//but not added to population
			assertEquals(i, pop.getPersons().size());
			pop.getPersons().put(person.getId(), person);
			assertEquals(i+1, pop.getPersons().size());
			//create the plan and add it to the person
			plan = builder.createPlan(person);
			assertNotNull(plan);
			assertEquals(plan.getPerson(), person);
			assertEquals(0, person.getPlans().size());
			person.getPlans().add(plan);
			assertEquals(1, person.getPlans().size());
			//create the plan elements
			activity = builder.createActivityFromLinkId("h", ids.get(0));
			assertNotNull(activity);
			assertEquals(0, plan.getPlanElements().size());
			//this should be called addActivity
			plan.addActivity(activity);
			assertEquals(1, plan.getPlanElements().size());
			activity.setEndTime(homeEndTime);
			
			leg = builder.createLeg(TransportMode.car);
			assertNotNull(leg);
			assertEquals(1, plan.getPlanElements().size());
			plan.addLeg(leg);
			assertEquals(2, plan.getPlanElements().size());
			route = builder.createRoute(ids.get(0), ids.get(2), ids.subList(1, 2));
			assertNotNull(route);
			assertNull(leg.getRoute());
			//we cannot add routes to legs as they cann't be written by the writers
//			leg.setRoute(route);
			
			activity = builder.createActivityFromLinkId("w", ids.get(2));
			assertNotNull(activity);
			activity.setEndTime(workEndTime);
			assertEquals(2, plan.getPlanElements().size());
			plan.addActivity(activity);
			assertEquals(3, plan.getPlanElements().size());
			
			leg = builder.createLeg(TransportMode.car);
			assertNotNull(leg);
			assertEquals(3, plan.getPlanElements().size());
			plan.addLeg(leg);
			assertEquals(4, plan.getPlanElements().size());
			route = builder.createRoute(ids.get(2), ids.get(0), ids.subList(3, 6));
			assertNotNull(route);
			assertNull(leg.getRoute());
			//we cannot add routes to legs as they cann't be written by the writers
//			leg.setRoute(route);
			
			activity = builder.createActivityFromLinkId("h", ids.get(0));
			assertNotNull(activity);
			assertEquals(4, plan.getPlanElements().size());
			plan.addActivity(activity);
			assertEquals(5, plan.getPlanElements().size());
			
		}
		
		//write created population
		PopulationWriter writer = new PopulationWriter(pop, this.getOutputDirectory() + populationFile);
		writer.write();
		File outfile = new File(this.getOutputDirectory() + populationFile);
		assertTrue(outfile.exists());
		
		
		//read population again, now the code gets really ugly, dirty and worth to refactor...
		Scenario scenario = new ScenarioImpl();
		Population population  = scenario.getPopulation();
		NetworkLayer network = (NetworkLayer)scenario.getNetwork();
		//this is really ugly...
		Node n1 = network.getFactory().createNode(ids.get(0), scenario.createCoord(0.0, 0.0), null);
		network.getNodes().put(n1.getId(), n1);
		Node n2 = network.getFactory().createNode(ids.get(1), scenario.createCoord(0.0, 0.0), null);
		network.getNodes().put(n2.getId(), n2);
		for (Id id : ids){
			Link l = network.getFactory().createLink(id, n1, n2, network, 23.0, 23.0, 23.0, 1.0);
			network.getLinks().put(l.getId(), l);
		}
		
		MatsimPopulationReader reader = new  MatsimPopulationReader(population, network);
		reader.readFile(outfile.getAbsolutePath());
		checkContent(population);
	}
	
	private void checkContent(Population population) {
		assertNotNull(population);
		assertEquals(ids.size(), population.getPersons().size());
		Person pers;
		Plan p;
		for (Id id : ids){
			pers = population.getPersons().get(id);
			assertNotNull(pers);
			assertNotNull(pers.getPlans());
			assertEquals(1, pers.getPlans().size());
			p = pers.getPlans().get(0);
			assertNotNull(p);
			for (int i = 0; i < p.getPlanElements().size(); i++){
				BasicPlanElement element = p.getPlanElements().get(i);
				assertNotNull(element);
			}
			assertEquals(this.homeEndTime, p.getFirstActivity().getEndTime(), EPSILON);
			assertEquals(ids.get(0), p.getFirstActivity().getLinkId());
			assertEquals(this.workEndTime, ((BasicActivity)p.getPlanElements().get(2)).getEndTime(), EPSILON);
			assertEquals(ids.get(2), ((BasicActivity)p.getPlanElements().get(2)).getLinkId());
			assertEquals(Time.UNDEFINED_TIME, p.getLastActivity().getEndTime(), EPSILON);
			assertEquals(ids.get(0), p.getLastActivity().getLinkId());
			
			
			assertEquals(TransportMode.car, p.getNextLeg(p.getFirstActivity()).getMode());
			assertNull(p.getNextLeg(p.getFirstActivity()).getRoute());
			assertEquals(TransportMode.car, p.getPreviousLeg(p.getLastActivity()).getMode());
			assertNull(p.getPreviousLeg(p.getLastActivity()).getRoute());
		}
		
		
	}

}
