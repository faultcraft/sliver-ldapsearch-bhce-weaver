package com.faultcraft.weaver.ad.resolve;

import java.util.List;
import java.util.Locale;

import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.DomainTrust;

/** Resolves trust relationships between domains. */
final class TrustResolver {

    private TrustResolver() {}

    /** Adds trust entries to domain objects. */
    static void resolve(List<BloodHoundObject> objects, List<DomainTrust> trusts) {
        if (trusts.isEmpty()) {
            return;
        }

        for (BloodHoundObject obj : objects) {
            if (!(obj instanceof BloodHoundDomain domain)) {
                continue;
            }

            String domainName = domain.getProperties().getString("name")
                    .toUpperCase(Locale.ROOT);

            for (DomainTrust trust : trusts) {
                String localDn = trust.getLocalDomainDn().toUpperCase(Locale.ROOT);
                if (domainName.equals(localDn) || domainName.contains(localDn)) {
                    domain.getTrusts().add(trust.toJson());
                }
            }
        }
    }
}
