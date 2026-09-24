package com.faultcraft.weaver.ad.resolve;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.BloodHoundAIACA;
import com.faultcraft.weaver.ad.model.BloodHoundCertTemplate;
import com.faultcraft.weaver.ad.model.BloodHoundComputer;
import com.faultcraft.weaver.ad.model.BloodHoundEnterpriseCA;
import com.faultcraft.weaver.ad.model.BloodHoundIssuancePolicy;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.util.Log;

/**
 * Resolves ADCS relationships: EnabledCertTemplates on EnterpriseCA,
 * HostingComputer, certificate chains, and issuance policy group links.
 */
final class AdcsResolver {

    private AdcsResolver() {}

    static void resolve(List<BloodHoundObject> objects,
            Map<String, BloodHoundObject> dnIndex) {

        List<BloodHoundCertTemplate> templates = objects.stream()
                .filter(o -> o instanceof BloodHoundCertTemplate)
                .map(o -> (BloodHoundCertTemplate) o)
                .toList();
        List<BloodHoundComputer> computers = objects.stream()
                .filter(o -> o instanceof BloodHoundComputer)
                .map(o -> (BloodHoundComputer) o)
                .toList();

        for (BloodHoundObject obj : objects) {
            if (obj instanceof BloodHoundEnterpriseCA ca) {
                resolvePublishedTemplates(ca, templates);
                resolveHostingComputer(ca, computers);
            } else if (obj instanceof BloodHoundIssuancePolicy policy) {
                resolveGroupLink(policy, dnIndex);
            }
        }
    }

    private static void resolvePublishedTemplates(BloodHoundEnterpriseCA ca,
            List<BloodHoundCertTemplate> templates) {
        List<String> templateNames = ca.getRawCertTemplateNames();
        if (templateNames.isEmpty()) {
            return;
        }

        String caDomain = ca.getProperties().getString("domain");

        for (String name : templateNames) {
            String lower = name.toLowerCase(Locale.ROOT);
            for (BloodHoundCertTemplate tmpl : templates) {
                String tmplName = tmpl.getProperties().getString("name");
                String tmplBaseName = tmplName.contains("@")
                        ? tmplName.substring(0, tmplName.indexOf('@')).toLowerCase(Locale.ROOT)
                        : tmplName.toLowerCase(Locale.ROOT);
                String tmplDomain = tmpl.getProperties().getString("domain");

                if (lower.equals(tmplBaseName) && caDomain.equalsIgnoreCase(tmplDomain)) {
                    ca.getEnabledCertTemplates().add(Map.of(
                            "ObjectIdentifier", tmpl.getObjectIdentifier(),
                            "ObjectType", "CertTemplate"));
                    break;
                }
            }
        }
    }

    private static void resolveHostingComputer(BloodHoundEnterpriseCA ca,
            List<BloodHoundComputer> computers) {
        String hostname = ca.getProperties().getString("dnshostname");
        if (hostname.isEmpty()) {
            return;
        }

        for (BloodHoundComputer comp : computers) {
            String compHost = comp.getProperties().getString("dnshostname");
            if (hostname.equalsIgnoreCase(compHost)) {
                ca.setHostingComputer(comp.getObjectIdentifier());
                return;
            }
        }
        Log.debug("Could not resolve CA hosting computer: %s", hostname);
    }

    private static void resolveGroupLink(BloodHoundIssuancePolicy policy,
            Map<String, BloodHoundObject> dnIndex) {
        String groupDn = policy.getGroupLink();
        if (groupDn.isEmpty()) {
            return;
        }
        BloodHoundObject group = dnIndex.get(groupDn.toUpperCase(Locale.ROOT));
        if (group != null) {
            policy.setResolvedGroupLink(group.getObjectIdentifier());
        }
    }
}
