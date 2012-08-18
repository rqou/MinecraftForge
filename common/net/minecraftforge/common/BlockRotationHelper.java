package net.minecraftforge.common;

import net.minecraft.src.*;

public class BlockRotationHelper
{
    /**
     * Actual logic for rotating a vanilla block on X-axis
     * @param world The current world
     * @param x X Position
     * @param y Y Position
     * @param z Z Position
     * @param amount Number of times to rotate
     * @return Whether the rotation succeeded
     */
    public static boolean vanillaRotateX(World world, int x, int y, int z, int amount)
    {
        boolean rotateNeg = amount < 0;
        amount = Math.abs(amount) % 4;
        if(rotateNeg)
            amount = 4 - amount;
        
        int meta = world.getBlockMetadata(x, y, z);
        int blkId = world.getBlockId(x, y, z);
        Block blk = Block.blocksList[blkId];
        
        //piston special case
        if(blk instanceof BlockPistonBase)
        {
            if(((BlockPistonBase)blk).isExtended(meta))
                return false;
        }
        
        //blocks that cannot face up/down
        if(blk instanceof BlockDispenser || blk instanceof BlockFurnace || blk instanceof BlockLadder || blk instanceof BlockButton || blk instanceof BlockFenceGate || blk instanceof BlockPumpkin || blk instanceof BlockTripWireSource || blk instanceof BlockHalfSlab)
        {
            if(amount % 2 != 0)
                return false;
        }
        
        //blocks that cannot rotate in the X-axis whatsoever
        if(blk instanceof BlockRedstoneRepeater || blk instanceof BlockTrapDoor || blk instanceof BlockMushroomCap)
        {
            return amount % 4 == 0;
        }
        
        //"simple" blocks with 0-5 in meta
        if(blk instanceof BlockPistonBase || blk instanceof BlockDispenser || blk instanceof BlockFurnace || blk instanceof BlockLadder)
        {
            meta = rotateBasicX(meta, amount);
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //"reversed" blocks with 1-6 indicating e, w, s, n, up, down
        if(blk instanceof BlockButton || blk instanceof BlockTorch || blk instanceof BlockRedstoneTorch)
        {
            meta = rotateReversedX(meta, amount);
            
            if(blk instanceof BlockTorch || blk instanceof BlockRedstoneTorch)
                if(meta == 6)   //down
                    return false;
            
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }

        //blocks with 0-3 indicating south, west, north, east (ORDER MATTERS)
        if(blk instanceof BlockFenceGate || blk instanceof BlockPumpkin || blk instanceof BlockTripWireSource)
        {
            if(meta == 1 || meta == 3)
            {
                meta = (meta + 2) % 4;
                world.setBlockMetadataWithNotify(x, y, z, meta);
            }
            return true;
        }
        
        //half slabs
        if(blk instanceof BlockHalfSlab)
        {
            if(amount % 4 != 0)
            {
                meta ^= 8;
                world.setBlockMetadataWithNotify(x, y, z, meta);
            }
            return true;
        }
        
        //logs
        if(blk instanceof BlockLog)
        {
            if(amount % 2 != 0)
            {
                int dir = (meta & 12) >> 2;
                if(dir == 0 || dir == 1)
                {
                    if(dir == 0)
                        dir = 1;
                    else if(dir == 1)
                        dir = 0;
                    meta = (meta & 3) | (dir << 2);
                    world.setBlockMetadataWithNotify(x, y, z, meta);
                }
            }
            return true;
        }
        
        //stairs
        if(blk instanceof BlockStairs)
        {
            int upsideDown = (meta & 4) >> 2;
            int direction = meta & 3;
            if(direction == 2 || direction == 3)
            {
                if(amount % 2 != 0)
                    return false;

                if(upsideDown == 0)
                    upsideDown = 1;
                else if(upsideDown == 1)
                    upsideDown = 0;
                world.setBlockMetadataWithNotify(x, y, z, direction | (upsideDown << 2));
                return true;
            }
            else
            {
                for(int i = 0; i < amount; i++)
                {
                    if(direction == 1 && upsideDown == 0)
                    {
                        direction = 0;
                    }
                    else if(direction == 0 && upsideDown == 0)
                    {
                        upsideDown = 1;
                    }
                    else if(direction == 0 && upsideDown == 1)
                    {
                        direction = 1;
                    }
                    else if(direction == 1 && upsideDown == 1)
                    {
                        upsideDown = 0;
                    }
                }
                world.setBlockMetadataWithNotify(x, y, z, direction | (upsideDown << 2));
                return true;
            }
        }
        
        //levers
        if(blk instanceof BlockLever)
        {
            int highbit = meta & 8;
            int dir = meta & 7;
            if(dir == 6)
                dir = 5;
            else if(dir == 0 || dir == 7)
                dir = 6;
            dir = rotateReversedX(dir, amount);
            if(dir == 5)
                dir = 6;
            else if(dir == 6)
                dir = 0;
            meta = dir | highbit;
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        return true;
    }
    
    /**
     * Actual logic for rotating a vanilla block on Y-axis
     * @param world The current world
     * @param x X Position
     * @param y Y Position
     * @param z Z Position
     * @param amount Number of times to rotate
     * @return Whether the rotation succeeded
     */
    public static boolean vanillaRotateY(World world, int x, int y, int z, int amount)
    {
        boolean rotateNeg = amount < 0;
        amount = Math.abs(amount) % 4;
        if(rotateNeg)
            amount = 4 - amount;
        
        int meta = world.getBlockMetadata(x, y, z);
        int blkId = world.getBlockId(x, y, z);
        Block blk = Block.blocksList[blkId];
        
        //piston special case
        if(blk instanceof BlockPistonBase)
        {
            if(((BlockPistonBase)blk).isExtended(meta))
                return false;
        }
        
        //"simple" blocks with 0-5 in meta
        if(blk instanceof BlockPistonBase || blk instanceof BlockDispenser || blk instanceof BlockFurnace || blk instanceof BlockLadder)
        {
            meta = rotateBasicY(meta, amount);
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //"reversed" blocks with 1-6 indicating e, w, s, n, up, down
        if(blk instanceof BlockButton || blk instanceof BlockTorch || blk instanceof BlockRedstoneTorch)
        {
            meta = rotateReversedY(meta, amount);
            
            if(blk instanceof BlockTorch || blk instanceof BlockRedstoneTorch)
                if(meta == 6)   //down
                    return false;
            
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //blocks with 0-3 indicating north, east, south, west
        if(blk instanceof BlockRedstoneRepeater || blk instanceof BlockFenceGate || blk instanceof BlockPumpkin || blk instanceof BlockTripWireSource)
        {
            meta = rotateCompassRoseY(meta, amount);
            
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //logs
        if(blk instanceof BlockLog)
        {
            if(amount % 2 != 0)
            {
                int dir = (meta & 12) >> 2;
                if(dir == 1 || dir == 2)
                {
                    if(dir == 1)
                        dir = 2;
                    else if(dir == 2)
                        dir = 1;
                    meta = (meta & 3) | (dir << 2);
                    world.setBlockMetadataWithNotify(x, y, z, meta);
                }
            }
            return true;
        }
        
        //trapdoor
        if(blk instanceof BlockTrapDoor)
        {
            //hack, but it works

            int highbit = meta & 12;
            ForgeDirection dir = ForgeDirection.getOrientation(5 - (meta & 3));
            if(dir == ForgeDirection.UNKNOWN)
                return false;
            for(int i = 0; i < 4 - amount; i++)
                dir = dir.getRotatedCounterClockWiseY();
            meta = highbit | (5 - dir.ordinal());
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //huge mushrooms
        if(blk instanceof BlockMushroomCap)
        {
            if(meta == 0 || meta == 10 || meta == 5)
                return true;
            for(int i = 0; i < amount; i++)
            {
                if((meta & 1) == 0)
                {
                    if(meta == 2)
                        meta = 4;
                    else if(meta == 4)
                        meta = 8;
                    else if(meta == 8)
                        meta = 6;
                    else if(meta == 6)
                        meta = 2;
                }
                else
                {
                    meta += 2;
                    if(meta == 5)
                        meta += 2;
                    if(meta == 11)
                        meta = 1;
                }
            }
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //stairs
        if(blk instanceof BlockStairs)
        {
            int highbit = meta & 12;
            ForgeDirection dir = ForgeDirection.getOrientation(5 - (meta & 3));
            if(dir == ForgeDirection.UNKNOWN)
                return false;
            for(int i = 0; i < 4 - amount; i++)
                dir = dir.getRotatedCounterClockWiseY();
            meta = highbit | (5 - dir.ordinal());
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //levers
        if(blk instanceof BlockLever)
        {
            int highbit = meta & 8;
            int dir = meta & 7;
            if(dir < 1 || dir > 4)
                return true;
            dir = rotateReversedY(dir, amount);
            meta = dir | highbit;
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        return true;
    }
    
    /**
     * Actual logic for rotating a vanilla block on Z-axis
     * @param world The current world
     * @param x X Position
     * @param y Y Position
     * @param z Z Position
     * @param amount Number of times to rotate
     * @return Whether the rotation succeeded
     */
    public static boolean vanillaRotateZ(World world, int x, int y, int z, int amount)
    {
        boolean rotateNeg = amount < 0;
        amount = Math.abs(amount) % 4;
        if(rotateNeg)
            amount = 4 - amount;
        
        int meta = world.getBlockMetadata(x, y, z);
        int blkId = world.getBlockId(x, y, z);
        Block blk = Block.blocksList[blkId];
        
        //piston special case
        if(blk instanceof BlockPistonBase)
        {
            if(((BlockPistonBase)blk).isExtended(meta))
                return false;
        }
        
        //blocks that cannot face up/down
        if(blk instanceof BlockDispenser || blk instanceof BlockFurnace || blk instanceof BlockLadder || blk instanceof BlockButton || blk instanceof BlockFenceGate || blk instanceof BlockPumpkin || blk instanceof BlockTripWireSource || blk instanceof BlockHalfSlab)
        {
            if(amount % 2 != 0)
                return false;
        }
        
        //blocks that cannot rotate in the Z-axis whatsoever
        if(blk instanceof BlockRedstoneRepeater || blk instanceof BlockTrapDoor || blk instanceof BlockMushroomCap)
        {
            return amount % 4 == 0;
        }
        
        //"simple" blocks with 0-5 in meta
        if(blk instanceof BlockPistonBase || blk instanceof BlockDispenser || blk instanceof BlockFurnace || blk instanceof BlockLadder)
        {
            meta = rotateBasicZ(meta, amount);
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        //"reversed" blocks with 1-6 indicating e, w, s, n, up, down
        if(blk instanceof BlockButton || blk instanceof BlockTorch || blk instanceof BlockRedstoneTorch)
        {
            meta = rotateReversedZ(meta, amount);
            
            if(blk instanceof BlockTorch || blk instanceof BlockRedstoneTorch)
                if(meta == 6)   //down
                    return false;
            
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }

        //blocks with 0-3 indicating south, west, north, east (ORDER MATTERS)
        if(blk instanceof BlockFenceGate || blk instanceof BlockPumpkin || blk instanceof BlockTripWireSource)
        {
            if(meta == 1 || meta == 3)
            {
                meta = (meta + 2) % 4;
                world.setBlockMetadataWithNotify(x, y, z, meta);
            }
            return true;
        }
        
        //half slabs
        if(blk instanceof BlockHalfSlab)
        {
            if(amount % 4 != 0)
            {
                meta ^= 8;
                world.setBlockMetadataWithNotify(x, y, z, meta);
            }
            return true;
        }
        
        //logs
        if(blk instanceof BlockLog)
        {
            if(amount % 2 != 0)
            {
                int dir = (meta & 12) >> 2;
                if(dir == 0 || dir == 2)
                {
                    if(dir == 0)
                        dir = 2;
                    else if(dir == 2)
                        dir = 0;
                    meta = (meta & 3) | (dir << 2);
                    world.setBlockMetadataWithNotify(x, y, z, meta);
                }
            }
            return true;
        }
        
        //stairs
        if(blk instanceof BlockStairs)
        {
            int upsideDown = (meta & 4) >> 2;
            int direction = meta & 3;
            if(direction == 0 || direction == 1)
            {
                if(amount % 2 != 0)
                    return false;

                if(upsideDown == 0)
                    upsideDown = 1;
                else if(upsideDown == 1)
                    upsideDown = 0;
                world.setBlockMetadataWithNotify(x, y, z, direction | (upsideDown << 2));
                return true;
            }
            else
            {
                for(int i = 0; i < amount; i++)
                {
                    if(direction == 2 && upsideDown == 0)
                    {
                        upsideDown = 1;
                    }
                    else if(direction == 2 && upsideDown == 1)
                    {
                        direction = 3;
                    }
                    else if(direction == 3 && upsideDown == 1)
                    {
                        upsideDown = 0;
                    }
                    else if(direction == 3 && upsideDown == 0)
                    {
                        direction = 2;
                    }
                }
                world.setBlockMetadataWithNotify(x, y, z, direction | (upsideDown << 2));
                return true;
            }
        }
        
        //levers
        if(blk instanceof BlockLever)
        {
            int highbit = meta & 8;
            int dir = meta & 7;
            if(dir == 6)
                dir = 5;
            else if(dir == 0 || dir == 7)
                dir = 6;
            dir = rotateReversedZ(dir, amount);
            if(dir == 6)
                dir = 7;
            meta = dir | highbit;
            world.setBlockMetadataWithNotify(x, y, z, meta);
            return true;
        }
        
        return true;
    }
    
    /**
     * Rotate a block that contains an orientation in the lower three bits of metadata. Examples include pistons. Intended as a helper function
     * @param meta Old metadata
     * @param amount Amount to rotate. Must be > 0
     * @return New metadata
     */
    public static int rotateBasicX(int meta, int amount)
    {
        int highbit = meta & 8;
        ForgeDirection dir = ForgeDirection.getOrientation(meta & 7);
        if(dir == ForgeDirection.UNKNOWN)
            return meta;
        for(int i = 0; i < amount; i++)
            dir = dir.getRotatedCounterClockWiseX();
        return dir.ordinal() | highbit;
    }
    
    /**
     * Rotate a block that contains an orientation in the lower three bits of metadata. Examples include pistons. Intended as a helper function
     * @param meta Old metadata
     * @param amount Amount to rotate. Must be > 0
     * @return New metadata
     */
    public static int rotateBasicY(int meta, int amount)
    {
        int highbit = meta & 8;
        ForgeDirection dir = ForgeDirection.getOrientation(meta & 7);
        if(dir == ForgeDirection.UNKNOWN)
            return meta;
        for(int i = 0; i < amount; i++)
            dir = dir.getRotatedCounterClockWiseY();
        return dir.ordinal() | highbit;
    }
    
    /**
     * Rotate a block that contains an orientation in the lower three bits of metadata. Examples include pistons. Intended as a helper function
     * @param meta Old metadata
     * @param amount Amount to rotate. Must be > 0
     * @return New metadata
     */
    public static int rotateBasicZ(int meta, int amount)
    {
        int highbit = meta & 8;
        ForgeDirection dir = ForgeDirection.getOrientation(meta & 7);
        if(dir == ForgeDirection.UNKNOWN)
            return meta;
        for(int i = 0; i < amount; i++)
            dir = dir.getRotatedCounterClockWiseZ();
        return dir.ordinal() | highbit;
    }
    
    /**
     * Rotate a block that contains an orientation in the lower three bits of metadata. The orientations are east, west, south, north, up, down starting with 1. Examples include buttons, levers, and torches. Intended as a helper function
     * @param meta Old metadata
     * @param amount Amount to rotate. Must be > 0
     * @return New metadata
     */
    public static int rotateReversedX(int meta, int amount)
    {
        int highbit = meta & 8;
        ForgeDirection dir = ForgeDirection.getOrientation(6 - (meta & 7));
        if(dir == ForgeDirection.UNKNOWN)
            return meta;
        for(int i = 0; i < amount; i++)
            dir = dir.getRotatedCounterClockWiseX();
        return (6 - dir.ordinal()) | highbit;
    }
    
    /**
     * Rotate a block that contains an orientation in the lower three bits of metadata. The orientations are east, west, south, north, up, down starting with 1. Examples include buttons, levers, and torches. Intended as a helper function
     * @param meta Old metadata
     * @param amount Amount to rotate. Must be > 0
     * @return New metadata
     */
    public static int rotateReversedY(int meta, int amount)
    {
        int highbit = meta & 8;
        ForgeDirection dir = ForgeDirection.getOrientation(6 - (meta & 7));
        if(dir == ForgeDirection.UNKNOWN)
            return meta;
        for(int i = 0; i < amount; i++)
            dir = dir.getRotatedCounterClockWiseY();
        return (6 - dir.ordinal()) | highbit;
    }
    
    /**
     * Rotate a block that contains an orientation in the lower three bits of metadata. The orientations are east, west, south, north, up, down starting with 1. Examples include buttons, levers, and torches. Intended as a helper function
     * @param meta Old metadata
     * @param amount Amount to rotate. Must be > 0
     * @return New metadata
     */
    public static int rotateReversedZ(int meta, int amount)
    {
        int highbit = meta & 8;
        ForgeDirection dir = ForgeDirection.getOrientation(6 - (meta & 7));
        if(dir == ForgeDirection.UNKNOWN)
            return meta;
        for(int i = 0; i < amount; i++)
            dir = dir.getRotatedCounterClockWiseZ();
        return (6 - dir.ordinal()) | highbit;
    }
    
    /**
     * Rotate a block that uses the lower two bits to indicate direction in the order north, east, south, west. Does not matter if it does not actually start with 0 = north, because rotation is relative. Examples include redstone repeaters. Intended as a helper function 
     * @param meta
     * @param amount
     * @return
     */
    public static int rotateCompassRoseY(int meta, int amount)
    {
        int highbit = meta & 12;
        int dir = meta & 3;
        dir = (dir + amount) % 4;
        return dir | highbit;
    }
}
