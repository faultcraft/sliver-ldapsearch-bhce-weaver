package com.faultcraft.weaver.ad.resolve;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.BloodHoundComputer;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundUser;
import com.faultcraft.weaver.util.Log;

/**
 * Resolves constrained delegation targets from SPN strings to computer SIDs.
 *
 * <p>Extracts hostnames from SPNs (e.g. "cifs/server.domain.local") and resolves
 * them to computer object SIDs via dnshostname matching. Resolved targets are
 * added to the delegating object's Aces as AllowedToDelegate edges.
 */
final class DelegationResolver {

    private DelegationResolver() {}

    static void resolve(List<BloodHoundObject> objects) {
        List<BloodHoundComputer> computers = objects.stream()
                .filter(o -> o instanceof BloodHoundComputer)
                .map(o -> (BloodHoundComputer) o)
                .toList();

        int edgeCount = 0;
        for (BloodHoundObject obj : objects) {
            List<String> spns = getDelegationSpns(obj);
            if (spns.isEmpty()) {
                continue;
            }

            for (String spn : spns) {
                String host = extractHostFromSpn(spn);
                if (host.isEmpty()) {
                    continue;
                }

                BloodHoundComputer target = findComputerByHostname(host, computers);
                if (target != null && !target.getObjectIdentifier().equals(
                        obj.getObjectIdentifier())) {
                    obj.getAces().add(Map.of(
                            "PrincipalSID", obj.getObjectIdentifier(),
                            "PrincipalType", capitalize(obj.objectType()),
                            "RightName", "AllowedToDelegate",
                            "IsInherited", false));
                    edgeCount++;
                }
            }
        }

        if (edgeCount > 0) {
            Log.info("Resolved %d constrained delegation edges", edgeCount);
        }
    }

    private static List<String> getDelegationSpns(BloodHoundObject obj) {
        if (obj instanceof BloodHoundUser || obj instanceof BloodHoundComputer) {
            return obj.getProperties().getStringList("allowedtodelegate");
        }
        return List.of();
    }

    static String extractHostFromSpn(String spn) {
        int slash = spn.indexOf('/');
        if (slash < 0 || slash >= spn.length() - 1) {
            return "";
        }
        String host = spn.substring(slash + 1);
        int colon = host.indexOf(':');
        if (colon > 0) {
            host = host.substring(0, colon);
        }
        return host;
    }

    private static BloodHoundComputer findComputerByHostname(
            String hostname, List<BloodHoundComputer> computers) {
        String lower = hostname.toLowerCase(Locale.ROOT);
        for (BloodHoundComputer comp : computers) {
            String compHost = comp.getProperties().getString("dnshostname");
            if (lower.equals(compHost.toLowerCase(Locale.ROOT))) {
                return comp;
            }
        }
        return null;
    }

    private static String capitalize(String objectType) {
        if (objectType.isEmpty()) {
            return objectType;
        }
        return objectType.substring(0, 1).toUpperCase(Locale.ROOT)
                + objectType.substring(1);
    }
}
