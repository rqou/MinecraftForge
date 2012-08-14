/**
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package net.minecraftforge.common;

import java.util.LinkedList;

import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ISpecialInventory;

public class StackUtil {

    public ItemStack items;
    public int itemsAdded;

    public StackUtil(ItemStack stack) {
        this.items = stack;
    }

    /**
     * Try adding the items given in parameter in the inventory, at the given
     * stackIndex. If doAdd is false, then no item will actually get added. If
     * addInEmpty is true, then items will be added in empty slots only,
     * otherwise in slot containing the same item only.
     * 
     * This will add one item at a time, and decrease the items member.
     */
    public boolean tryAdding(IInventory inventory, int stackIndex, boolean doAdd, boolean addInEmpty) {
        ItemStack stack = inventory.getStackInSlot(stackIndex);

        if (!addInEmpty) {
            if (stack != null)
                if (stack.getItem() == items.getItem() && (!stack.getHasSubtypes() || stack.getItemDamage() == items.getItemDamage())
                && ItemStack.func_77970_a(stack, items)
                && stack.stackSize + 1 <= stack.getMaxStackSize()
                && stack.stackSize + 1 <= inventory.getInventoryStackLimit()) {

                    if (doAdd) {
                        stack.stackSize++;
                        itemsAdded++;
                    }

                    return true;
                }
        } else if (stack == null) {
            if (doAdd) {
                // need to to a copy to keep NBT with enchantements
                stack = items.copy();
                stack.stackSize = 1;

                itemsAdded++;
                inventory.setInventorySlotContents(stackIndex, stack);
            }

            return true;
        }

        return false;
    }

}
