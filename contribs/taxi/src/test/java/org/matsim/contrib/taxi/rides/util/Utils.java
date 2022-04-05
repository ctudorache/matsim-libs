package org.matsim.contrib.taxi.rides.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.locationtech.jts.util.AssertionFailedException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import java.util.*;

public class Utils {
	private static List<Link> generateNetwork(Network network) {
		network.setCapacityPeriod(Time.parseTime("1:00:00"));
		var node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		var node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(100, 0));
		var node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(850, 0));
		var node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(1600, 0));
		var node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(1700, 0));
		var node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(1800, 0));

		var link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), node1, node2, 105, 100, 3600, 1);
		var link2 = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node2, node3, 750, 100, 3600, 1);
		var link3 = NetworkUtils.createAndAddLink(network, Id.create("3", Link.class), node3, node4, 750, 100, 3600, 1);
		var link4 = NetworkUtils.createAndAddLink(network, Id.create("4", Link.class), node4, node5, 105, 100, 360, 1);
		var link5 = NetworkUtils.createAndAddLink(network, Id.create("5", Link.class), node5, node6, 105, 100, 3600, 1);

		return List.of(link1, link2, link3, link4, link5);
	}

	public static void logEvents(Logger log, List<Event> events) {
	  logEvents(log, events, Integer.MAX_VALUE);
	}
	public static void logEvents(Logger log, List<Event> events, int count) {
	  log.info("Events: #" + events.size() + ":");
	  for (int i = 0; i < events.size() && i < count; ++i) {
	    log.info(" - " + events.get(i));
	  }
	  if (count < events.size()) {
	    log.info(" - ... total: " + events.size());
	  }
	}

	private static <T> boolean matchersEqual(Matcher<T> a, Matcher<T> b) {
		return Objects.equals(a.toString(), b.toString());
	}

	public static void expectEvents(List<Event> actual, List<PartialEvent> expectedEventList) {
		if (expectedEventList.isEmpty()) {
			return;
		}

		// Build an ordered map of: time -> list of events
		// Purpose: events with the same time can appear in any order.
		List<Tuple<Matcher<Double>, Set<PartialEvent>>> expectedPartials = new ArrayList<>();
		for (PartialEvent ev : expectedEventList) {
			if (expectedPartials.isEmpty() || !matchersEqual(expectedPartials.get(expectedPartials.size() - 1).getFirst(), ev.time)) {
				expectedPartials.add(new Tuple<>(ev.time, new HashSet<>()));
			}
			expectedPartials.get(expectedPartials.size() - 1).getSecond().add(ev);
		}
		System.out.println("expectedPartials: " + strArray(expectedPartials));

		var expectedIt = expectedPartials.iterator();
		Tuple<Matcher<Double>, Set<PartialEvent>> expectedEvs = expectedIt.next();
		for (Event actualEv : actual) {
			boolean typeMatch = expectedEvs.getSecond().stream().anyMatch(
					pEv -> Objects.equals(actualEv.getEventType(), pEv.type));

			if (typeMatch) {
				Optional<PartialEvent> matcher = expectedEvs.getSecond().stream().filter(pEv -> pEv.matches(actualEv)).findFirst();
				if (matcher.isEmpty()) {
					Assert.fail("Event mismatch:" +
							"\n - expected one of: " + strArray(expectedEvs.getSecond(), expectedEvs.getSecond().size(), 1) +
							"\n - actual: " + actualEv);
				}
				expectedEvs.getSecond().remove(matcher.get());

				if (expectedEvs.getSecond().isEmpty()) {
					if (!expectedIt.hasNext()) {
						return;
					}
					expectedEvs = expectedIt.next();
				}
			}
		}
		Assert.fail("Event not found: " + strArray(expectedEvs.getSecond()));
	}

	public static Matcher<Double> matcherAproxTime(Double t) {
		return Matchers.both(Matchers.greaterThanOrEqualTo(t)).and(Matchers.lessThanOrEqualTo(t + 30));
	}

	public static double nextBatchedDispatchingTime(int batchDuration, double t) {
		return Math.floorDiv((int)(t + batchDuration), batchDuration) * batchDuration;
	}

	/** Serialize to string the first 'count' elements of the array. Useful for debug printing. */
	public static <T> String strArray(Collection<T> arr, int count, int indent) {
		StringBuilder s = new StringBuilder();
		s.append("#" + arr.size() + "\n");
		String strIndent = StringUtils.repeat(" ", indent * 2);
		int printCount = 0;
		for (T elem : arr) {
			s.append(strIndent + " - " + elem + "\n");
			printCount += 1;
			if (printCount >= count) {
				break;
			}
		}
		if (count < arr.size()) {
			s.append(strIndent + " ... omitted");
		}
		return s.toString();
	}

	public static <T> String strArray(Collection<T> arr, int count) {
		return strArray(arr, count, 0);
	}

	public static <T> String strArray(Collection<T> arr) {
		return strArray(arr, arr.size(), 0);
	}
}
