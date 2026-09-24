package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemainingModelsTest {

    @Test
    void gpo_fromEntry_setsGpcPath() {
        var entry = baseEntry();
        entry.put("gpcfilesyspath",
                List.of("\\\\test.local\\sysvol\\test.local\\Policies\\{ABC}"));

        BloodHoundGPO gpo = BloodHoundGPO.fromEntry(entry);

        assertEquals("gpos", gpo.objectType());
        assertEquals("\\\\test.local\\sysvol\\test.local\\Policies\\{ABC}",
                gpo.getProperties().get("gpcpath"));
    }

    @Test
    void container_fromEntry_setsObjectType() {
        BloodHoundContainer c = BloodHoundContainer.fromEntry(baseEntry());
        assertEquals("containers", c.objectType());
        assertTrue(c.toJson().containsKey("ChildObjects"));
    }

    @Test
    void container_childObjects_mutable() {
        BloodHoundContainer c = BloodHoundContainer.fromEntry(baseEntry());
        c.getChildObjects().add(Map.of("ObjectIdentifier", "test"));
        assertEquals(1, c.getChildObjects().size());
    }

    @Test
    void certTemplate_fromEntry_parsesFlags() {
        var entry = baseEntry();
        entry.put("mspki-certificate-name-flag", List.of("1"));
        entry.put("mspki-enrollment-flag", List.of("0"));
        entry.put("mspki-template-schema-version", List.of("2"));

        BloodHoundCertTemplate tmpl = BloodHoundCertTemplate.fromEntry(entry);

        assertEquals("certtemplates", tmpl.objectType());
        assertTrue(tmpl.getProperties().getBoolean("enrolleesuppliessubject"));
    }

    @Test
    void certTemplate_authenticationOid_detected() {
        var entry = baseEntry();
        entry.put("pkiextendedkeyusage", List.of("1.3.6.1.5.5.7.3.2"));
        entry.put("mspki-template-schema-version", List.of("1"));

        BloodHoundCertTemplate tmpl = BloodHoundCertTemplate.fromEntry(entry);

        assertTrue(tmpl.getProperties().getBoolean("authenticationenabled"));
    }

    @Test
    void certTemplate_noEkus_authenticationDefault() {
        var entry = baseEntry();
        entry.put("mspki-template-schema-version", List.of("1"));

        BloodHoundCertTemplate tmpl = BloodHoundCertTemplate.fromEntry(entry);

        assertTrue(tmpl.getProperties().getBoolean("authenticationenabled"));
    }

    @Test
    void enterpriseCa_fromEntry_setsProperties() {
        var entry = baseEntry();
        entry.put("dnshostname", List.of("ca.test.local"));
        entry.put("name", List.of("TEST-CA"));
        entry.put("certificatetemplates", List.of("User, Machine"));

        BloodHoundEnterpriseCA ca = BloodHoundEnterpriseCA.fromEntry(entry);

        assertEquals("enterprisecas", ca.objectType());
        assertEquals("ca.test.local", ca.getProperties().get("dnshostname"));
        assertEquals("TEST-CA", ca.getProperties().get("caname"));
    }

    @Test
    void rootCa_fromEntry_setsObjectType() {
        BloodHoundRootCA rootCa = BloodHoundRootCA.fromEntry(baseEntry());
        assertEquals("rootcas", rootCa.objectType());
    }

    @Test
    void aiaCa_fromEntry_setsObjectType() {
        BloodHoundAIACA aiaCa = BloodHoundAIACA.fromEntry(baseEntry());
        assertEquals("aiacas", aiaCa.objectType());
    }

    @Test
    void ntAuthStore_fromEntry_setsObjectType() {
        BloodHoundNTAuthStore store = BloodHoundNTAuthStore.fromEntry(baseEntry());
        assertEquals("ntauthstores", store.objectType());
    }

    @Test
    void issuancePolicy_fromEntry_setsOid() {
        var entry = baseEntry();
        entry.put("mspki-cert-template-oid", List.of("1.3.6.1.4.1.311.21.8.1234"));

        BloodHoundIssuancePolicy policy = BloodHoundIssuancePolicy.fromEntry(entry);

        assertEquals("issuancepolicies", policy.objectType());
        assertEquals("1.3.6.1.4.1.311.21.8.1234",
                policy.getProperties().get("certtemplateoid"));
    }

    @Test
    void issuancePolicy_groupLink_set() {
        var entry = baseEntry();
        entry.put("msds-oidtogrouplink",
                List.of("CN=Group1,CN=Users,DC=test,DC=local"));

        BloodHoundIssuancePolicy policy = BloodHoundIssuancePolicy.fromEntry(entry);

        assertEquals("CN=GROUP1,CN=USERS,DC=TEST,DC=LOCAL",
                policy.getGroupLink());
    }

    private static Map<String, List<String>> baseEntry() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname",
                List.of("CN=Test,DC=test,DC=local"));
        entry.put("objectguid", List.of("{12345678-1234-1234-1234-123456789ABC}"));
        return entry;
    }
}
