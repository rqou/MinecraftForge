package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ModLoader
{
    public static final String VERSION = "ModLoader Server 1.1";
	
	/*
    private static final List<TextureFX> animList = new LinkedList<TextureFx>();
    */
    private static final Map<Integer, BaseMod> blockModels = new HashMap<Integer, BaseMod>();
    private static final Map<Integer, Boolean> blockSpecialInv = new HashMap<Integer, Boolean>();
    private static File cfgdir;
    private static File cfgfile;
    public static Level cfgLoggingLevel = Level.FINER;
    private static Map<String, Class> classMap = null;
    private static long clock = 0L;
    public static final boolean DEBUG = false;
    /*
    private static Field field_animList = null;
    private static Field field_armorList = null;
    */
    private static Field field_modifiers = null;
    /*
    private static Field field_TileEntityRenderers = null;
    */
    private static boolean hasInit = false;
    private static int highestEntityId = 3000;
    private static final HashMap<BaseMod, Boolean> inGameHooks = new HashMap<BaseMod, Boolean>();
    /*
    private static final Map inGUIHooks = new HashMap();
    private static Minecraft instance = null;
    private static int itemSpriteIndex = 0;
    private static int itemSpritesLeft = 0;
    private static final Map<BaseMod, HashMap<KeyBinding, Boolean[]>> keyList = new HashMap<BaseMod, HashMap<KeyBinding, Boolean[]>>();
    */
    private static File logfile = new File(/*Minecraft.getMinecraftDir(),*/ "ModLoader.txt");
    private static final Logger logger = Logger.getLogger("ModLoader");
    private static FileHandler logHandler = null;
    private static Method method_RegisterEntityID = null;
    private static Method method_RegisterTileEntity = null;
    private static File moddir; /* Moved to init()  = new File(Minecraft.getMinecraftDir(), "/mods/"); */
    private static final LinkedList<BaseMod> modList = new LinkedList<BaseMod>();
    private static int nextBlockModelID = 1000;
    private static final Map<Integer, HashMap<String, Integer>> overrides = new HashMap<Integer, HashMap<String, Integer>>();
    public static final Properties props = new Properties();
    private static BiomeGenBase standardBiomes[];
    private static int terrainSpriteIndex = 0;
    private static int terrainSpritesLeft = 0;
    private static String texPack = null;
    private static boolean texturesAdded = false;
    private static final boolean usedItemSprites[] = new boolean[256];
    private static final boolean usedTerrainSprites[] = new boolean[256];
    private static Map<String, String> localizedStrings = new HashMap<String, String>();
    /*
    private static String langPack = null;
    */
    private static boolean isClient = true;
    
    public static void AddAchievementDesc(Achievement achievement, String name, String desc)
    {
        try
        {
        	String key = (String)getPrivateValue(StatBase.class, achievement, 1);
            if (key.contains("."))
            {
                String parts[] = key.split("\\.");
                if (parts.length == 2)
                {
                    String statName = parts[1];
                    AddLocalization("achievement." + statName, name);
                    AddLocalization("achievement." + statName + ".desc", desc);
                    setPrivateValue(StatBase.class, achievement, 1, StatCollector.translateToLocal("achievement." + statName));
                    setPrivateValue(Achievement.class, achievement, 3, StatCollector.translateToLocal("achievement." + statName + ".desc"));
                }
                else
                {
                    setPrivateValue(StatBase.class, achievement, 1, name);
                    setPrivateValue(Achievement.class, achievement, 3, desc);
                }
            }
            else
            {
                setPrivateValue(StatBase.class, achievement, 1, name);
                setPrivateValue(Achievement.class, achievement, 3, desc);
            }
        }
        catch (IllegalArgumentException illegalargumentexception)
        {
            logger.throwing("ModLoader", "AddAchievementDesc", illegalargumentexception);
            ThrowException(illegalargumentexception);
        }
        catch (SecurityException securityexception)
        {
            logger.throwing("ModLoader", "AddAchievementDesc", securityexception);
            ThrowException(securityexception);
        }
        catch (NoSuchFieldException nosuchfieldexception)
        {
            logger.throwing("ModLoader", "AddAchievementDesc", nosuchfieldexception);
            ThrowException(nosuchfieldexception);
        }
    }

    public static int AddAllFuel(int itemID, int itemMeta)
    {
        logger.finest("Finding fuel for " + itemID + ":" + itemMeta);
        int value = 0;
        for (BaseMod mod : modList)
        {
        	value = mod.AddFuel(itemID, itemMeta);
        	if (value != 0)
        	{
                logger.finest("Returned " + value);
        		return value;
        	}
        }
        return 0;
    }

    public static void AddAllRenderers(Map map)
    {
        if (!hasInit)
        {
            init();
            logger.fine("Initialized");
        }
        
        for (BaseMod mod : modList)
        {
        	mod.AddRenderer(map);
        }
    }

    /* TODO: Move to Client Side Only Class
    public static void addAnimation(TextureFX fx)
    {
        logger.finest("Adding animation " + fx);
        for (TextureFX anim : animList)
        {
            if (anim.iconIndex == fx.iconIndex && anim.tileImage == fx.tileImage)
            {
                animList.remove(fx);
                break;
            }
        }

        animList.add(fx);
    }
    */

    public static int AddArmor(String prefix)
    {
    	/* TODO: Move to Client Side Only Class, Renamed to proper name? AddArmorRenderPrefix?
        try
        {
            String values[] = (String[])field_armorList.get(null);
            ArrayList<String> alValues = new ArrayList<String>(Arrays.asList(values));
            
            if (!alValues.contains(prefix))
            {
                alValues.add(prefix);
            }
            int index = alValues.indexOf(prefix);
            field_armorList.set(null, (alValues.toArray(new String[0])));
            return index;
        }
        catch (IllegalArgumentException illegalargumentexception)
        {
            logger.throwing("ModLoader", "AddArmor", illegalargumentexception);
            ThrowException("An impossible error has occured!", illegalargumentexception);
        }
        catch (IllegalAccessException illegalaccessexception)
        {
            logger.throwing("ModLoader", "AddArmor", illegalaccessexception);
            ThrowException("An impossible error has occured!", illegalaccessexception);
        }
        return -1;
        */
    	return 0;
    }

    public static void AddLocalization(String key, String value)
    {
        localizedStrings.put(key, value);
    }

    private static void addMod(ClassLoader loader, String fileName)
    {
        try
        {
            String className = fileName.split("\\.")[0];
            if (className.contains("$"))
            {
                return;
            }
            if (props.containsKey(className) && (props.getProperty(className).equalsIgnoreCase("no") || props.getProperty(className).equalsIgnoreCase("off")))
            {
                return;
            }
            Package namespace = (ModLoader.class).getPackage();
            if (namespace != null)
            {
                className = namespace.getName() + "." + className;
            }
            Class modClass = loader.loadClass(className);
            if (!(BaseMod.class).isAssignableFrom(modClass))
            {
                return;
            }
            setupProperties(modClass);
            BaseMod basemod = (BaseMod)modClass.newInstance();
            if (basemod != null)
            {
                modList.add(basemod);
                logger.fine("Mod Initialized: \"" + basemod + "\" from " + fileName);
                System.out.println("Mod Initialized: " + basemod);
            }
        }
        catch (Throwable throwable)
        {
            logger.fine("Failed to load mod from \"" + fileName + "\"");
            System.out.println("Failed to load mod from \"" + fileName + "\"");
            logger.throwing("ModLoader", "addMod", throwable);
            ThrowException(throwable);
        }
    }

    public static void AddName(Object obj, String name)
    {
        String key = null;
        if (obj instanceof Item)
        {
            Item item = (Item)obj;
            if (item.getItemName() != null)
            {
                key = item.getItemName() + ".name";
            }
        }
        else if (obj instanceof Block)
        {
            Block block = (Block)obj;
            if (block.getBlockName() != null)
            {
                key = block.getBlockName() + ".name";
            }
        }
        else if (obj instanceof ItemStack)
        {
            ItemStack itemstack = (ItemStack)obj;
            String stackKey = Item.itemsList[itemstack.itemID].getItemNameIS(itemstack);
            if (stackKey != null)
            {
                key = stackKey + ".name";
            }
        }
        else
        {
            Exception ex = new Exception(obj.getClass().getName() + " cannot have name attached to it!");
            logger.throwing("ModLoader", "AddName", ex);
            ThrowException(ex);
        }
        if (key != null)
        {
            AddLocalization(key, name);
        }
        else
        {
            Exception ex = new Exception(obj + " is missing name tag!");
            logger.throwing("ModLoader", "AddName", ex);
            ThrowException(ex);
        }
    }

    public static int addOverride(String texture, String override)
    {
    	/* TODO: Move this To a Client Side Only Class
        try
        {
            int i = getUniqueSpriteIndex(texture);
            addOverride(texture, override, i);
            return i;
        }
        catch (Throwable throwable)
        {
            logger.throwing("ModLoader", "addOverride", throwable);
            ThrowException(throwable);
            throw new RuntimeException(throwable);
        }
        */
    	return 0;
    }

    public static void addOverride(String texture, String override, int index)
    {
    	/* TODO: Move this to a ClientSide Only Class
        int textureID = -1;
        int spritesLeft = 0;
        if (texture.equals("/terrain.png"))
        {
            textureID = 0;
            spritesLeft = terrainSpritesLeft;
        }
        else if (texture.equals("/gui/items.png"))
        {
            textureID = 1;
            spritesLeft = itemSpritesLeft;
        }
        else
        {
            return;
        }
        System.out.println("Overriding " + texture + " with " + override + " @ " + index + ". " + spritesLeft + " left.");
        logger.finer("addOverride(" + texture + "," + override + "," + index + "). " + spritesLeft + " left.");
        HashMap<String, Integer> obj = overrides.get(textureID);
        if (obj == null)
        {
            obj = new HashMap<String, Integer>();
            overrides.put(textureID, obj);
        }
        obj.put(override, index);
        */
    }

    public static void AddRecipe(ItemStack result, Object... recipe)
    {
        CraftingManager.getInstance().addRecipe(result, recipe);
    }

    public static void AddShapelessRecipe(ItemStack result, Object... recipe)
    {
        CraftingManager.getInstance().addShapelessRecipe(result, recipe);
    }

    public static void AddSmelting(int sourceItemID, ItemStack result)
    {
        FurnaceRecipes.smelting().addSmelting(sourceItemID, result);
    }

    public static void AddSpawn(Class entityClass, int weight, int minSpawn, int maxSpawn, EnumCreatureType type)
    {
        AddSpawn(entityClass, weight, minSpawn, maxSpawn, type, null);
    }

    public static void AddSpawn(Class entityClass, int weight, int minSpawn, int maxSpawn, EnumCreatureType type, BiomeGenBase biomes[])
    {
        if (entityClass == null)
        {
            throw new IllegalArgumentException("entityClass cannot be null");
        }
        if (type == null)
        {
            throw new IllegalArgumentException("spawnList cannot be null");
        }
        if (biomes == null)
        {
            biomes = standardBiomes;
        }
        for (int l = 0; l < biomes.length; l++)
        {
            List<SpawnListEntry> list = biomes[l].getSpawnableList(type);
            if (list != null)
            {
                boolean replaced = false;
                for (Iterator<SpawnListEntry> iterator = list.iterator(); iterator.hasNext();)
                {
                    SpawnListEntry entry = iterator.next();
                    if (entry.entityClass == entityClass)
                    {
                        entry.itemWeight = weight;
                        entry.field_35484_b = minSpawn;
                        entry.field_35485_c= maxSpawn;
                        replaced = true;
                        break;
                    }
                }

                if (!replaced)
                {
                    list.add(new SpawnListEntry(entityClass, weight, minSpawn, maxSpawn));
                }
            }
        }
    }

    public static void AddSpawn(String entityName, int weight, int minSpawn, int maxSpawn, EnumCreatureType type)
    {
        AddSpawn(entityName, weight, minSpawn, maxSpawn, type, null);
    }

    public static void AddSpawn(String entityName, int weight, int minSpawn, int maxSpawn, EnumCreatureType type, BiomeGenBase biomes[])
    {
        Class entityClass = classMap.get(entityName);
        if (entityClass != null && (EntityLiving.class).isAssignableFrom(entityClass))
        {
            AddSpawn(entityClass, weight, minSpawn, maxSpawn, type, biomes);
        }
    }

    public static boolean DispenseEntity(World world, double posX, double posY, double posZ, int volX, int volZ, ItemStack item)
    {
        for (BaseMod mod : modList)
        {
        	if (mod.DispenseEntity(world, posX, posY, posZ, volX, volZ, item))
        	{
        		return true;
        	}
        }
        return false;
    }

    public static List<BaseMod> getLoadedMods()
    {
        return Collections.unmodifiableList(modList);
    }

    public static Logger getLogger()
    {
        return logger;
    }

    /* TODO: Move this to a Client Side Only Class
    public static Minecraft getMinecraftInstance()
    {
        if (instance == null)
        {
            try
            {
                ThreadGroup threadgroup = Thread.currentThread().getThreadGroup();
                int i = threadgroup.activeCount();
                Thread threads[] = new Thread[i];
                threadgroup.enumerate(threads);
                for (int j = 0; j < threads.length; j++)
                {
                    System.out.println(threads[j].getName());
                }

                for (int k = 0; k < threads.length; k++)
                {
                    if (!threads[k].getName().equals("Minecraft main thread"))
                    {
                        continue;
                    }
                    instance = (Minecraft)getPrivateValue(Thread.class, threads[k], "target");
                    break;
                }
            }
            catch (SecurityException securityexception)
            {
                logger.throwing("ModLoader", "getMinecraftInstance", securityexception);
                throw new RuntimeException(securityexception);
            }
            catch (NoSuchFieldException nosuchfieldexception)
            {
                logger.throwing("ModLoader", "getMinecraftInstance", nosuchfieldexception);
                throw new RuntimeException(nosuchfieldexception);
            }
        }
        return instance;
    }
    */

    public static Object getPrivateValue(Class tClass, Object instance, int index) throws IllegalArgumentException, SecurityException, NoSuchFieldException
    {
        try
        {
            Field field = tClass.getDeclaredFields()[index];
            field.setAccessible(true);
            return field.get(instance);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "getPrivateValue", ex);
            ThrowException("An impossible error has occured!", ex);
            return null;
        }
    }

    public static Object getPrivateValue(Class tClass, Object instance, String name) throws IllegalArgumentException, SecurityException, NoSuchFieldException
    {
        try
        {
            Field field = tClass.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        }
        catch (IllegalAccessException illegalaccessexception)
        {
            logger.throwing("ModLoader", "getPrivateValue", illegalaccessexception);
            ThrowException("An impossible error has occured!", illegalaccessexception);
            return null;
        }
    }

    /* TODO: This could probably be moved to a Client Side Only Class */
    public static int getUniqueBlockModelID(BaseMod mod, boolean flag)
    {
        int i = nextBlockModelID++;
        blockModels.put(Integer.valueOf(i), mod);
        blockSpecialInv.put(Integer.valueOf(i), Boolean.valueOf(flag));
        return i;
    }

    /* TODO: Some way of holding a map of these and sending to client on connect */
    public static int getUniqueEntityId()
    {
        return highestEntityId++;
    }

    private static int getUniqueItemSpriteIndex()
    {
    	/* Unneeded on server, Spite Indexes don't matter
        for (; itemSpriteIndex < usedItemSprites.length; itemSpriteIndex++)
        {
            if (!usedItemSprites[itemSpriteIndex])
            {
                usedItemSprites[itemSpriteIndex] = true;
                itemSpritesLeft--;
                return itemSpriteIndex++;
            }
        }

        Exception ex = new Exception("No more empty item sprite indices left!");
        logger.throwing("ModLoader", "getUniqueItemSpriteIndex", ex);
        ThrowException(ex);
        */
        return 0;
    }

    public static int getUniqueSpriteIndex(String texture)
    {
    	/* Unnneded server side
        if (texture.equals("/gui/items.png"))
        {
            return getUniqueItemSpriteIndex();
        }
        if (texture.equals("/terrain.png"))
        {
            return getUniqueTerrainSpriteIndex();
        }
        else
        {
            Exception ex = new Exception("No registry for this texture: " + texture);
            logger.throwing("ModLoader", "getUniqueItemSpriteIndex", ex);
            ThrowException(ex);
            return 0;
        }
        */
        return 0;
    }

    private static int getUniqueTerrainSpriteIndex()
    {
    	/* Unneeded Server Side
        for (; terrainSpriteIndex < usedTerrainSprites.length; terrainSpriteIndex++)
        {
            if (!usedTerrainSprites[terrainSpriteIndex])
            {
                usedTerrainSprites[terrainSpriteIndex] = true;
                terrainSpritesLeft--;
                return terrainSpriteIndex++;
            }
        }

        Exception ex = new Exception("No more empty terrain sprite indices left!");
        logger.throwing("ModLoader", "getUniqueItemSpriteIndex", ex);
        ThrowException(ex);
        */
        return 0;
    }

    private static void init()
    {
        if (hasInit)
        {
        	return;
        }
        hasInit = true;

        if (cfgdir == null)
        {
	        try
	        {
	            String basePath = ModLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
	            basePath = basePath.substring(0, basePath.lastIndexOf('/'));
	            cfgdir = new File(basePath, "/config/");
	            cfgfile = new File(basePath, "/config/ModLoader.cfg");
	            logfile = new File(basePath, "ModLoader.txt");
	            moddir = new File(basePath, "/mods/");
	        }
	        catch (URISyntaxException urisyntaxexception)
	        {
	            getLogger().throwing("ModLoader", "Init", urisyntaxexception);
	            ThrowException("ModLoader", urisyntaxexception);
	            return;
	        }
        }
        
        try 
        {
        	ModLoader.class.getClassLoader().loadClass("net.minecraft.client.Minecraft");
        	isClient = true;
        } 
        catch (ClassNotFoundException ex)
        {
        	isClient = false;
        }
        
        /* TODO: Move all these commented out stuff to a client side only class
        String s = "1111111111111111111111111111111111111101111111111111111111111111111111111111111111111111111111111111110111111111111111000111111111111101111111110000000101111111000000010100111100000000000000110000000000000000000000000000000000000000000000001111111111111111";
        String s1 = "1111111111111111111111111111110111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111110111111111111110000001111111111000000001111000000000111111000000000001111111000000001111111111111111111";
        for (int i = 0; i < 256; i++)
        {
            usedItemSprites[i] = s.charAt(i) == '1';
            if (!usedItemSprites[i])
            {
                itemSpritesLeft++;
            }
            usedTerrainSprites[i] = s1.charAt(i) == '1';
            if (!usedTerrainSprites[i])
            {
                terrainSpritesLeft++;
            }
        }
        */

        try
        {
        	/* 
            instance = (Minecraft)getPrivateValue(net.minecraft.client.Minecraft.class, null, 1);
            instance.entityRenderer = new EntityRendererProxy(instance);
            */
            classMap = (Map<String, Class>)getPrivateValue(EntityList.class, null, 0);
            field_modifiers = (Field.class).getDeclaredField("modifiers");
            field_modifiers.setAccessible(true);
            /*
            field_TileEntityRenderers = (TileEntityRenderer.class).getDeclaredFields()[0];
            field_TileEntityRenderers.setAccessible(true);
            field_armorList = (RenderPlayer.class).getDeclaredFields()[3];
            field_modifiers.setInt(field_armorList, field_armorList.getModifiers() & 0xffffffef);
            field_armorList.setAccessible(true);
            field_animList = (RenderEngine.class).getDeclaredFields()[6];
            field_animList.setAccessible(true);
            */
            LinkedList<BiomeGenBase> biomes = new LinkedList<BiomeGenBase>();
            for (Field field : BiomeGenBase.class.getDeclaredFields())
            {
                if ((field.getModifiers() & 8) != 0 && field.getType().isAssignableFrom(BiomeGenBase.class))
                {
                    BiomeGenBase biome = (BiomeGenBase)field.get(null);
                    if (!(biome instanceof BiomeGenHell) && !(biome instanceof BiomeGenEnd))
                    {
                        biomes.add(biome);
                    }
                }
            }

            standardBiomes = biomes.toArray(new BiomeGenBase[0]);
            try
            {
                method_RegisterTileEntity = TileEntity.class.getDeclaredMethod("a", Class.class, String.class);
            }
            catch (NoSuchMethodException nosuchmethodexception1)
            {
                method_RegisterTileEntity = TileEntity.class.getDeclaredMethod("addMapping", Class.class, String.class);
            }
            method_RegisterTileEntity.setAccessible(true);
            try
            {
                method_RegisterEntityID = EntityList.class.getDeclaredMethod("a", Class.class, String.class, Integer.TYPE);
            }
            catch (NoSuchMethodException nosuchmethodexception2)
            {
                method_RegisterEntityID = EntityList.class.getDeclaredMethod("addMapping", Class.class, String.class, Integer.TYPE);
            }
            method_RegisterEntityID.setAccessible(true);
        }
        catch (SecurityException ex)
        {
            logger.throwing("ModLoader", "init", ex);
            ThrowException(ex);
            throw new RuntimeException(ex);
        }
        catch (NoSuchFieldException ex)
        {
            logger.throwing("ModLoader", "init", ex);
            ThrowException(ex);
            throw new RuntimeException(ex);
        }
        catch (NoSuchMethodException ex)
        {
            logger.throwing("ModLoader", "init", ex);
            ThrowException(ex);
            throw new RuntimeException(ex);
        }
        catch (IllegalArgumentException ex)
        {
            logger.throwing("ModLoader", "init", ex);
            ThrowException(ex);
            throw new RuntimeException(ex);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "init", ex);
            ThrowException(ex);
            throw new RuntimeException(ex);
        }
        try
        {
            loadConfig();
            if (props.containsKey("loggingLevel"))
            {
                cfgLoggingLevel = Level.parse(props.getProperty("loggingLevel"));
            }
            /* TODO: Move this to Client Side Only Class
            if (props.containsKey("grassFix"))
            {
                RenderBlocks.cfgGrassFix = Boolean.parseBoolean(props.getProperty("grassFix"));
            }
            */
            logger.setLevel(cfgLoggingLevel);
            if ((logfile.exists() || logfile.createNewFile()) && logfile.canWrite() && logHandler == null)
            {
                logHandler = new FileHandler(logfile.getPath());
                logHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(logHandler);
            }
            
            logger.fine(VERSION + " Initializing...");
            System.out.println(VERSION + " Initializing...");
            
            moddir.mkdirs();
            readFromClassPath(new File(ModLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            readFromModFolder(moddir);
            sortModList();
            
        	for (BaseMod mod : modList)
            {
                mod.load();
                logger.fine("Mod Loaded: \"" + mod + "\"");
                System.out.println("Mod Loaded: " + mod);
                if (!props.containsKey(mod.getClass().getSimpleName()))
                {
                    props.setProperty(mod.getClass().getSimpleName(), "on");
                }
            }

            for (BaseMod mod : modList)
            {
            	mod.ModsLoaded();
            }

            System.out.println("Done.");
            props.setProperty("loggingLevel", cfgLoggingLevel.getName());
            /* TODO: Move this to Client Side Only Class
            props.setProperty("grassFix", Boolean.toString(RenderBlocks.cfgGrassFix));
            instance.gameSettings.keyBindings = RegisterAllKeys(instance.gameSettings.keyBindings);
            instance.gameSettings.loadOptions();
            */
            initStats();
            saveConfig();
        }
        catch (Throwable throwable)
        {
            logger.throwing("ModLoader", "init", throwable);
            ThrowException("ModLoader has failed to initialize.", throwable);
            if (logHandler != null)
            {
                logHandler.close();
            }
            throw new RuntimeException(throwable);
        }
    }

    private static void sortModList() throws Exception
    {
        HashMap<String, BaseMod> loadedMods = new HashMap<String, BaseMod>();
        for (BaseMod mod : getLoadedMods())
        {
        	loadedMods.put(mod.getClass().getSimpleName(), mod);
        }

        LinkedList<BaseMod> sorted = new LinkedList<BaseMod>();
        for (int i = 0; sorted.size() != modList.size(); i++)
        {
            if (i > 10)
            {
                break;
            }
            Iterator<BaseMod> mods = modList.iterator();
            beforeWhileIttr: //Eww labels!
            while (mods.hasNext())
            {
                BaseMod mod = mods.next();
                if (sorted.contains(mod))
                {
                    continue;
                }
                
                String priorities = mod.getPriorities();
                if (priorities == null || priorities.length() == 0 || priorities.indexOf(':') == -1)
                {
                    sorted.add(mod);
                    continue;
                }
                
                if (i <= 0)
                {
                    continue;
                }
                
                int index = -1;
                int k = 0x80000000;
                int l = 0x7fffffff;
                
                for (String priority : (priorities.indexOf(';') > 0 ? priorities.split(";") : new String[]{priorities}))
                {
                    if (priority.indexOf(':') == -1)
                    {
                        continue;
                    }
                    
                    String parts[] = priority.split(":");
                    String status = parts[0];
                    String modName = parts[1];
                    
                    if (!status.contentEquals("required-before") && !status.contentEquals("before") && !status.contentEquals("after") && !status.contentEquals("required-after"))
                    {
                        continue;
                    }
                    if (modName.contentEquals("*"))
                    {
                        if (status.contentEquals("required-before") || status.contentEquals("before"))
                        {
                            index = 0;
                        }
                        else if (status.contentEquals("required-after") || status.contentEquals("after"))
                        {
                            index = sorted.size();
                        }
                        break;
                    }
                    if ((status.contentEquals("required-before") || status.contentEquals("required-after")) && !loadedMods.containsKey(modName))
                    {
                        throw new Exception(String.format("%s is missing dependency: %s", mod, modName));
                    }
                    
                    BaseMod dependancy = loadedMods.get(modName);
                    if (!sorted.contains(dependancy))
                    {
                        continue beforeWhileIttr;
                    }
                    
                    int depIndex = sorted.indexOf(dependancy);
                    if (status.contentEquals("required-before") || status.contentEquals("before"))
                    {
                        index = depIndex;
                        if (index < l)
                        {
                            l = index;
                        }
                        else
                        {
                            index = l;
                        }
                    }
                    else if (status.contentEquals("required-after") || status.contentEquals("after"))
                    {
                        index = depIndex + 1;
                        if (index > k)
                        {
                            k = index;
                        }
                        else
                        {
                            index = k;
                        }
                    }
                }

                if (index != -1)
                {
                    sorted.add(index, mod);
                }
            }
        }

        modList.clear();
        modList.addAll(sorted);
    }

    private static void initStats()
    {
        for (int i = 0; i < Block.blocksList.length; i++)
        {
            if (!StatList.oneShotStats.containsKey(Integer.valueOf(0x1000000 + i)) && Block.blocksList[i] != null && Block.blocksList[i].getEnableStats())
            {
                String s = StringTranslate.getInstance().translateKeyFormat("stat.mineBlock", new Object[]{ Block.blocksList[i].translateBlockName() });
                StatList.mineBlockStatArray[i] = (new StatCrafting(0x1000000 + i, s, i)).registerStat();
                StatList.objectMineStats.add(StatList.mineBlockStatArray[i]);
            }
        }

        for (int j = 0; j < Item.itemsList.length; j++)
        {
            if (!StatList.oneShotStats.containsKey(Integer.valueOf(0x1020000 + j)) && Item.itemsList[j] != null)
            {
                String s1 = StringTranslate.getInstance().translateKeyFormat("stat.useItem", new Object[]{ Item.itemsList[j].getStatName() });
                StatList.objectUseStats[j] = (new StatCrafting(0x1020000 + j, s1, j)).registerStat();
                if (j >= Block.blocksList.length)
                {
                    StatList.itemStats.add(StatList.objectUseStats[j]);
                }
            }
            if (!StatList.oneShotStats.containsKey(Integer.valueOf(0x1030000 + j)) && Item.itemsList[j] != null && Item.itemsList[j].isDamageable())
            {
                String s2 = StringTranslate.getInstance().translateKeyFormat("stat.breakItem", new Object[]{ Item.itemsList[j].getStatName() });
                StatList.objectBreakStats[j] = (new StatCrafting(0x1030000 + j, s2, j)).registerStat();
            }
        }

        HashSet<Integer> recipieOutput = new HashSet<Integer>();
        for (Object obj : CraftingManager.getInstance().getRecipeList())
        {
        	recipieOutput.add(((IRecipe)obj).getRecipeOutput().itemID);
        }
        
        for (Object obj : FurnaceRecipes.smelting().getSmeltingList().values())
        {
        	recipieOutput.add(((ItemStack)obj).itemID);
        }

        for (int itemID : recipieOutput)
        {
            if (!StatList.oneShotStats.containsKey(Integer.valueOf(0x1010000 + itemID)) && Item.itemsList[itemID] != null)
            {
                String s3 = StringTranslate.getInstance().translateKeyFormat("stat.craftItem", new Object[]{ Item.itemsList[itemID].getStatName() });
                StatList.objectCraftStats[itemID] = (new StatCrafting(0x1010000 + itemID, s3, itemID)).registerStat();
            }
        }
    }

    public static boolean isGUIOpen(Class screen)
    {
    	/* TODO: Move this to Client Side Only Class
        Minecraft minecraft = getMinecraftInstance();
        if (class1 == null)
        {
            return minecraft.currentScreen == null;
        }
        if (minecraft.currentScreen == null && screen != null)
        {
            return false;
        }
        else
        {
            return class1.isInstance(minecraft.currentScreen);
        }
        */
    	return false;
    }

    public static boolean isModLoaded(String name)
    {
        Class modClass = null;
        try
        {
            modClass = Class.forName(name);
        }
        catch (ClassNotFoundException classnotfoundexception)
        {
            return false;
        }
        if (modClass != null)
        {
            for (BaseMod mod : modList)
            {
                if (modClass.isInstance(mod))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static void loadConfig() throws IOException
    {
        cfgdir.mkdir();
        if (!cfgfile.exists() && !cfgfile.createNewFile())
        {
            return;
        }
        if (cfgfile.canRead())
        {
            FileInputStream fileinputstream = new FileInputStream(cfgfile);
            props.load(fileinputstream);
            fileinputstream.close();
        }
    }

    /* TODO: Move this to a Client Side Only Class
    public static java.awt.image.BufferedImage loadImage(RenderEngine renderengine, String texture) throws Exception
    {
        TexturePackList texPack = (TexturePackList)getPrivateValue(RenderEngine.class, renderengine, 11);
        InputStream input = texPack.selectedTexturePack.getResourceAsStream(texture);
        if (input == null)
        {
            throw new Exception("Image not found: " + texture);
        }
        BufferedImage image = ImageIO.read(input);
        if (image == null)
        {
            throw new Exception("Image corrupted: " + texture);
        }
        else
        {
            return image;
        }
    }
    */

    public static void OnItemPickup(EntityPlayer player, ItemStack item)
    {
        BaseMod basemod;
        for (BaseMod mod : modList)
        {
        	mod.OnItemPickup(player, item);
        }
    }

    /* TODO: Move Minecraft Client Side Specific Stuff to Client Side Only Class
    public static void OnTick(float f, Minecraft minecraft)
    {
        Profiler.endSection();
        Profiler.endSection();
        Profiler.startSection("modtick");
        if (!hasInit)
        {
            init();
            logger.fine("Initialized");
        }
        if (texPack == null || minecraft.gameSettings.skin != texPack)
        {
            texturesAdded = false;
            texPack = minecraft.gameSettings.skin;
        }
        if (langPack == null || StringTranslate.getInstance().func_44024_c() != langPack)
        {
            Properties properties = null;
            try
            {
                properties = (Properties)getPrivateValue(StringTranslate.class, StringTranslate.getInstance(), 1);
            }
            catch (SecurityException securityexception)
            {
                logger.throwing("ModLoader", "AddLocalization", securityexception);
                ThrowException(securityexception);
            }
            catch (NoSuchFieldException nosuchfieldexception)
            {
                logger.throwing("ModLoader", "AddLocalization", nosuchfieldexception);
                ThrowException(nosuchfieldexception);
            }
            if (properties != null)
            {
                properties.putAll(localizedStrings);
            }
            langPack = StringTranslate.getInstance().func_44024_c();
        }
        if (!texturesAdded && minecraft.renderEngine != null)
        {
            RegisterAllTextureOverrides(minecraft.renderEngine);
            texturesAdded = true;
        }
        long l = 0L;
        if (minecraft.theWorld != null)
        {
            l = minecraft.theWorld.getWorldTime();
            for (Iterator iterator = inGameHooks.entrySet().iterator(); iterator.hasNext();)
            {
                java.util.Map.Entry entry1 = (java.util.Map.Entry)iterator.next();
                if ((clock != l || !((Boolean)entry1.getValue()).booleanValue()) && !((BaseMod)entry1.getKey()).OnTickInGame(f, minecraft))
                {
                    iterator.remove();
                }
            }
        }
        if (minecraft.standardGalacticFontRenderer != null)
        {
            for (Iterator iterator1 = inGUIHooks.entrySet().iterator(); iterator1.hasNext();)
            {
                java.util.Map.Entry entry2 = (java.util.Map.Entry)iterator1.next();
                if ((clock != l || !(((Boolean)entry2.getValue()).booleanValue() & (minecraft.theWorld != null))) && !((BaseMod)entry2.getKey()).OnTickInGUI(f, minecraft, minecraft.currentScreen))
                {
                    iterator1.remove();
                }
            }
        }
        if (clock != l)
        {
            for (Iterator iterator2 = keyList.entrySet().iterator(); iterator2.hasNext();)
            {
                java.util.Map.Entry entry = (java.util.Map.Entry)iterator2.next();
                for (Iterator iterator3 = ((Map)entry.getValue()).entrySet().iterator(); iterator3.hasNext();)
                {
                    java.util.Map.Entry entry3 = (java.util.Map.Entry)iterator3.next();
                    int i = ((KeyBinding)entry3.getKey()).keyCode;
                    boolean flag;
                    if (i < 0)
                    {
                        flag = Mouse.isButtonDown(i += 100);
                    }
                    else
                    {
                        flag = Keyboard.isKeyDown(i);
                    }
                    boolean aflag[] = (boolean[])entry3.getValue();
                    boolean flag1 = aflag[1];
                    aflag[1] = flag;
                    if (flag && (!flag1 || aflag[0]))
                    {
                        ((BaseMod)entry.getKey()).KeyboardEvent((KeyBinding)entry3.getKey());
                    }
                }
            }
        }
        clock = l;
        Profiler.endSection();
        Profiler.startSection("render");
        Profiler.startSection("gameRenderer");
    }
    */

    /* TODO: Move to Client Side Only Class
    public static void OpenGUI(EntityPlayer entityplayer, GuiScreen guiscreen)
    {
        if (!hasInit)
        {
            init();
            logger.fine("Initialized");
        }
        Minecraft minecraft = getMinecraftInstance();
        if (minecraft.renderViewEntity != entityplayer)
        {
            return;
        }
        if (guiscreen != null)
        {
            minecraft.displayGuiScreen(guiscreen);
        }
    }
    */

    public static void PopulateChunk(IChunkProvider provider, int i, int j, World world)
    {
        if (!hasInit)
        {
            init();
            logger.fine("Initialized");
        }
        Random random = new Random(world.getRandomSeed());
        long l = (random.nextLong() / 2L) * 2L + 1L;
        long l1 = (random.nextLong() / 2L) * 2L + 1L;
        random.setSeed((long)i * l + (long)j * l1 ^ world.getRandomSeed());
        
        for (BaseMod mod : modList)
        {
            //TODO: Make this more generic Flatmap, End, etc...
            if (provider instanceof ChunkProviderGenerate || provider instanceof ChunkProviderServer)
            {
                mod.GenerateSurface(world, random, i << 4, j << 4);
            }
            else if (provider instanceof ChunkProviderHell)
            {
                mod.GenerateNether(world, random, i << 4, j << 4);
            }
        }
    }

    private static void readFromClassPath(File archive) throws FileNotFoundException, IOException
    {
        logger.finer("Adding mods from " + archive.getCanonicalPath());
        
        ClassLoader loader = ModLoader.class.getClassLoader();
        if (archive.isFile() && (archive.getName().endsWith(".jar") || archive.getName().endsWith(".zip")))
        {
            logger.finer("Zip found.");
            FileInputStream fis = new FileInputStream(archive);
            ZipInputStream zip = new ZipInputStream(fis);
            
            do
            {
                ZipEntry zipentry = zip.getNextEntry();
                if (zipentry == null)
                {
                    break;
                }
                String name = zipentry.getName();
                if (!zipentry.isDirectory() && name.startsWith("mod_") && name.endsWith(".class"))
                {
                    addMod(loader, name);
                }
            }
            while (true);
            fis.close();
        }
        else if (archive.isDirectory())
        {
            Package namespace = ModLoader.class.getPackage();
            if (namespace != null)
            {
                String path = namespace.getName().replace('.', File.separatorChar);
                archive = new File(archive, path);
            }
            logger.finer("Directory found.");
            File files[] = archive.listFiles();
            if (files != null)
            {
            	for (File file : files)
                {
                    String name = file.getName();
                    if (file.isFile() && name.startsWith("mod_") && name.endsWith(".class"))
                    {
                        addMod(loader, name);
                    }
                }
            }
        }
    }

    private static void readFromModFolder(File path) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
    {
        ClassLoader loader = ModLoader.class.getClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        
        if (!path.isDirectory())
        {
            throw new IllegalArgumentException("folder must be a Directory.");
        }
        
        File files[] = path.listFiles();
        Arrays.sort(files);
        if (loader instanceof URLClassLoader)
        {
        	for (File file : files)
            {
                if (file.isDirectory() || file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")))
                {
                    method.invoke(loader, file.toURI().toURL());
                }
            }
        }
        
        for (File file : files)
        {
            if (file.isDirectory() || file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")))
            {
                logger.finer("Adding mods from " + file.getCanonicalPath());
                if (file.isFile())
                {
                    logger.finer("Zip found.");
                    FileInputStream fis = new FileInputStream(file);
                    ZipInputStream zip = new ZipInputStream(fis);
                    
                    do
                    {
                        ZipEntry entry = zip.getNextEntry();
                        if (entry == null)
                        {
                            break;
                        }
                        String name = entry.getName();
                        if (!entry.isDirectory() && name.startsWith("mod_") && name.endsWith(".class"))
                        {
                            addMod(loader, name);
                        }
                    }
                    while (true);
                    zip.close();
                    fis.close();
                }
                else if (file.isDirectory())
                {
                    Package namespace = ModLoader.class.getPackage();
                    if (namespace != null)
                    {
                        file = new File(file, namespace.getName().replace('.', File.separatorChar));
                    }
                    
                    logger.finer("Directory found.");
                    for (File subFile : file.listFiles())
                    {
                        String name = subFile.getName();
                        if (subFile.isFile() && name.startsWith("mod_") && name.endsWith(".class"))
                        {
                            addMod(loader, name);
                        }
                    }
                }
            }
        }
    }

    /* TODO: Move to Client Side Only Class
    public static KeyBinding[] RegisterAllKeys(KeyBinding[] bindings)
    {
        LinkedList<KeyBinding> tmp = new LinkedList<KeyBinding>();
        tmp.addAll(Arrays.asList(bindings));
        
        for (HashMap<KeyBinding, Boolean> maps  : keyList.values())
        {
        	tmp.addAll(maps.keySet());
        }

        return tmp.toArray(new KeyBinding[0]);
    }
    */
    
    /* TODO: Move to Client Side Only Class
    public static void RegisterAllTextureOverrides(RenderEngine engine)
    {
        animList.clear();
        Minecraft mc = getMinecraftInstance();
        
        for (BaseMod mod : modList)
        {
        	mod.RegisterAnimation(mc);
        }
        
        for (TextureFX fx : animList)
        {
        	engine.registerTextureFX(fx);
        }
        
        for (Map.Entry<Integer, HashMap<String, Integer>> entry : overrides.entrySet())
        {
        	for (Map.Entry<String, Integer> entry1 : entry.getValue().entrySet())
        	{
                String texture = entry1.getKey();
                int spriteIndex = entry1.getValue();
                int textureID = entry.getKey();
                
                try
                {
                    BufferedImage image = loadImage(engine, texture);
                    ModTextureStatic fx = new ModTextureStatic(spriteIndex, textureID, image);
                    engine.registerTextureFX(fx);
                }
                catch (Exception ex)
                {
                    logger.throwing("ModLoader", "RegisterAllTextureOverrides", ex);
                    ThrowException(ex);
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    */

    public static void RegisterBlock(Block block)
    {
        RegisterBlock(block, null);
    }

    public static void RegisterBlock(Block block, Class itemClass)
    {
        try
        {
            if (block == null)
            {
                throw new IllegalArgumentException("block parameter cannot be null.");
            }
            
            int id = block.blockID;
            ItemBlock item = null;
            if (itemClass != null)
            {
                item = (ItemBlock)itemClass.getConstructor(Integer.TYPE).newInstance(id - Block.blocksList.length);
            }
            else
            {
                item = new ItemBlock(id - Block.blocksList.length);
            }
            if (Block.blocksList[id] != null && Item.itemsList[id] == null)
            {
                Item.itemsList[id] = item;
            }
        }
        catch (IllegalArgumentException ex)
        {
            logger.throwing("ModLoader", "RegisterBlock", ex);
            ThrowException(ex);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "RegisterBlock", ex);
            ThrowException(ex);
        }
        catch (SecurityException ex)
        {
            logger.throwing("ModLoader", "RegisterBlock", ex);
            ThrowException(ex);
        }
        catch (InstantiationException ex)
        {
            logger.throwing("ModLoader", "RegisterBlock", ex);
            ThrowException(ex);
        }
        catch (InvocationTargetException ex)
        {
            logger.throwing("ModLoader", "RegisterBlock", ex);
            ThrowException(ex);
        }
        catch (NoSuchMethodException ex)
        {
            logger.throwing("ModLoader", "RegisterBlock", ex);
            ThrowException(ex);
        }
    }

    public static void RegisterEntityID(Class entityClass, String name, int id)
    {
        try
        {
            method_RegisterEntityID.invoke(null, entityClass, name, id);
        }
        catch (IllegalArgumentException ex)
        {
            logger.throwing("ModLoader", "RegisterEntityID", ex);
            ThrowException(ex);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "RegisterEntityID", ex);
            ThrowException(ex);
        }
        catch (InvocationTargetException ex)
        {
            logger.throwing("ModLoader", "RegisterEntityID", ex);
            ThrowException(ex);
        }
    }

    public static void RegisterEntityID(Class entityClass, String name, int id, int mainColor, int subColor)
    {
        RegisterEntityID(entityClass, name, id);
        EntityList.field_44015_a.put(id, new EntityEggInfo(id, mainColor, subColor));
    }

    /* TODO: Move to Client Side Only Class
    public static void RegisterKey(BaseMod mod, KeyBinding binding, boolean repeat)
    {
    	HashMap<KeyBinding, Boolean[]> binds = keyList.get(mod);
        if (binds == null)
        {
            binds = new HashMap<KeyBinding, Boolean[]>();
        }
        
        binds.put(binding, new boolean[]{ repeat, false });
        keyList.put(mod, binds);
    }
    */

    
    public static void RegisterTileEntity(Class tileClass, String name)
    {
        try
        {
            method_RegisterTileEntity.invoke(null, tileClass, name);
        }
        catch (IllegalArgumentException ex)
        {
            logger.throwing("ModLoader", "RegisterTileEntity", ex);
            ThrowException(ex);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "RegisterTileEntity", ex);
            ThrowException(ex);
        }
        catch (InvocationTargetException ex)
        {
            logger.throwing("ModLoader", "RegisterTileEntity", ex);
            ThrowException(ex);
        }
    }

    /* TODO: Move to Client Side Only Class
    public static void RegisterTileEntity(Class tileClass, String name, TileEntitySpecialRenderer renderer)
    {
        try
        {
        	RegisterTileEntity(tileClass, name);
            if (renderer != null)
            {
                TileEntityRenderer inst = TileEntityRenderer.instance;
                Map map = (Map)field_TileEntityRenderers.get(inst);
                map.put(tileClass, renderer);
                renderer.setTileEntityRenderer(inst);
            }
        }
        catch (IllegalArgumentException ex)
        {
            logger.throwing("ModLoader", "RegisterTileEntity", ex);
            ThrowException(ex);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "RegisterTileEntity", ex);
            ThrowException(ex);
        }
        catch (InvocationTargetException ex)
        {
            logger.throwing("ModLoader", "RegisterTileEntity", ex);
            ThrowException(ex);
        }
    }
    */

    public static void RemoveSpawn(Class entityClass, EnumCreatureType type)
    {
        RemoveSpawn(entityClass, type, null);
    }

    public static void RemoveSpawn(Class entityClass, EnumCreatureType type, BiomeGenBase biomes[])
    {
        if (entityClass == null)
        {
            throw new IllegalArgumentException("entityClass cannot be null");
        }
        if (type == null)
        {
            throw new IllegalArgumentException("spawnList cannot be null");
        }
        if (biomes == null)
        {
            biomes = standardBiomes;
        }
        for (int i = 0; i < biomes.length; i++)
        {
            List<SpawnListEntry> list = biomes[i].getSpawnableList(type);
            if (list != null)
            {
                for (Iterator<SpawnListEntry> itr = list.iterator(); itr.hasNext();)
                {
                    if ((itr.next()).entityClass == entityClass)
                    {
                        itr.remove();
                    }
                }
            }
        }
    }

    public static void RemoveSpawn(String name, EnumCreatureType type)
    {
        RemoveSpawn(name, type, null);
    }

    public static void RemoveSpawn(String name, EnumCreatureType type, BiomeGenBase biomes[])
    {
        Class entityClass = classMap.get(name);
        if (entityClass != null && EntityLiving.class.isAssignableFrom(entityClass))
        {
            RemoveSpawn(entityClass, type, biomes);
        }
    }

    public static boolean RenderBlockIsItemFull3D(int id)
    {
    	//TODO: This should be moved to a Client Side Only Class
        if (!blockSpecialInv.containsKey(id))
        {
            return id == 16;
        }
        else
        {
            return blockSpecialInv.get(id);
        }
    }

    /* TODO: Move to Client Side Only Class
    public static void RenderInvBlock(RenderBlocks renderblocks, Block block, int blockID, int renderType)
    {
        BaseMod mod = blockModels.get(renderType);
        if (mod != null)
        {
            mod.RenderInvBlock(renderblocks, block, blockID, renderType);
            return;
        }
    }
    */

    /* TODO: Move to Client Side Only Class
    public static boolean RenderWorldBlock(RenderBlocks renderblocks, IBlockAccess world, int xPos, int yPos, int zPos, Block block, int renderType)
    {
        BaseMod mod = blockModels.get(renderType);
        if (mod == null)
        {
            return false;
        }
        else
        {
            return mod.RenderWorldBlock(renderblocks, world, xPos, yPos, zPos, block, renderType);
        }
    }
    */

    public static void saveConfig() throws IOException
    {
        cfgdir.mkdir();
        if (!cfgfile.exists() && !cfgfile.createNewFile())
        {
            return;
        }
        if (cfgfile.canWrite())
        {
            FileOutputStream fos = new FileOutputStream(cfgfile);
            props.store(fos, "ModLoader Config");
            fos.close();
        }
    }

    public static void SetInGameHook(BaseMod mod, boolean add, boolean repeat)
    {
        if (add)
        {
            inGameHooks.put(mod, repeat);
        }
        else
        {
            inGameHooks.remove(mod);
        }
    }
    
    public static Map<BaseMod, Boolean> getInGameHooks()
    {
    	return Collections.unmodifiableMap(inGameHooks);
    }

    public static void SetInGUIHook(BaseMod mod, boolean add, boolean repeat)
    {
    	/* TODO: Move to Client Side Only Class, As not ran on server
        if (add)
        {
            inGUIHooks.put(mod, repeat);
        }
        else
        {
            inGUIHooks.remove(mod);
        }
        */
    }

    public static void setPrivateValue(Class cls, Object inst, int index, Object value) throws IllegalArgumentException, SecurityException, NoSuchFieldException
    {
        try
        {
            Field field = cls.getDeclaredFields()[index];
            field.setAccessible(true);
            int j = field_modifiers.getInt(field);
            if ((j & 0x10) != 0)
            {
                field_modifiers.setInt(field, j & 0xffffffef);
            }
            field.set(inst, value);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "setPrivateValue", ex);
            ThrowException("An impossible error has occured!", ex);
        }
    }

    public static void setPrivateValue(Class cls, Object inst, String name, Object value) throws IllegalArgumentException, SecurityException, NoSuchFieldException
    {
        try
        {
            Field field = cls.getDeclaredField(name);
            int i = field_modifiers.getInt(field);
            
            if ((i & 0x10) != 0)
            {
                field_modifiers.setInt(field, i & 0xffffffef);
            }
            
            field.setAccessible(true);
            field.set(inst, value);
        }
        catch (IllegalAccessException ex)
        {
            logger.throwing("ModLoader", "setPrivateValue", ex);
            ThrowException("An impossible error has occured!", ex);
        }
    }

    private static void setupProperties(Class modClass)  throws IllegalArgumentException, IllegalAccessException, IOException, SecurityException, NoSuchFieldException, NoSuchAlgorithmException, DigestException
    {
        LinkedList<Field> propFields = new LinkedList<Field>();
        Properties properties = new Properties();
        
        int checksum = 0;
        int cfgChecksum = 0;
        
        File cfg = new File(cfgdir, modClass.getSimpleName() + ".cfg");
        
        if (cfg.exists() && cfg.canRead())
        {
            properties.load(new FileInputStream(cfg));
        }
        
        if (properties.containsKey("checksum"))
        {
            cfgChecksum = Integer.parseInt(properties.getProperty("checksum"), 36);
        }
        
        for (Field field : modClass.getDeclaredFields())
        {
            if ((field.getModifiers() & 8) != 0 && field.isAnnotationPresent(MLProp.class))
            {
                propFields.add(field);
                checksum += field.get(null).hashCode();
            }
        }

        StringBuilder configLine = new StringBuilder();
        for (Field field : propFields)
        {
            if ((field.getModifiers() & 8) == 0 || !field.isAnnotationPresent(MLProp.class))
            {
                continue;
            }
            
            Class fieldType = field.getType();
            
            MLProp mlprop = field.getAnnotation(MLProp.class);
            
            String name = mlprop.name().length() != 0 ? mlprop.name() : field.getName();
            
            Object staticValue = field.get(null);
            
            StringBuilder valueRange = new StringBuilder();
            if (mlprop.min() != Double.NEGATIVE_INFINITY)
            {
                valueRange.append(String.format(",>=%.1f", mlprop.min()));
            }
            
            if (mlprop.max() != Double.POSITIVE_INFINITY)
            {
                valueRange.append(String.format(",<=%.1f", mlprop.max()));
            }
            
            StringBuilder info = new StringBuilder();
            if (mlprop.info().length() > 0)
            {
                info.append(" -- ");
                info.append(mlprop.info());
            }
            
            configLine.append(String.format("%s (%s:%s%s)%s\n", name, fieldType.getName(), staticValue, valueRange, info));
            if (cfgChecksum == checksum && properties.containsKey(name))
            {
                String value = properties.getProperty(name);
                Object typedValue = null;
                if (fieldType.isAssignableFrom(String.class))
                {
                    typedValue = value;
                }
                else if (fieldType.isAssignableFrom(Integer.TYPE))
                {
                    typedValue = Integer.parseInt(value);
                }
                else if (fieldType.isAssignableFrom(Short.TYPE))
                {
                    typedValue = Short.parseShort(value);
                }
                else if (fieldType.isAssignableFrom(Byte.TYPE))
                {
                    typedValue = Byte.parseByte(value);
                }
                else if (fieldType.isAssignableFrom(Boolean.TYPE))
                {
                    typedValue = Boolean.parseBoolean(value);
                }
                else if (fieldType.isAssignableFrom(Float.TYPE))
                {
                    typedValue = Float.parseFloat(value);
                }
                else if (fieldType.isAssignableFrom(Double.TYPE))
                {
                    typedValue = Double.parseDouble(value);
                }
                if (typedValue == null)
                {
                    continue;
                }
                if (typedValue instanceof Number)
                {
                    double d = ((Number)typedValue).doubleValue();
                    if (mlprop.min() != (-1.0D / 0.0D) && d < mlprop.min() || mlprop.max() != (1.0D / 0.0D) && d > mlprop.max())
                    {
                        continue;
                    }
                }
                logger.finer(name + " set to " + typedValue);
                if (!typedValue.equals(staticValue))
                {
                    field.set(null, typedValue);
                }
            }
            else
            {
                logger.finer(name + " not in config, using default: " + staticValue);
                properties.setProperty(name, staticValue.toString());
            }
        }
        properties.put("checksum", Integer.toString(checksum, 36));
        if (!properties.isEmpty() && (cfg.exists() || cfg.createNewFile()) && cfg.canWrite())
        {
            properties.store(new FileOutputStream(cfg), configLine.toString());
        }
    }

    public static void TakenFromCrafting(EntityPlayer player, ItemStack stack, IInventory matrix)
    {
        for (BaseMod mod : modList)
        {
        	mod.TakenFromCrafting(player, stack, matrix);
        }
    }

    public static void TakenFromFurnace(EntityPlayer player, ItemStack stack)
    {
        for(BaseMod mod : modList)
        {
        	mod.TakenFromFurnace(player, stack);
        }
    }

    public static void ThrowException(String functionName, Throwable throwable)
    {
        /*Minecraft minecraft = getMinecraftInstance();
        if (minecraft != null)
        {
            minecraft.displayUnexpectedThrowable(new UnexpectedThrowable(functionName, throwable));
        }
        else
        {*/
            throw new RuntimeException(throwable);
        //}
    }

    private static void ThrowException(Throwable throwable)
    {
        ThrowException("Exception occured in ModLoader", throwable);
    }

    private ModLoader()
    {
    }
    
    /**
     * Used to determine if the mod is running on the Client, or on the Server
     * @return True if the client, false otherwise
     */
    public boolean isClient()
    {
    	return isClient;
    }
    
    /*************************************************************************
     * ModLoader MP Changes that break the side agnostic nature of this class.
     *************************************************************************/

	public static Object getMinecraftServerInstance() 
	{
		if (isClient)
			return null;
		else
			return ModLoaderMp.getMinecraftServerInstance();
	}
	
	public static void setDirectories(File configDir, File configFile, File logFile, File modDir)
	{
		if (cfgdir != null)
		{
			ThrowException("setDirectories", new Exception("Tried to set directories after they have already been set"));
		}
        cfgdir = configDir;
        cfgfile = configFile;
        logfile = logFile;
        moddir = modDir;
	}
}
