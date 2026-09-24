package com.faultcraft.weaver.ad.resolve;

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

/** Resolves containment relationships from DN hierarchy. */
final class ContainmentResolver {

    private ContainmentResolver() {}

    /** Resolves Contains/ContainedBy edges from DN parent-child relationships. */
    static void resolve(List<BloodHoundObject> objects,
            Map<String, BloodHoundObject> dnIndex) {

        for (BloodHoundObject obj : objects) {
            if (obj instanceof BloodHoundDomain) {
                continue;
            }

            String dn = obj.getProperties().getString("distinguishedname");
            if (dn.isEmpty()) {
                continue;
            }

            String parentDn = getParentDn(dn);
            if (parentDn.isEmpty()) {
                continue;
            }

            BloodHoundObject parent = dnIndex.get(parentDn.toUpperCase(Locale.ROOT));
            if (parent == null) {
                continue;
            }

            Map<String, Object> childRef = Map.of(
                    "ObjectIdentifier", obj.getObjectIdentifier(),
                    "ObjectType", capitalize(obj.objectType()));

            addChild(parent, childRef);
            setContainedBy(obj, parent);
        }
    }

    private static void addChild(BloodHoundObject parent, Map<String, Object> childRef) {
        if (parent instanceof BloodHoundDomain domain) {
            domain.getChildObjects().add(childRef);
        } else if (parent instanceof BloodHoundOU ou) {
            ou.getChildObjects().add(childRef);
        } else if (parent instanceof BloodHoundContainer container) {
            container.getChildObjects().add(childRef);
        }
    }

    private static void setContainedBy(BloodHoundObject child, BloodHoundObject parent) {
        Map<String, Object> ref = Map.of(
                "ObjectIdentifier", parent.getObjectIdentifier(),
                "ObjectType", capitalize(parent.objectType()));

        if (child instanceof BloodHoundUser u) { u.setContainedBy(ref); }
        else if (child instanceof BloodHoundComputer c) { c.setContainedBy(ref); }
        else if (child instanceof BloodHoundGroup g) { g.setContainedBy(ref); }
        else if (child instanceof BloodHoundOU o) { o.setContainedBy(ref); }
        else if (child instanceof BloodHoundGPO g) { g.setContainedBy(ref); }
        else if (child instanceof BloodHoundContainer c) { c.setContainedBy(ref); }
        else if (child instanceof BloodHoundCertTemplate t) { t.setContainedBy(ref); }
        else if (child instanceof BloodHoundEnterpriseCA e) { e.setContainedBy(ref); }
        else if (child instanceof BloodHoundRootCA r) { r.setContainedBy(ref); }
        else if (child instanceof BloodHoundAIACA a) { a.setContainedBy(ref); }
        else if (child instanceof BloodHoundNTAuthStore n) { n.setContainedBy(ref); }
        else if (child instanceof BloodHoundIssuancePolicy i) { i.setContainedBy(ref); }
    }

    private static String getParentDn(String dn) {
        int comma = dn.indexOf(',');
        if (comma < 0 || comma >= dn.length() - 1) {
            return "";
        }
        return dn.substring(comma + 1);
    }

    private static String capitalize(String objectType) {
        if (objectType.isEmpty()) {
            return objectType;
        }
        return objectType.substring(0, 1).toUpperCase(Locale.ROOT)
                + objectType.substring(1);
    }
}
