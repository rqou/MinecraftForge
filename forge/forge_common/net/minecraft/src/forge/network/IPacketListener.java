package net.minecraft.src.forge.network;

import net.minecraft.src.EntityPlayer;

public interface IPacketListener 
{
	public void ProcessPacket(EntityPlayer player, String channel, byte[] data);
}
