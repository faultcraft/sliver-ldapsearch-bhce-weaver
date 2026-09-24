package com.faultcraft.weaver.ad.resolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundGPO;
import com.faultcraft.weaver.ad.model.BloodHoundOU;
import com.faultcraft.weaver.ad.model.BloodHoundObject;

class GpoLinkResolverTest {

    private static final String GUID_GPO1 = "{AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA}";
    private static final String GUID_GPO2 = "{BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB}";
    private static final String GUID_OU1 = "{CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC}";
    private static final String GUID_GPO_A = "{DDDDDDDD-DDDD-DDDD-DDDD-DDDDDDDDDDDD}";
    private static final String GUID_GPO_B = "{EEEEEEEE-EEEE-EEEE-EEEE-EEEEEEEEEEEE}";

    @Test
    void resolve_domainGpLink_createsLinkEntry() {
        var gpo = makeGPO("CN={ABC-123},CN=Policies,CN=System,DC=test,DC=local", GUID_GPO1);
        var domain = makeDomainWithGpLink(
                "DC=test,DC=local", "S-1-5-21-1-2-3",
                "CN={ABC-123},CN=Policies,CN=System,DC=test,DC=local", "0");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain, gpo));
        var dnIndex = buildDnIndex(objects);

        GpoLinkResolver.resolve(objects, dnIndex);

        assertEquals(1, domain.getLinks().size());
        assertEquals(GUID_GPO1, domain.getLinks().get(0).get("GUID"));
        assertEquals(false, domain.getLinks().get(0).get("IsEnforced"));
    }

    @Test
    void resolve_ouGpLink_enforced() {
        var gpo = makeGPO("CN={DEF-456},CN=Policies,CN=System,DC=test,DC=local", GUID_GPO2);
        var ou = makeOUWithGpLink(
                "OU=Workstations,DC=test,DC=local", GUID_OU1,
                "CN={DEF-456},CN=Policies,CN=System,DC=test,DC=local", "2");

        var objects = new ArrayList<BloodHoundObject>(List.of(ou, gpo));
        var dnIndex = buildDnIndex(objects);

        GpoLinkResolver.resolve(objects, dnIndex);

        assertEquals(1, ou.getLinks().size());
        assertEquals(true, ou.getLinks().get(0).get("IsEnforced"));
    }

    @Test
    void resolve_missingGpo_skipped() {
        var domain = makeDomainWithGpLink(
                "DC=test,DC=local", "S-1-5-21-1-2-3",
                "CN={MISSING},CN=Policies,CN=System,DC=test,DC=local", "0");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain));
        var dnIndex = buildDnIndex(objects);

        GpoLinkResolver.resolve(objects, dnIndex);

        assertTrue(domain.getLinks().isEmpty());
    }

    @Test
    void resolve_multipleGpLinks_allResolved() {
        var gpo1 = makeGPO("CN={A},CN=Policies,CN=System,DC=test,DC=local", GUID_GPO_A);
        var gpo2 = makeGPO("CN={B},CN=Policies,CN=System,DC=test,DC=local", GUID_GPO_B);
        var domain = makeDomainWithMultiGpLink("DC=test,DC=local", "S-1-5-21-1-2-3",
                "[LDAP://CN={A},CN=Policies,CN=System,DC=test,DC=local;0]"
                + "[LDAP://CN={B},CN=Policies,CN=System,DC=test,DC=local;2]");

        var objects = new ArrayList<BloodHoundObject>(List.of(domain, gpo1, gpo2));
        var dnIndex = buildDnIndex(objects);

        GpoLinkResolver.resolve(objects, dnIndex);

        assertEquals(2, domain.getLinks().size());
    }

    private static BloodHoundGPO makeGPO(String dn, String guid) {
        return BloodHoundGPO.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid),
                "displayname", List.of("Test GPO")));
    }

    private static BloodHoundDomain makeDomainWithGpLink(
            String dn, String sid, String gpoDn, String options) {
        return BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "gplink", List.of("[LDAP://" + gpoDn + ";" + options + "]")));
    }

    private static BloodHoundDomain makeDomainWithMultiGpLink(
            String dn, String sid, String rawGpLink) {
        return BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "gplink", List.of(rawGpLink)));
    }

    private static BloodHoundOU makeOUWithGpLink(
            String dn, String guid, String gpoDn, String opts) {
        return BloodHoundOU.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid),
                "gplink", List.of("[LDAP://" + gpoDn + ";" + opts + "]")));
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
