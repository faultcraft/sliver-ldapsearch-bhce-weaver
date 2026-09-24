package com.faultcraft.weaver.ad.resolve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.classify.ObjectClassifier;
import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundGroup;
import com.faultcraft.weaver.ad.model.BloodHoundOU;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundUser;
import com.faultcraft.weaver.ad.model.DomainTrust;

class RelationshipResolverTest {

    @Test
    void resolve_fullGraph_containmentAndGroupMembership() {
        var domain = BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of("DC=test,DC=local"),
                "objectsid", List.of("S-1-5-21-1-2-3")));
        var ou = BloodHoundOU.fromEntry(Map.of(
                "distinguishedname", List.of("OU=Users,DC=test,DC=local"),
                "objectguid", List.of("{11111111-1111-1111-1111-111111111111}")));
        var group = BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of("CN=Domain Users,OU=Users,DC=test,DC=local"),
                "objectsid", List.of("S-1-5-21-1-2-3-513"),
                "samaccountname", List.of("Domain Users")));
        var user = BloodHoundUser.fromEntry(Map.of(
                "distinguishedname", List.of("CN=jdoe,OU=Users,DC=test,DC=local"),
                "objectsid", List.of("S-1-5-21-1-2-3-1001"),
                "samaccountname", List.of("jdoe"),
                "memberof", List.of("CN=Domain Users,OU=Users,DC=test,DC=local")));

        var objects = new ArrayList<BloodHoundObject>(List.of(domain, ou, group, user));
        var classified = new ObjectClassifier.ClassificationResult(objects, List.of(), 0);

        var resolver = new RelationshipResolver();
        resolver.resolve(classified);

        assertFalse(domain.getChildObjects().isEmpty());
        assertFalse(ou.getChildObjects().isEmpty());
        assertFalse(group.getMembers().isEmpty());
        assertEquals("S-1-5-21-1-2-3-1001",
                group.getMembers().get(0).get("ObjectIdentifier"));
    }

    @Test
    void resolve_withTrusts_addedToDomain() {
        var domain = BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of("DC=test,DC=local"),
                "objectsid", List.of("S-1-5-21-1-2-3")));
        var trust = DomainTrust.fromEntry(Map.of(
                "distinguishedname", List.of("CN=child.test.local,CN=System,DC=test,DC=local"),
                "trustpartner", List.of("child.test.local"),
                "trustdirection", List.of("2"),
                "trusttype", List.of("2"),
                "trustattributes", List.of("0")));

        var objects = new ArrayList<BloodHoundObject>(List.of(domain));
        var classified = new ObjectClassifier.ClassificationResult(objects, List.of(trust), 0);

        var resolver = new RelationshipResolver();
        resolver.resolve(classified);

        assertEquals(1, domain.getTrusts().size());
    }

    @Test
    void resolve_emptyInput_noErrors() {
        var classified = new ObjectClassifier.ClassificationResult(
                new ArrayList<>(), List.of(), 0);
        var resolver = new RelationshipResolver();
        assertDoesNotThrow(() -> resolver.resolve(classified));
    }
}
