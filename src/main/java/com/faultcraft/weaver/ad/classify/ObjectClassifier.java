package com.faultcraft.weaver.ad.classify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.BloodHoundAIACA;
import com.faultcraft.weaver.ad.model.BloodHoundCertTemplate;
import com.faultcraft.weaver.ad.model.BloodHoundComputer;
import com.faultcraft.weaver.ad.model.BloodHoundContainer;
import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundEnterpriseCA;
import com.faultcraft.weaver.ad.model.BloodHoundGPO;
import com.faultcraft.weaver.ad.model.BloodHoundGroup;
import com.faultcraft.weaver.ad.model.BloodHoundIssuancePolicy;
import com.faultcraft.weaver.ad.model.BloodHoundNTAuthStore;
import com.faultcraft.weaver.ad.model.BloodHoundOU;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundRootCA;
import com.faultcraft.weaver.ad.model.BloodHoundUser;
import com.faultcraft.weaver.ad.model.DomainTrust;
import com.faultcraft.weaver.util.Log;

/**
 * Routes merged LDAP entries to typed AD model objects.
 *
 * <p>Classification order: samAccountType first (User, Computer, Group),
 * then objectClass for structural types (Domain, OU, GPO, Container, ADCS, Trust).
 */
public final class ObjectClassifier {

    private static final long SAM_USER = 0x30000000L;
    private static final long SAM_MACHINE = 0x30000001L;
    private static final long SAM_GROUP = 0x10000000L;
    private static final long SAM_NON_SECURITY_GROUP = 0x10000001L;
    private static final long SAM_ALIAS = 0x20000000L;
    private static final long SAM_NON_SECURITY_ALIAS = 0x20000001L;

    /** Result of classifying a batch of merged entries. */
    public record ClassificationResult(
            List<BloodHoundObject> objects,
            List<DomainTrust> trusts,
            int unclassifiedCount) {}

    /** Classifies a list of merged LDAP entries into typed AD objects. */
    public ClassificationResult classify(List<Map<String, List<String>>> entries) {
        var objects = new ArrayList<BloodHoundObject>();
        var trusts = new ArrayList<DomainTrust>();
        int unclassified = 0;

        for (Map<String, List<String>> entry : entries) {
            BloodHoundObject obj = classifyBySamAccountType(entry);
            if (obj != null) {
                objects.add(obj);
                continue;
            }

            obj = classifyByObjectClass(entry);
            if (obj != null) {
                objects.add(obj);
                continue;
            }

            DomainTrust trust = classifyAsTrust(entry);
            if (trust != null) {
                trusts.add(trust);
                continue;
            }

            unclassified++;
            String dn = firstValue(entry, "distinguishedname");
            Log.debug("Unclassified entry: %s", dn);
        }

        Log.info("Classified %d objects (%d trusts, %d unclassified)",
                objects.size(), trusts.size(), unclassified);
        return new ClassificationResult(
                List.copyOf(objects), List.copyOf(trusts), unclassified);
    }

    private BloodHoundObject classifyBySamAccountType(Map<String, List<String>> entry) {
        String satStr = firstValue(entry, "samaccounttype");
        if (satStr.isEmpty() || "0".equals(satStr.strip())) {
            return null;
        }

        long sat;
        try {
            sat = Long.parseLong(satStr.strip());
        } catch (NumberFormatException e) {
            return null;
        }

        if (sat == SAM_MACHINE) {
            return BloodHoundComputer.fromEntry(entry);
        }
        if (sat == SAM_USER) {
            return classifyUserOrComputer(entry);
        }
        if (sat == SAM_GROUP || sat == SAM_NON_SECURITY_GROUP
                || sat == SAM_ALIAS || sat == SAM_NON_SECURITY_ALIAS) {
            return BloodHoundGroup.fromEntry(entry);
        }

        return null;
    }

    private BloodHoundObject classifyUserOrComputer(Map<String, List<String>> entry) {
        List<String> objectClasses = allValuesLower(entry, "objectclass");
        if (objectClasses.contains("computer")) {
            return BloodHoundComputer.fromEntry(entry);
        }
        String gmsa = firstValue(entry, "msds-groupmsamembership");
        if (!gmsa.isEmpty()) {
            return BloodHoundUser.fromEntry(entry);
        }
        return BloodHoundUser.fromEntry(entry);
    }

    private BloodHoundObject classifyByObjectClass(Map<String, List<String>> entry) {
        List<String> objectClasses = allValuesLower(entry, "objectclass");
        if (objectClasses.isEmpty()) {
            return null;
        }

        if (objectClasses.contains("domaindns") || objectClasses.contains("domain")) {
            if (!firstValue(entry, "objectsid").isEmpty()) {
                return BloodHoundDomain.fromEntry(entry);
            }
        }
        if (objectClasses.contains("organizationalunit")) {
            return BloodHoundOU.fromEntry(entry);
        }
        if (objectClasses.contains("grouppolicycontainer")) {
            return BloodHoundGPO.fromEntry(entry);
        }
        if (objectClasses.contains("pkicertificatetemplate")) {
            return BloodHoundCertTemplate.fromEntry(entry);
        }
        if (objectClasses.contains("pkienrollmentservice")) {
            return BloodHoundEnterpriseCA.fromEntry(entry);
        }
        if (objectClasses.contains("certificationauthority")) {
            return classifyCertAuthority(entry);
        }
        if (objectClasses.contains("mspki-enterprise-oid")) {
            String flags = firstValue(entry, "flags");
            if ("2".equals(flags.strip())) {
                return BloodHoundIssuancePolicy.fromEntry(entry);
            }
        }
        if (objectClasses.contains("container") || objectClasses.contains("builtindomain")) {
            return BloodHoundContainer.fromEntry(entry);
        }

        return null;
    }

    private BloodHoundObject classifyCertAuthority(Map<String, List<String>> entry) {
        String dn = firstValue(entry, "distinguishedname").toUpperCase(Locale.ROOT);

        if (dn.contains("CN=NTAUTHCERTIFICATES")) {
            return BloodHoundNTAuthStore.fromEntry(entry);
        }
        if (dn.contains("CN=AIA,")) {
            return BloodHoundAIACA.fromEntry(entry);
        }
        return BloodHoundRootCA.fromEntry(entry);
    }

    private DomainTrust classifyAsTrust(Map<String, List<String>> entry) {
        List<String> objectClasses = allValuesLower(entry, "objectclass");
        if (objectClasses.contains("trusteddomain")) {
            return DomainTrust.fromEntry(entry);
        }
        return null;
    }

    private static String firstValue(Map<String, List<String>> entry, String key) {
        List<String> vals = entry.get(key);
        if (vals == null || vals.isEmpty()) {
            return "";
        }
        return vals.get(0);
    }

    private static List<String> allValuesLower(Map<String, List<String>> entry, String key) {
        List<String> vals = entry.get(key);
        if (vals == null || vals.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<String>();
        for (String val : vals) {
            for (String part : val.split(",")) {
                String trimmed = part.strip().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }
}
