package com.faultcraft.weaver.ad.resolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.DomainTrust;

class TrustResolverTest {

    @Test
    void resolve_matchingTrust_addedToDomain() {
        var domain = makeDomain("DC=test,DC=local", "S-1-5-21-1-2-3");
        var trust = makeTrust("CN=child.test.local,CN=System,DC=test,DC=local",
                "child.test.local", "2", "2", "0");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain));
        TrustResolver.resolve(objects, List.of(trust));

        assertEquals(1, domain.getTrusts().size());
        assertEquals("CHILD.TEST.LOCAL", domain.getTrusts().get(0).get("TargetDomainName"));
    }

    @Test
    void resolve_noMatchingDomain_trustNotAdded() {
        var domain = makeDomain("DC=other,DC=local", "S-1-5-21-9-8-7");
        var trust = makeTrust("CN=child.test.local,CN=System,DC=test,DC=local",
                "child.test.local", "2", "2", "0");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain));
        TrustResolver.resolve(objects, List.of(trust));

        assertTrue(domain.getTrusts().isEmpty());
    }

    @Test
    void resolve_emptyTrustList_noop() {
        var domain = makeDomain("DC=test,DC=local", "S-1-5-21-1-2-3");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain));
        TrustResolver.resolve(objects, List.of());

        assertTrue(domain.getTrusts().isEmpty());
    }

    @Test
    void resolve_multipleTrusts_allMatched() {
        var domain = makeDomain("DC=test,DC=local", "S-1-5-21-1-2-3");
        var trust1 = makeTrust("CN=child1.test.local,CN=System,DC=test,DC=local",
                "child1.test.local", "2", "2", "0");
        var trust2 = makeTrust("CN=child2.test.local,CN=System,DC=test,DC=local",
                "child2.test.local", "1", "3", "4");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain));
        TrustResolver.resolve(objects, List.of(trust1, trust2));

        assertEquals(2, domain.getTrusts().size());
    }

    private static BloodHoundDomain makeDomain(String dn, String sid) {
        return BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid)));
    }

    private static DomainTrust makeTrust(String dn, String partner,
            String direction, String type, String attrs) {
        return DomainTrust.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "trustpartner", List.of(partner),
                "trustdirection", List.of(direction),
                "trusttype", List.of(type),
                "trustattributes", List.of(attrs)));
    }
}
