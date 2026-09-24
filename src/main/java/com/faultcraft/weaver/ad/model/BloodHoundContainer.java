package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BloodHoundContainer extends BloodHoundObject {

    private final List<Map<String, Object>> childObjects = new ArrayList<>();
    private Map<String, Object> containedBy = Map.of();

    @Override
    public String objectType() {
        return "containers";
    }

    public static BloodHoundContainer fromEntry(Map<String, List<String>> entry) {
        var container = new BloodHoundContainer();
        container.populateCommon(entry);
        container.getProperties().put("highvalue", false);
        return container;
    }

    public List<Map<String, Object>> getChildObjects() { return childObjects; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        json.put("ChildObjects", List.copyOf(childObjects));
        return json;
    }
}
