package net.minecraft.src;

import net.minecraft.src.BaseMod;

import java.util.logging.Logger;

public abstract class BaseModMp extends BaseMod
{
    public final int getId()
    {
        return this.toString().hashCode();
    }

    public void ModsLoaded()
    {
        
    }

    public void HandlePacket(Packet230ModLoader var1) {}

    public void HandleTileEntityPacket(int var1, int var2, int var3, int var4, int[] var5, float[] var6, String[] var7) {}

    public GuiScreen HandleGUI(int var1)
    {
        return null;
    }
}
