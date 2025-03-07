package xyz.xenondevs.inventoryaccess.version;

import xyz.xenondevs.inventoryaccess.util.VersionUtils;

public enum InventoryAccessRevision {
    
    // this order is required
    R20("r20", "1.21.0"),
    R19("r19", "1.20.5"),
    R18("r18", "1.20.3"),
    R17("r17", "1.20.2"),
    R16("r16", "1.20.0");
    
    public static final InventoryAccessRevision REQUIRED_REVISION = getRequiredRevision();
    
    private final String packageName;
    private final int[] since;
    
    InventoryAccessRevision(String packageName, String since) {
        this.packageName = packageName;
        this.since = VersionUtils.toMajorMinorPatch(since);
    }
    
    private static InventoryAccessRevision getRequiredRevision() {
        for (InventoryAccessRevision revision : values())
            if (VersionUtils.isServerHigherOrEqual(revision.getSince())) return revision;
        
        throw new UnsupportedOperationException("Your version of Minecraft is not supported by InventoryAccess");
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public int[] getSince() {
        return since;
    }
    
}
