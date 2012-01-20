package net.minecraft.src.forge.network;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;

import net.minecraft.src.EntityPlayer;

public class MessageManager 
{
    private static Hashtable<String, ArrayList<IPacketListener>> channelListeners = new Hashtable<String, ArrayList<IPacketListener>>();
    private static Hashtable<IPacketListener, HashSet<String>> listenerChannels = new Hashtable<IPacketListener, HashSet<String>>();
    
    private MessageManager(){}
    
    public static void registerListener(String channel, IPacketListener listener)
    {
        ArrayList<IPacketListener> list = channelListeners.get(channel);
        HashSet<String> list2 = listenerChannels.get(listener);
                
        if (list == null)
        {
            list = new ArrayList<IPacketListener>();
            channelListeners.put(channel, list);
        }
        if (!list.contains(listener))
        {
            list.add(listener);
        }
        
        if (list2 == null)
        {
            list2 = new HashSet<String>();
            listenerChannels.put(listener, list2);
        }
        if (!list2.contains(channel))
        {
            list2.add(channel);
        }
    }
    
    public static void unregisterListener(String channel, IPacketListener listener)
    {
        ArrayList<IPacketListener> list = channelListeners.get(channel);
        HashSet<String> list2 = listenerChannels.get(listener);
        
        if (list != null && list.contains(listener))
        {
            list.remove(listener);
        }
        if (list2 != null && list2.contains(channel))
        {
            list2.remove(channel);
        }
    }
    
    public static void unregisterListener(IPacketListener listener)
    {
        HashSet<String> list = listenerChannels.get(listener);
        for (String channel : list)
        {
            ArrayList<IPacketListener> lst = channelListeners.get(channel);
            if (lst != null && lst.contains(listener))
            {
                lst.remove(listener);
            }
        }
        listenerChannels.remove(listener);
    }
    
    public static void unregisterChannel(String channel)
    {
        ArrayList<IPacketListener> list = channelListeners.get(channel);
        if (list != null)
        {
            list = (ArrayList<IPacketListener>)list.clone();
            for (IPacketListener listener : list)
            {
                unregisterListener(channel, listener);
            }            
        }
    }
    
    public static void dispatchMessage(EntityPlayer player, String channel, byte[] data)
    {
        ArrayList<IPacketListener> list = channelListeners.get(channel);
        byte[] clone = null;
        if (data != null)
        {
            clone = new byte[data.length];
        }
        if (list != null)
        {
            list = (ArrayList<IPacketListener>)list.clone();
            for (IPacketListener listener : list)
            {
                if (data != null)
                {
                    for (int x = 0; x < data.length; x++)
                    {
                        clone[x] = data[x];
                    }
                }
                listener.ProcessPacket(player, channel, clone);
            }            
        }
    }
    
    
    
}
