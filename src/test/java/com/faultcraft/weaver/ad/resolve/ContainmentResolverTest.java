package com.faultcraft.weaver.ad.resolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.model.BloodHoundContainer;
import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundGroup;
import com.faultcraft.weaver.ad.model.BloodHoundOU;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundUser;

class ContainmentResolverTest {

    private static final String GUID_OU1 = "{11111111-1111-1111-1111-111111111111}";
    private static final String GUID_CONT1 = "{22222222-2222-2222-2222-222222222222}";

    @Test
    void resolve_userUnderOU_setsContainedBy() {
        var ou = makeOU("OU=Users,DC=test,DC=local", GUID_OU1);
        var user = makeUser("CN=jdoe,OU=Users,DC=test,DC=local", "S-1-5-21-1-2-3-1001");

        List<BloodHoundObject> objects = List.of(ou, user);
        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);

        ContainmentResolver.resolve(new ArrayList<>(objects), dnIndex);

        assertEquals(1, ou.getChildObjects().size());
        assertEquals("S-1-5-21-1-2-3-1001", ou.getChildObjects().get(0).get("ObjectIdentifier"));
        assertEquals("Users", ou.getChildObjects().get(0).get("ObjectType"));

        Map<String, Object> json = user.toJson();
        @SuppressWarnings("unchecked")
        Map<String, Object> containedBy = (Map<String, Object>) json.get("ContainedBy");
        assertEquals(GUID_OU1, containedBy.get("ObjectIdentifier"));
        assertEquals("Ous", containedBy.get("ObjectType"));
    }

    @Test
    void resolve_ouUnderDomain_addsChild() {
        var domain = makeDomain("DC=test,DC=local", "S-1-5-21-1-2-3");
        var ou = makeOU("OU=Users,DC=test,DC=local", GUID_OU1);

        List<BloodHoundObject> objects = List.of(domain, ou);
        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);

        ContainmentResolver.resolve(new ArrayList<>(objects), dnIndex);

        assertEquals(1, domain.getChildObjects().size());
        assertEquals(GUID_OU1, domain.getChildObjects().get(0).get("ObjectIdentifier"));
    }

    @Test
    void resolve_containerUnderDomain_addsChild() {
        var domain = makeDomain("DC=test,DC=local", "S-1-5-21-1-2-3");
        var container = makeContainer("CN=Users,DC=test,DC=local", GUID_CONT1);

        List<BloodHoundObject> objects = List.of(domain, container);
        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);

        ContainmentResolver.resolve(new ArrayList<>(objects), dnIndex);

        assertEquals(1, domain.getChildObjects().size());
    }

    @Test
    void resolve_groupUnderContainer_setsContainedBy() {
        var container = makeContainer("CN=Users,DC=test,DC=local", GUID_CONT1);
        var group = makeGroup("CN=Admins,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-512");

        List<BloodHoundObject> objects = List.of(container, group);
        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);

        ContainmentResolver.resolve(new ArrayList<>(objects), dnIndex);

        assertEquals(1, container.getChildObjects().size());

        Map<String, Object> json = group.toJson();
        @SuppressWarnings("unchecked")
        Map<String, Object> containedBy = (Map<String, Object>) json.get("ContainedBy");
        assertEquals(GUID_CONT1, containedBy.get("ObjectIdentifier"));
    }

    @Test
    void resolve_noParent_skipped() {
        var user = makeUser("CN=jdoe,OU=Missing,DC=test,DC=local", "S-1-5-21-1-2-3-1001");

        List<BloodHoundObject> objects = List.of(user);
        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);

        ContainmentResolver.resolve(new ArrayList<>(objects), dnIndex);

        Map<String, Object> json = user.toJson();
        @SuppressWarnings("unchecked")
        Map<String, Object> containedBy = (Map<String, Object>) json.get("ContainedBy");
        assertTrue(containedBy.isEmpty());
    }

    @Test
    void resolve_domainNotContained() {
        var parentDomain = makeDomain("DC=test,DC=local", "S-1-5-21-1-2-3");

        List<BloodHoundObject> objects = List.of(parentDomain);
        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);

        ContainmentResolver.resolve(new ArrayList<>(objects), dnIndex);

        Map<String, Object> json = parentDomain.toJson();
        assertNull(json.get("ContainedBy"));
    }

    private static BloodHoundDomain makeDomain(String dn, String sid) {
        return BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid)));
    }

    private static BloodHoundOU makeOU(String dn, String guid) {
        return BloodHoundOU.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid)));
    }

    private static BloodHoundUser makeUser(String dn, String sid) {
        return BloodHoundUser.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("jdoe")));
    }

    private static BloodHoundGroup makeGroup(String dn, String sid) {
        return BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("Admins")));
    }

    private static BloodHoundContainer makeContainer(String dn, String guid) {
        return BloodHoundContainer.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid)));
    }

    private static Map<String, BloodHoundObject> buildDnIndex(List<BloodHoundObject> objects) {
        var index = new HashMap<String, BloodHoundObject>();
        for (BloodHoundObject obj : objects) {
            String dn = obj.getProperties().getString("distinguishedname");
            if (!dn.isEmpty()) {
                index.put(dn.toUpperCase(Locale.ROOT), obj);
            }
        }
        return index;
    }
}
