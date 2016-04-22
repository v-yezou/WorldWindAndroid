/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.LruMemoryCache;

public class GpuObjectCache extends LruMemoryCache<Object, GpuObject> {

    protected List<GpuObject> disposalQueue = new ArrayList<>();

    public GpuObjectCache(int capacity) {
        super(capacity);
    }

    public GpuObjectCache(int capacity, int lowWater) {
        super(capacity, lowWater);
    }

    public void contextLost(DrawContext dc) {
        this.entries.clear(); // the cache entries are invalid; clear but don't call entryRemoved
        this.disposalQueue.clear(); // the disposal queue no longer needs to be processed
        this.usedCapacity = 0;
    }

    public void disposeEvictedObjects(DrawContext dc) {

        for (GpuObject object : this.disposalQueue) {
            try {
                object.dispose(dc);
                if (Logger.isLoggable(Logger.DEBUG)) {
                    Logger.log(Logger.DEBUG, "Disposed GPU object \'" + object + "\'");
                }
            } catch (Exception e) {
                Logger.log(Logger.ERROR, "Exception disposing GPU object \'" + object + "\'", e);
            }
        }

        this.disposalQueue.clear();
    }

    @Override
    protected void entryRemoved(Object key, GpuObject value) {
        // Explicitly free GPU objects associatd with the cache entry. We collect evicted GPU objects here and dispose
        // them at the end of a frame in disposeEvictedObjects. This avoids unexpected side effects like GPU programs
        // being evicted while in use, which can occur when this cache is too small and thrashes during a frame.
        this.disposalQueue.add(value);
    }
}
