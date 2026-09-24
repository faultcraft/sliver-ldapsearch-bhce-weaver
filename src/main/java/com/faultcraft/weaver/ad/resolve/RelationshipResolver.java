package com.faultcraft.weaver.ad.resolve;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.faultcraft.weaver.ad.acl.AceResolver;
import com.faultcraft.weaver.ad.acl.SecurityDescriptorParser;
import com.faultcraft.weaver.ad.classify.ObjectClassifier;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.DomainTrust;
import com.faultcraft.weaver.util.Log;

/**
 * Orchestrates all relationship resolution passes over classified objects.
 *
 * <p>Resolves ACLs, containment, group membership, GPO links, trusts,
 * and owner edges. Operates on the classified result from ObjectClassifier.
 */
public final class RelationshipResolver {

    private final AceResolver aceResolver = new AceResolver();

    /** Resolves all relationships for classified objects and trusts. */
    public void resolve(ObjectClassifier.ClassificationResult classified) {
        List<BloodHoundObject> objects = classified.objects();
        List<DomainTrust> trusts = classified.trusts();

        Map<String, BloodHoundObject> dnIndex = buildDnIndex(objects);
        Map<String, BloodHoundObject> sidIndex = buildSidIndex(objects);

        resolveAcls(objects);
        ContainmentResolver.resolve(objects, dnIndex);
        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);
        GpoLinkResolver.resolve(objects, dnIndex);
        TrustResolver.resolve(objects, trusts);
        AdcsResolver.resolve(objects, dnIndex);
        DelegationResolver.resolve(objects);

        Log.info("Resolved relationships for %d objects", objects.size());
    }

    private void resolveAcls(List<BloodHoundObject> objects) {
        int aclCount = 0;
        for (BloodHoundObject obj : objects) {
            String rawSd = obj.getRawNtSecurityDescriptor();
            if (rawSd.isEmpty()) {
                continue;
            }

            var parsed = SecurityDescriptorParser.parse(rawSd);
            obj.setAclProtected(parsed.isDaclProtected());

            boolean hasLaps = obj.objectType().equals("computers")
                    && obj.getProperties().getBoolean("haslaps");
            var resolved = aceResolver.resolve(parsed.aces(), obj.objectType(), hasLaps);
            obj.getAces().addAll(resolved);

            if (!parsed.ownerSid().isEmpty()) {
                var ownerAce = new java.util.LinkedHashMap<String, Object>();
                ownerAce.put("PrincipalSID", parsed.ownerSid());
                ownerAce.put("PrincipalType", "");
                ownerAce.put("RightName", "Owns");
                ownerAce.put("IsInherited", false);
                obj.getAces().add(ownerAce);
            }

            aclCount++;
        }
        Log.info("Resolved ACLs for %d objects", aclCount);
    }

    private static Map<String, BloodHoundObject> buildDnIndex(List<BloodHoundObject> objects) {
        return objects.stream()
                .filter(o -> !o.getProperties().getString("distinguishedname").isEmpty())
                .collect(Collectors.toMap(
                        o -> o.getProperties().getString("distinguishedname")
                                .toUpperCase(Locale.ROOT),
                        o -> o,
                        (a, b) -> a));
    }

    private static Map<String, BloodHoundObject> buildSidIndex(List<BloodHoundObject> objects) {
        return objects.stream()
                .filter(o -> !o.getObjectIdentifier().isEmpty())
                .collect(Collectors.toMap(
                        BloodHoundObject::getObjectIdentifier,
                        o -> o,
                        (a, b) -> a));
    }
}
