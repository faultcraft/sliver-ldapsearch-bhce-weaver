package com.faultcraft.weaver.ad.classify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.classify.ObjectClassifier.ClassificationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectClassifierTest {

    private ObjectClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ObjectClassifier();
    }

    @Test
    void classify_userBySamAccountType() {
        var entry = entryWith("samaccounttype", "805306368");
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-1001"));
        entry.put("distinguishedname", List.of("CN=jdoe,CN=Users,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("users", result.objects().get(0).objectType());
    }

    @Test
    void classify_computerBySamAccountType() {
        var entry = entryWith("samaccounttype", "805306369");
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-1002"));
        entry.put("distinguishedname", List.of("CN=WS01,CN=Computers,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("computers", result.objects().get(0).objectType());
    }

    @Test
    void classify_groupBySamAccountType() {
        var entry = entryWith("samaccounttype", "268435456");
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-512"));
        entry.put("distinguishedname", List.of("CN=Domain Admins,CN=Users,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("groups", result.objects().get(0).objectType());
    }

    @Test
    void classify_groupAliasBySamAccountType() {
        var entry = entryWith("samaccounttype", "536870912");
        entry.put("objectsid", List.of("S-1-5-32-544"));
        entry.put("distinguishedname", List.of("CN=Administrators,CN=Builtin,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("groups", result.objects().get(0).objectType());
    }

    @Test
    void classify_domainByObjectClass() {
        var entry = entryWith("objectclass", "top, domainDNS");
        entry.put("objectsid", List.of("S-1-5-21-111-222-333"));
        entry.put("distinguishedname", List.of("DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("domains", result.objects().get(0).objectType());
    }

    @Test
    void classify_domainRequiresSid() {
        var entry = entryWith("objectclass", "top, domainDNS");
        entry.put("distinguishedname", List.of("DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(0, result.objects().size());
        assertEquals(1, result.unclassifiedCount());
    }

    @Test
    void classify_ouByObjectClass() {
        var entry = entryWith("objectclass", "top, organizationalUnit");
        entry.put("objectguid", List.of("{12345678-1234-1234-1234-123456789ABC}"));
        entry.put("distinguishedname", List.of("OU=Sales,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("ous", result.objects().get(0).objectType());
    }

    @Test
    void classify_gpoByObjectClass() {
        var entry = entryWith("objectclass", "container, groupPolicyContainer");
        entry.put("objectguid", List.of("{ABCDEF12-3456-7890-ABCD-EF1234567890}"));
        entry.put("distinguishedname",
                List.of("CN={31B2F340},CN=Policies,CN=System,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("gpos", result.objects().get(0).objectType());
    }

    @Test
    void classify_containerByObjectClass() {
        var entry = entryWith("objectclass", "top, container");
        entry.put("objectguid", List.of("{11111111-2222-3333-4444-555555555555}"));
        entry.put("distinguishedname", List.of("CN=Users,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("containers", result.objects().get(0).objectType());
    }

    @Test
    void classify_certTemplateByObjectClass() {
        var entry = entryWith("objectclass", "top, pKICertificateTemplate");
        entry.put("objectguid", List.of("{22222222-3333-4444-5555-666666666666}"));
        entry.put("distinguishedname",
                List.of("CN=User,CN=Certificate Templates,CN=Public Key Services,CN=Services,CN=Configuration,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("certtemplates", result.objects().get(0).objectType());
    }

    @Test
    void classify_enterpriseCaByObjectClass() {
        var entry = entryWith("objectclass", "top, pKIEnrollmentService");
        entry.put("objectguid", List.of("{33333333-4444-5555-6666-777777777777}"));
        entry.put("distinguishedname",
                List.of("CN=TEST-CA,CN=Enrollment Services,CN=Public Key Services,CN=Services,CN=Configuration,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("enterprisecas", result.objects().get(0).objectType());
    }

    @Test
    void classify_rootCaByDn() {
        var entry = entryWith("objectclass", "top, certificationAuthority");
        entry.put("objectguid", List.of("{44444444-5555-6666-7777-888888888888}"));
        entry.put("distinguishedname",
                List.of("CN=TEST-ROOT-CA,CN=Certification Authorities,CN=Public Key Services,CN=Services,CN=Configuration,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("rootcas", result.objects().get(0).objectType());
    }

    @Test
    void classify_aiaCaByDn() {
        var entry = entryWith("objectclass", "top, certificationAuthority");
        entry.put("objectguid", List.of("{55555555-6666-7777-8888-999999999999}"));
        entry.put("distinguishedname",
                List.of("CN=TEST-CA,CN=AIA,CN=Public Key Services,CN=Services,CN=Configuration,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("aiacas", result.objects().get(0).objectType());
    }

    @Test
    void classify_ntAuthStoreByDn() {
        var entry = entryWith("objectclass", "top, certificationAuthority");
        entry.put("objectguid", List.of("{66666666-7777-8888-9999-AAAAAAAAAAAA}"));
        entry.put("distinguishedname",
                List.of("CN=NTAuthCertificates,CN=Public Key Services,CN=Services,CN=Configuration,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("ntauthstores", result.objects().get(0).objectType());
    }

    @Test
    void classify_trustByObjectClass() {
        var entry = entryWith("objectclass", "top, trustedDomain");
        entry.put("trustpartner", List.of("other.local"));
        entry.put("distinguishedname",
                List.of("CN=other.local,CN=System,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(0, result.objects().size());
        assertEquals(1, result.trusts().size());
        assertEquals("OTHER.LOCAL", result.trusts().get(0).getTargetDomainName());
    }

    @Test
    void classify_issuancePolicyByObjectClass() {
        var entry = entryWith("objectclass", "top, msPKI-Enterprise-Oid");
        entry.put("flags", List.of("2"));
        entry.put("objectguid", List.of("{77777777-8888-9999-AAAA-BBBBBBBBBBBB}"));
        entry.put("distinguishedname",
                List.of("CN=1.2.3.4,CN=OID,CN=Public Key Services,CN=Services,CN=Configuration,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("issuancepolicies", result.objects().get(0).objectType());
    }

    @Test
    void classify_unknownEntry_incrementsCount() {
        var entry = entryWith("objectclass", "top, unknownType");
        entry.put("distinguishedname", List.of("CN=something,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(0, result.objects().size());
        assertEquals(0, result.trusts().size());
        assertEquals(1, result.unclassifiedCount());
    }

    @Test
    void classify_mixedBatch_routesCorrectly() {
        var user = entryWith("samaccounttype", "805306368");
        user.put("objectsid", List.of("S-1-5-21-111-222-333-1001"));
        user.put("distinguishedname", List.of("CN=jdoe,CN=Users,DC=test,DC=local"));

        var computer = entryWith("samaccounttype", "805306369");
        computer.put("objectsid", List.of("S-1-5-21-111-222-333-1002"));
        computer.put("distinguishedname", List.of("CN=WS01,CN=Computers,DC=test,DC=local"));

        var domain = entryWith("objectclass", "top, domainDNS");
        domain.put("objectsid", List.of("S-1-5-21-111-222-333"));
        domain.put("distinguishedname", List.of("DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(user, computer, domain));

        assertEquals(3, result.objects().size());
        assertEquals(0, result.trusts().size());
        assertEquals(0, result.unclassifiedCount());
    }

    @Test
    void classify_emptyInput_returnsEmpty() {
        ClassificationResult result = classifier.classify(List.of());

        assertTrue(result.objects().isEmpty());
        assertTrue(result.trusts().isEmpty());
        assertEquals(0, result.unclassifiedCount());
    }

    @Test
    void classify_samAccountTypeZero_fallsToObjectClass() {
        var entry = entryWith("samaccounttype", "0");
        entry.put("objectclass", List.of("top, organizationalUnit"));
        entry.put("objectguid", List.of("{AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE}"));
        entry.put("distinguishedname", List.of("OU=Test,DC=test,DC=local"));

        ClassificationResult result = classifier.classify(List.of(entry));

        assertEquals(1, result.objects().size());
        assertEquals("ous", result.objects().get(0).objectType());
    }

    private static Map<String, List<String>> entryWith(String key, String value) {
        var entry = new HashMap<String, List<String>>();
        entry.put(key, List.of(value));
        return entry;
    }
}
