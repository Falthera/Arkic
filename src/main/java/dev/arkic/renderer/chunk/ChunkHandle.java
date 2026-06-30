package dev.arkic.renderer.chunk;

public class ChunkHandle {
    public final int sectionX, sectionY, sectionZ;
    public final long ssboOffset;
    public final int vertexCount;
    public final int drawOffset;
    public boolean visible = true;
    public boolean dirty = true;
    public long lastUpdateTick;
    public int renderDistance;
    
    public ChunkHandle(int x, int y, int z) {
        this.sectionX = x;
        this.sectionY = y;
        this.sectionZ = z;
        this.ssboOffset = -1;
        this.vertexCount = 0;
        this.drawOffset = -1;
    }
    
    public void markDirty() {
        dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setUploaded(long offset, int vertexCount, int drawOffset) {
        this.ssboOffset = offset;
        this.vertexCount = vertexCount;
        this.drawOffset = drawOffset;
        this.dirty = false;
        this.visible = true;
    }
    
    public String key() {
        return sectionX + "," + sectionY + "," + sectionZ;
    }
}
