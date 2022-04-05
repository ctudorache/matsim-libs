package org.matsim.contrib.taxi.rides.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Map;

public class GridNetworkGenerator {
	final Network network;

	public enum Param {
		NETWORK_CAPACITY_PERIOD,
		LINK_LENGTH,
		LINK_FREE_SPEED,
		LINK_CAPACITY,
		LINK_LANES,
	};
	public static final String DEFAULT_NETWORK_CAPACITY_PERIOD = "1:00:00";
	public static final Double DEFAULT_LINK_LENGTH = 100.0; // TODO(CTudorache): this should be link_unit_length
	public static final Double DEFAULT_LINK_FREE_SPEED = 15.0;
	public static final Double DEFAULT_LINK_CAPACITY = 100.0;
	public static final Double DEFAULT_LINK_LANES = 1.0;

	final double linkLength;
	final double linkFreeSpeed;
	final double linkCapacity;
	final double linkLanes;

	public GridNetworkGenerator(Network network, int xNodes, int yNodes, Map<Param, Object> params, boolean generateLinks) {
		this.network = network;
		this.network.setCapacityPeriod(Time.parseTime((String)params.getOrDefault(Param.NETWORK_CAPACITY_PERIOD, DEFAULT_NETWORK_CAPACITY_PERIOD)));
		this.linkLength = (Double)params.getOrDefault(Param.LINK_LENGTH, DEFAULT_LINK_LENGTH);
		this.linkFreeSpeed = (Double)params.getOrDefault(Param.LINK_FREE_SPEED, DEFAULT_LINK_FREE_SPEED);
		this.linkCapacity = (Double)params.getOrDefault(Param.LINK_CAPACITY, DEFAULT_LINK_CAPACITY);
		this.linkLanes = (Double)params.getOrDefault(Param.LINK_LANES, DEFAULT_LINK_LANES);
		this.build(xNodes, yNodes, generateLinks);
	}

	public Id<Node> nodeId(int x, int y) {
		return Id.create(String.format("%d_%d", x, y), Node.class);
	}
	public Coord nodeCoord(int x, int y) {
		return new Coord(x * 1000, y * 1000);
	}

	public void addNode(int x, int y) {
		NetworkUtils.createAndAddNode(network, nodeId(x, y), nodeCoord(x, y));
	}

	public Node getNode(int x, int y) {
		return network.getNodes().get(nodeId(x, y));
	}

	public Id<Link> linkId(Node from, Node to) {
		return Id.create(from.getId() + "-" + to.getId(), Link.class);
	}

	public Id<Link> linkId(int fromX, int fromY, int toX, int toY) {
		return linkId(getNode(fromX, fromY), getNode(toX, toY));
	}

	public void addOneWayLink(Node a, Node b) {
		// TODO(CTudorache): link length should be a multiple of link_unit_length (get it by distance a -> b)
		//                   Will allow longer links between far-away nodes (to have realistic length).
		NetworkUtils.createAndAddLink(network, linkId(a, b), a, b, linkLength, linkFreeSpeed, linkCapacity,  linkLanes);
	}

	public void addDoubleLink(Node a, Node b) {
		addOneWayLink(a, b);
		addOneWayLink(b, a);
	}

	private void build(int xNodes, int yNodes, boolean generateLinks) {
		for (int x = 0; x < xNodes; ++x) {
			for (int y = 0; y < yNodes; ++y) {
				addNode(x, y);
			}
		}
		if (generateLinks) {
			for (int x = 0; x < xNodes; ++x) {
				for (int y = 0; y < yNodes; ++y) {
					Node crtNode = getNode(x, y);
					Node leftNode = getNode(x - 1, y);
					Node downNode = getNode(x, y - 1);
					if (leftNode != null) {
						addDoubleLink(leftNode, crtNode);
					}
					if (downNode != null) {
						addDoubleLink(downNode, crtNode);
					}
				}
			}
		}
	}
}
