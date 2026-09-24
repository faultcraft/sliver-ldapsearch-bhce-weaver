package com.faultcraft.weaver.ad.resolve;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundGPO;
import com.faultcraft.weaver.ad.model.BloodHoundOU;
import com.faultcraft.weaver.ad.model.BloodHoundObject;

/** Resolves GPO link relationships from gpLink attribute. */
final class GpoLinkResolver {

    private GpoLinkResolver() {}

    /** Resolves GPLink edges from parsed gpLink DN references. */
    static void resolve(List<BloodHoundObject> objects,
            Map<String, BloodHoundObject> dnIndex) {

        for (BloodHoundObject obj : objects) {
            List<String[]> gpLinks = null;
            if (obj instanceof BloodHoundDomain domain) {
                gpLinks = domain.getGpLinks();
            } else if (obj instanceof BloodHoundOU ou) {
                gpLinks = ou.getGpLinks();
            }

            if (gpLinks == null || gpLinks.isEmpty()) {
                continue;
            }

            for (String[] link : gpLinks) {
                String gpoDn = link[0];
                boolean enforced = "2".equals(link[1]);

                BloodHoundObject gpo = dnIndex.get(gpoDn.toUpperCase(Locale.ROOT));
                if (!(gpo instanceof BloodHoundGPO)) {
                    continue;
                }

                Map<String, Object> linkRef = Map.of(
                        "GUID", gpo.getObjectIdentifier(),
                        "IsEnforced", enforced);

                if (obj instanceof BloodHoundDomain domain) {
                    domain.getLinks().add(linkRef);
                } else if (obj instanceof BloodHoundOU ou) {
                    ou.getLinks().add(linkRef);
                }
            }
        }
    }
}
