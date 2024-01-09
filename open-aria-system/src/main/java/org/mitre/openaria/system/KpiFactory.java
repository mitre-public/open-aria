
package org.mitre.openaria.system;

/**
 * A KpiFactory builds independent copies of a StreamingKpi.
 *
 * <p>A factory object makes it easier to ensure that all copies of a StreamingKpi will reuse
 * certain assets. For example, when we create 200 copies of the Airborne ARIA Algorithm (one for
 * each NOP Facility) or 30 copies of the Surface ARIA Algorithm (one for each airport) we want all
 * those KPIs to use the same configuration, the same smoothing cache, and the same output
 * pipeline.
 */
@FunctionalInterface
public interface KpiFactory<K> {

    public StreamingKpi createKpi(K key);
}
