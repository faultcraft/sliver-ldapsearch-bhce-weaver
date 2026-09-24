package com.faultcraft.weaver.ad.resolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.model.BloodHoundCertTemplate;
import com.faultcraft.weaver.ad.model.BloodHoundComputer;
import com.faultcraft.weaver.ad.model.BloodHoundEnterpriseCA;
import com.faultcraft.weaver.ad.model.BloodHoundIssuancePolicy;
import com.faultcraft.weaver.ad.model.BloodHoundObject;

class AdcsResolverTest {

    private static final String GUID_TMPL = "{11111111-1111-1111-1111-111111111111}";
    private static final String GUID_CA = "{22222222-2222-2222-2222-222222222222}";
    private static final String GUID_POL = "{33333333-3333-3333-3333-333333333333}";

    @Test
    void resolve_publishedTemplate_linkedToCA() {
        var template = makeCertTemplate(
                "CN=UserAuth,CN=Certificate Templates,CN=Public Key Services,CN=Services,"
                + "CN=Configuration,DC=test,DC=local",
                GUID_TMPL, "UserAuth");
        var ca = makeEnterpriseCA(
                "CN=TestCA,CN=Enrollment Services,CN=Public Key Services,CN=Services,"
                + "CN=Configuration,DC=test,DC=local",
                GUID_CA, "UserAuth");

        var objects = new ArrayList<BloodHoundObject>(List.of(template, ca));
        var dnIndex = buildDnIndex(objects);

        AdcsResolver.resolve(objects, dnIndex);

        assertEquals(1, ca.getEnabledCertTemplates().size());
        assertEquals(GUID_TMPL,
                ca.getEnabledCertTemplates().get(0).get("ObjectIdentifier"));
    }

    @Test
    void resolve_hostingComputer_linked() {
        var ca = makeEnterpriseCAWithHost(
                "CN=TestCA,CN=Enrollment Services,CN=Public Key Services,CN=Services,"
                + "CN=Configuration,DC=test,DC=local",
                GUID_CA, "dc01.test.local");
        var comp = makeComputer(
                "CN=DC01,OU=Domain Controllers,DC=test,DC=local",
                "S-1-5-21-1-2-3-1000", "dc01.test.local");

        var objects = new ArrayList<BloodHoundObject>(List.of(ca, comp));
        var dnIndex = buildDnIndex(objects);

        AdcsResolver.resolve(objects, dnIndex);

        Map<String, Object> json = ca.toJson();
        assertEquals("S-1-5-21-1-2-3-1000", json.get("HostingComputer"));
    }

    @Test
    void resolve_issuancePolicyGroupLink_resolved() {
        var policy = makeIssuancePolicy(
                "CN=1.2.3,CN=OID,CN=Public Key Services,CN=Services,"
                + "CN=Configuration,DC=test,DC=local",
                GUID_POL,
                "CN=HighAssurance,CN=Users,DC=test,DC=local");
        var group = makeGroup(
                "CN=HighAssurance,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-2001");

        var objects = new ArrayList<BloodHoundObject>(List.of(policy, group));
        var dnIndex = buildDnIndex(objects);

        AdcsResolver.resolve(objects, dnIndex);

        Map<String, Object> json = policy.toJson();
        assertEquals("S-1-5-21-1-2-3-2001", json.get("GroupLink"));
    }

    @Test
    void resolve_noMatchingTemplate_emptyList() {
        var ca = makeEnterpriseCA(
                "CN=TestCA,CN=Enrollment Services,CN=Public Key Services,CN=Services,"
                + "CN=Configuration,DC=test,DC=local",
                GUID_CA, "NonExistent");

        var objects = new ArrayList<BloodHoundObject>(List.of(ca));
        var dnIndex = buildDnIndex(objects);

        AdcsResolver.resolve(objects, dnIndex);

        assertTrue(ca.getEnabledCertTemplates().isEmpty());
    }

    private static BloodHoundCertTemplate makeCertTemplate(String dn, String guid, String name) {
        return BloodHoundCertTemplate.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid),
                "name", List.of(name)));
    }

    private static BloodHoundEnterpriseCA makeEnterpriseCA(
            String dn, String guid, String templateName) {
        return BloodHoundEnterpriseCA.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid),
                "name", List.of("TestCA"),
                "certificatetemplates", List.of(templateName)));
    }

    private static BloodHoundEnterpriseCA makeEnterpriseCAWithHost(
            String dn, String guid, String dnshost) {
        return BloodHoundEnterpriseCA.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid),
                "name", List.of("TestCA"),
                "dnshostname", List.of(dnshost)));
    }

    private static BloodHoundComputer makeComputer(String dn, String sid, String dnshost) {
        return BloodHoundComputer.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "dnshostname", List.of(dnshost),
                "samaccountname", List.of("DC01$")));
    }

    private static BloodHoundIssuancePolicy makeIssuancePolicy(
            String dn, String guid, String groupLink) {
        return BloodHoundIssuancePolicy.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectguid", List.of(guid),
                "msds-oidtogrouplink", List.of(groupLink)));
    }

    private static com.faultcraft.weaver.ad.model.BloodHoundGroup makeGroup(
            String dn, String sid) {
        return com.faultcraft.weaver.ad.model.BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("HighAssurance")));
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
