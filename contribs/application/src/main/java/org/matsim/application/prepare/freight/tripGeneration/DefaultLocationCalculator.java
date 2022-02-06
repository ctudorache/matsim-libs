package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultLocationCalculator implements FreightAgentGenerator.LocationCalculator {
    private final Random rnd = new Random(5678);
    private final LanduseOptions landUse;
    private final Network network;
    private final Map<String, List<Id<Link>>> mapping = new HashMap<>();
    private final ShpOptions shp;
    private static final String lookUpTablePath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/" +
            "scenarios/countries/de/german-wide-freight/v2/processed-data/complete-lookup-table.csv"; // This one is now fixed

    public DefaultLocationCalculator(Network network, Path shpFilePath, LanduseOptions landUse) throws IOException {
        this.shp = new ShpOptions(shpFilePath, "EPSG:4326", StandardCharsets.ISO_8859_1);
        // Reading shapefile from URL may not work properly, therefore users may need to download the shape file to the local directory
        this.landUse = landUse;
        this.network = network;
        prepareMapping();
    }

    private void prepareMapping() throws IOException {
        ShpOptions.Index index = shp.createIndex("EPSG:25832", "NUTS_ID"); // network CRS: EPSG:25832
        ShpOptions.Index landIndex = landUse.getIndex("EPSG:25832"); //TODO
        List<Link> links = network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
                .collect(Collectors.toList());
        Map<String, List<Link>> nutsToLinksMapping = new HashMap<>();
        Map<String, List<Link>> filteredNutsToLinksMapping = new HashMap<>();

        for (Link link : links) {
            String nutsId = index.query(link.getToNode().getCoord());
            if (nutsId != null) {
                nutsToLinksMapping.computeIfAbsent(nutsId, l -> new ArrayList<>()).add(link);
                if (landIndex != null) {
                    if (!landIndex.contains(link.getToNode().getCoord())) {
                        continue;
                    }
                    filteredNutsToLinksMapping.computeIfAbsent(nutsId, l -> new ArrayList<>()).add(link);
                }
            }
        }

        // When filtered links list is not empty, then we use filtered links list. Otherwise, we use the full link lists in the NUTS region.
        for (String nutsId : filteredNutsToLinksMapping.keySet()) {
            nutsToLinksMapping.put(nutsId, filteredNutsToLinksMapping.get(nutsId));
        }

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(lookUpTablePath), StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String verkehrszelle = record.get(0);
                String nuts2021 = record.get(3);
                if (!nuts2021.equals("")) {
                    mapping.put(verkehrszelle, nutsToLinksMapping.get(nuts2021).stream().map(Identifiable::getId).collect(Collectors.toList()));
                    continue;
                }
                Coord backupCoord = new Coord(Double.parseDouble(record.get(5)), Double.parseDouble(record.get(6)));
                CoordinateTransformation ct = new GeotoolsTransformation("EPSG:4326", "EPSG:25832");
                Coord transformedCoord = ct.transform(backupCoord);
                Link backupLink = NetworkUtils.getNearestLink(network, transformedCoord);
                assert backupLink != null;
                mapping.put(verkehrszelle, List.of(backupLink.getId()));
            }
        }
    }

    @Override
    public Id<Link> getLocationOnNetwork(String verkehrszelle) {
        int size = mapping.get(verkehrszelle).size();
        return mapping.get(verkehrszelle).get(rnd.nextInt(size));
    }
}
