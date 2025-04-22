package cn.chengzhiya;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Main {
    @Getter
    private static final File configFile = new File("config.yml");
    @Getter
    private static final YamlConfiguration config = YamlConfiguration.loadConfiguration(getConfigFile());
    private static final List<String> configFolderNameList = getConfig().getStringList("configFolderNameList");
    private static final List<String> resourceFolderNameList = getConfig().getStringList("resourceFolderNameList");
    private static final HashMap<String, List<Integer>> blockUseIdHashMap = new HashMap<>();
    private static final HashMap<String, Integer> blockIdHashMap = new HashMap<>();
    private static final HashMap<String, List<String>> namespaceItemListHashMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        File inFolder = new File("in");
        if (!inFolder.exists()) {
            System.out.println("找不到输入目录!");
            return;
        }

        File outFolder = new File("out");
        if (outFolder.exists()) {
            removeFiles(outFolder);
        }
        outFolder.mkdir();

        updateBlockUseIdHashMap();
        System.out.println("方块状态ID占用情况: " + blockUseIdHashMap);
        System.out.println("初始化完成!");

        HashMap<String, List<File>> namespaceConfigHashMap = new HashMap<>();
        {
            for (File contentFolder : Objects.requireNonNull(inFolder.listFiles())) {
                for (String configFolderName : configFolderNameList) {
                    File configFolder = new File(contentFolder, configFolderName);
                    if (!configFolder.exists()) {
                        continue;
                    }

                    for (File configFile : listFiles(configFolder)) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        String namespace = config.getString("info.namespace");
                        if (namespace == null) {
                            continue;
                        }

                        List<File> fileList = (namespaceConfigHashMap.get(namespace) != null) ? namespaceConfigHashMap.get(namespace) : new ArrayList<>();
                        fileList.add(configFile);

                        namespaceConfigHashMap.put(namespace, fileList);
                    }
                }
            }
        }
        System.out.println("遍历到所有空间命名的配置文件 " + namespaceConfigHashMap);

        HashMap<String, List<File>> namespaceResourceHashMap = new HashMap<>();
        {
            for (File contentFolder : Objects.requireNonNull(inFolder.listFiles())) {
                for (String resourceFolderName : resourceFolderNameList) {
                    File resourceFolder = new File(contentFolder, resourceFolderName);
                    if (!resourceFolder.exists()) {
                        continue;
                    }

                    for (File resourceFile : listFiles(resourceFolder)) {
                        String allPath = resourceFile.getPath();
                        String[] path = allPath.split("\\\\");
                        if (path.length <= 4) {
                            continue;
                        }

                        String namespace = path[3].equals("assets") ? path[4] : path[3];

                        List<File> fileList = (namespaceResourceHashMap.get(namespace) != null) ? namespaceResourceHashMap.get(namespace) : new ArrayList<>();
                        fileList.add(resourceFile);

                        namespaceResourceHashMap.put(namespace, fileList);
                    }
                }
            }
        }
        System.out.println("遍历到所有空间命名的资源文件 " + namespaceResourceHashMap);

        System.out.println("开始处理配置文件!");
        for (String namespace : namespaceConfigHashMap.keySet()) {
            System.out.println("开始处理空间ID为 " + namespace + " 的配置文件！");
            File namespaceFolder = new File(outFolder, namespace);
            namespaceFolder.mkdirs();

            File configFolder = new File(namespaceFolder, "configuration");
            configFolder.mkdirs();

            List<File> configList = namespaceConfigHashMap.get(namespace);
            for (File configFile : configList) {
                String allPath = configFile.getPath();
                String configFolderName = allPath.split("\\\\")[2];
                String path = allPath.substring(allPath.lastIndexOf(configFolderName) + configFolderName.length());

                Path outPath = new File(configFolder, path).toPath();
                Files.createDirectories(outPath.getParent());

                File outFile = outPath.toFile();
                outFile.createNewFile();
                System.out.println("创建配置文件 " + outPath + " 完成!");

                System.out.println("开始转换配置文件 " + configFile.getPath() + " 至 " + outFile.getPath());
                concertConfig(configFile, outFile);
                System.out.println("转换配置文件 " + configFile.getPath() + " 至 " + outFile.getPath() + " 完成!");
            }
        }
        System.out.println("配置文件处理完成!");

        System.out.println("开始处理资源文件!");
        for (String namespace : namespaceResourceHashMap.keySet()) {
            System.out.println("开始处理空间ID为 " + namespace + " 的资源文件！");

            File namespaceFolder = new File(outFolder, namespace);
            namespaceFolder.mkdirs();

            File resourcepackFolder = new File(namespaceFolder, "resourcepack");
            resourcepackFolder.mkdirs();

            List<File> resourceList = namespaceResourceHashMap.get(namespace);
            for (File resourceFile : resourceList) {
                String allPath = resourceFile.getPath();
                String path = allPath.substring(allPath.indexOf(allPath.split("\\\\")[3]));
                if (!path.startsWith("assets\\")) {
                    path = "assets\\" + path;
                }

                Path outPath = new File(resourcepackFolder, path).toPath();
                Files.createDirectories(outPath.getParent());
                Files.copy(resourceFile.toPath(), outPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("复制资源文件 " + outPath + " 完成!");
            }
        }
        System.out.println("资源文件处理完成!");

        System.out.println("开始处理纹理图集!");
        {
            File blocksFile = new File(outFolder, "minecraft/resourcepack/assets/minecraft/atlases/blocks.json");
            if (!blocksFile.exists()) {
                Files.createDirectories(blocksFile.toPath().getParent());
                blocksFile.createNewFile();
            }

            JSONObject blocks = JSON.parseObject(new String(Files.readAllBytes(blocksFile.toPath())));
            if (blocks == null) {
                blocks = new JSONObject();
            }

            JSONArray sources = blocks.getJSONArray("sources");
            if (sources == null) {
                sources = new JSONArray();
            }

            for (String namespace : namespaceResourceHashMap.keySet()) {
                if (namespace.equals("minecraft")) {
                    continue;
                }

                System.out.println("开始处理空间ID为 " + namespace + " 的纹理图集！");
                List<File> resourceList = namespaceResourceHashMap.get(namespace);
                for (File resourceFile : resourceList) {
                    String allPath = resourceFile.getPath();
                    String path = allPath.substring(allPath.indexOf(allPath.split("\\\\")[3]));
                    if (!path.contains("textures\\")) {
                        continue;
                    }
                    if (!path.endsWith(".png")) {
                        continue;
                    }

                    path = path.replace("\\", "/");
                    path = path.replace("assets/", "");
                    path = path.replace(namespace + "/textures/", "");
                    path = path.replace("textures/", "");
                    path = path.replace(".png", "");
                    path = namespace + ":" + path;

                    JSONObject source = new JSONObject();
                    source.put("type", "single");
                    source.put("resource", path);
                    sources.add(source);
                }
                blocks.put("sources", sources);
            }

            try (FileWriter writer = new FileWriter(blocksFile)) {
                writer.write(blocks.toJSONString());
            }
        }
        System.out.println("纹理图集处理完成!");

        System.out.println("开始创建资源信息文件!");
        {
            for (File contentFolder : Objects.requireNonNull(outFolder.listFiles())) {
                File packInfoFile = new File(contentFolder, "pack.yml");
                packInfoFile.createNewFile();
                System.out.println("开始创建资源信息文件 " + packInfoFile.getPath());

                YamlConfiguration packInfo = YamlConfiguration.loadConfiguration(packInfoFile);
                packInfo.set("author", getConfig().getString("author"));
                packInfo.set("version", getConfig().getString("version"));
                packInfo.set("description", "");
                packInfo.set("namespace", contentFolder.getName());
                packInfo.save(packInfoFile);

                System.out.println("创建资源信息文件 " + packInfoFile.getPath() + " 完成!");
            }
        }
        System.out.println("创建资源信息文件完成!");

        System.out.println("开始创建物品列表文件!");
        {
            for (File contentFolder : Objects.requireNonNull(outFolder.listFiles())) {
                String namespace = contentFolder.getName();
                List<String> itemList = namespaceItemListHashMap.get(namespace);
                if (itemList == null || itemList.isEmpty()) {
                    continue;
                }

                File categoriesFile = new File(contentFolder, "configuration/categories.yml");
                categoriesFile.createNewFile();
                System.out.println("开始创建物品列表文件 " + categoriesFile.getPath());

                YamlConfiguration categories = YamlConfiguration.loadConfiguration(categoriesFile);
                categories.set("categories." + namespace + ":default.name", namespace);
                categories.set("categories." + namespace + ":default.lore", new ArrayList<>());
                categories.set("categories." + namespace + ":default.icon", itemList.get(0));
                categories.set("categories." + namespace + ":default.list", itemList);
                categories.save(categoriesFile);

                System.out.println("创建物品列表文件 " + categoriesFile.getPath() + " 完成!");
            }
            System.out.println("创建物品列表文件完成!");
        }
    }

    public static void concertConfig(File inFile, File outFile) throws IOException {
        File idFile = new File("items_ids_cache.yml");
        YamlConfiguration id = YamlConfiguration.loadConfiguration(idFile);

        File unicodeFile = new File("font_images_unicode_cache.yml");
        YamlConfiguration unicode = YamlConfiguration.loadConfiguration(unicodeFile);

        YamlConfiguration in = YamlConfiguration.loadConfiguration(inFile);
        YamlConfiguration out = YamlConfiguration.loadConfiguration(outFile);

        String namespace = in.getString("info.namespace");

        // 处理物品（物品、方块、家具）
        YamlConfiguration items = in.getConfigurationSection("items");
        if (items != null) {
            List<String> itemList = namespaceItemListHashMap.get(namespace) != null ? namespaceItemListHashMap.get(namespace) : new ArrayList<>();
            for (String key : items.getKeys()) {
                YamlConfiguration item = items.getConfigurationSection(key);
                if (item == null) {
                    continue;
                }

                itemList.add(namespace + ":" + key);
                System.out.println("开始转换物品 " + namespace + ":" + key);

                String model = null;

                // 处理基础属性
                {
                    String displayName = item.getString("display_name");
                    if (displayName != null) {
                        out.set("items." + namespace + ":" + key + ".data.display-name", displayName);
                    }

                    List<String> lore = item.getStringList("lore");
                    if (!lore.isEmpty()) {
                        out.set("items." + namespace + ":" + key + ".data.lore", lore);
                    }

                    String material = item.getString("resource.material");
                    out.set("items." + namespace + ":" + key + ".material", material);

                    Integer customModelData = id.getInt(material + "." + namespace + ":" + key);
                    if (customModelData != null) {
                        out.set("items." + namespace + ":" + key + ".custom-model-data", customModelData);
                    }

                    out.set("items." + namespace + ":" + key + ".model.type", "minecraft:model");

                    boolean generate = item.getBoolean("resource.generate");
                    if (generate) {
                        List<String> textures = item.getStringList("resource.textures");
                        if (!textures.isEmpty()) {
                            out.set("items." + namespace + ":" + key + ".model.generation.parent", "minecraft:item/handheld");

                            model = namespace + ":" + textures.get(0).replace(".png", "");
                            out.set("items." + namespace + ":" + key + ".model.path", model);
                            out.set("items." + namespace + ":" + key + ".model.generation.textures.layer0", model);
                        }
                    } else {
                        String modelPath = item.getString("resource.model_path");
                        if (modelPath != null) {
                            model = namespace + ":" + modelPath;
                            out.set("items." + namespace + ":" + key + ".model.path", model);
                        }
                    }
                }

                // 处理方块属性
                if (item.getConfigurationSection("specific_properties.block") != null) {
                    out.set("items." + namespace + ":" + key + ".behavior.type", "block_item");

                    out.set("items." + namespace + ":" + key + ".behavior.block.loot.template", getConfig().getString("defaultLootTable"));
                    out.set("items." + namespace + ":" + key + ".behavior.block.loot.arguments.item", namespace + ":" + key);

                    out.set("items." + namespace + ":" + key + ".behavior.block.settings.item", namespace + ":" + key);

                    // 音效
                    {
                        String breakSound = item.getString("specific_properties.block.sound.break");
                        if (breakSound != null) {
                            out.set("items." + namespace + ":" + key + ".behavior.block.settings.sounds.break", breakSound);
                        }

                        String stepSound = item.getString("specific_properties.block.sound.step");
                        if (stepSound != null) {
                            out.set("items." + namespace + ":" + key + ".behavior.block.settings.sounds.step", stepSound);
                        }

                        String placeSound = item.getString("specific_properties.block.sound.place");
                        if (placeSound != null) {
                            out.set("items." + namespace + ":" + key + ".behavior.block.settings.sounds.place", placeSound);
                        }

                        String hitSound = item.getString("specific_properties.block.sound.hit");
                        if (hitSound != null) {
                            out.set("items." + namespace + ":" + key + ".behavior.block.settings.sounds.hit", hitSound);
                        }

                        String fallSound = item.getString("specific_properties.block.sound.fall");
                        if (fallSound != null) {
                            out.set("items." + namespace + ":" + key + ".behavior.block.settings.sounds.fall", fallSound);
                        }
                    }

                    String type = item.getString("specific_properties.block.placed_model.type");
                    if (type == null) {
                        type = "REAL_NOTE";
                    }

                    String craftengineType = "note_block";
                    if (type.equals("REAL_WIRE")) {
                        craftengineType = "tripwire";
                    }

                    int blockId = getNextBlockId(craftengineType);

                    out.set("items." + namespace + ":" + key + ".behavior.block.state.id", blockId);
                    out.set("items." + namespace + ":" + key + ".behavior.block.state.state", craftengineType + ":" + blockId);
                    out.set("items." + namespace + ":" + key + ".behavior.block.state.model.path", model);

                    blockId++;
                    blockIdHashMap.put(craftengineType, blockId);
                }

                // 处理家具属性
                if (item.getConfigurationSection("behaviours.furniture") != null) {
                    out.set("items." + namespace + ":" + key + ".behavior.type", "furniture_item");
                    out.set("items." + namespace + ":" + key + ".behavior.furniture.settings.item", namespace + ":" + key);

                    out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.rules.rotation", "ANY");
                    out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.rules.alignment", "CENTER");

                    List<YamlConfiguration> hitboxs = new ArrayList<>();
                    Double length = item.getDouble("behaviours.furniture.hitbox.width");
                    if (length == null) {
                        length = 1.0;
                    }

                    Double width = item.getDouble("behaviours.furniture.hitbox.width");
                    if (width == null) {
                        width = 1.0;
                    }

                    Double height = item.getDouble("behaviours.furniture.hitbox.height");
                    if (height == null) {
                        height = 1.0;
                    }

                    double positionY = height - 0.5;

                    List<YamlConfiguration> elements = new ArrayList<>();
                    YamlConfiguration element = new YamlConfiguration();
                    element.set("item", namespace + ":" + key);
                    element.set("display-transform", "NONE");
                    element.set("billboard", "FIXED");
                    element.set("position", "0," + positionY + ",0");
                    element.set("translation", "0,0,0");
                    elements.add(element);

                    out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.elements", elements);

                    // 处理碰撞箱
                    {
                        for (int x = 0; x < length.intValue(); x++) {
                            for (int y = 0; y < height.intValue(); y++) {
                                for (int z = 0; z < width.intValue(); z++) {
                                    YamlConfiguration hitbox = new YamlConfiguration();
                                    hitbox.set("position", x + "," + y + "," + z);
                                    hitbox.set("width", 1);
                                    hitbox.set("height", 1);

                                    boolean interactive = false;

                                    // 处理椅子
                                    if (item.getConfigurationSection("behaviours.furniture_sit") != null) {
                                        double sitHeight = height - 1;

                                        List<String> seats = new ArrayList<>();
                                        seats.add("0,-" + sitHeight + ",0");
                                        hitbox.set("seats", seats);
                                    }

                                    hitbox.set("interactive", interactive);
                                    hitboxs.add(hitbox);
                                }
                            }
                        }

                        out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.hitboxes", hitboxs);
                    }
                }

                System.out.println("转换物品 " + namespace + ":" + key + " 完成!");
            }
            namespaceItemListHashMap.put(namespace, itemList);
        }

        // 处理贴图
        YamlConfiguration images = in.getConfigurationSection("font_images");
        if (images != null) {
            for (String key : images.getKeys()) {
                YamlConfiguration image = images.getConfigurationSection(key);
                if (image == null) {
                    continue;
                }

                System.out.println("开始转换贴图 " + namespace + ":" + key);

                {
                    out.set("images." + namespace + ":" + key + ".font", "minecraft:default");

                    Integer y_position = image.getInt("y_position");
                    if (y_position == null) {
                        y_position = 0;
                    }
                    out.set("images." + namespace + ":" + key + ".ascent", y_position);

                    String path = image.getString("path");
                    if (path != null && !path.endsWith(".png")) {
                        path = path + ".png";
                    }
                    out.set("images." + namespace + ":" + key + ".file", namespace + ":" + path);

                    Integer scale_ratio = image.getInt("scale_ratio");
                    if (scale_ratio == null && path != null) {
                        String inFilePath = inFile.getPath();
                        String prefix = inFilePath.substring(0, inFilePath.indexOf(inFilePath.split("\\\\")[2]));
                        File imageFile = null;
                        for (String resourceFolderName : resourceFolderNameList) {
                            if (resourceFolderName.equals("assets")) {
                                imageFile = new File(prefix + "/" + resourceFolderName + "/" + namespace + "/textures/" + path);
                            } else {
                                imageFile = new File(prefix + "/" + resourceFolderName + "/assets/" + namespace + "/textures/" + path);
                            }
                            if (imageFile.exists()) {
                                break;
                            }
                        }
                        if (imageFile == null) {
                            continue;
                        }
                        BufferedImage bufferedImage = ImageIO.read(imageFile);
                        scale_ratio = bufferedImage.getHeight();
                    }
                    if (scale_ratio == null) {
                        scale_ratio = 256;
                    }
                    out.set("images." + namespace + ":" + key + ".height", scale_ratio);

                    String text = unicode.getString(namespace + ":" + key);
                    if (text != null) {
                        out.set("images." + namespace + ":" + key + ".char", text);
                    }
                }

                System.out.println("转换贴图 " + namespace + ":" + key + " 完成!");
            }
        }

        // 处理语言复写
        YamlConfiguration langOverWrites = in.getConfigurationSection("minecraft_lang_overwrite");
        if (langOverWrites != null) {
            for (String key : langOverWrites.getKeys()) {
                YamlConfiguration langOverWrite = langOverWrites.getConfigurationSection(key);
                if (langOverWrite == null) {
                    continue;
                }

                System.out.println("开始转换语言文件复写 " + namespace + ":" + key);

                YamlConfiguration entries = langOverWrite.getConfigurationSection("entries");
                if (entries == null) {
                    continue;
                }

                List<String> languages = langOverWrite.getStringList("languages");
                if (languages.isEmpty() || languages.contains("all") || languages.contains("ALL")) {
                    languages = getConfig().getStringList("overwriteLang");
                }

                for (String language : languages) {
                    for (String entity : entries.getKeys()) {
                        String text = (String) entries.get(entity, false);

                        out.set("lang." + language + "." + entity, text);
                    }
                }

                System.out.println("转换语言文件复写 " + namespace + ":" + key + " 完成!");
            }
        }

        out.save(outFile);
    }

    public static void removeFiles(File directory) {
        if (!directory.exists()) {
            return;
        }

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                removeFiles(file);
                continue;
            }
            file.delete();
        }
        directory.delete();
    }

    public static List<File> listFiles(File directory) {
        if (!directory.exists()) {
            return new ArrayList<>();
        }

        List<File> fileList = new ArrayList<>();
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                fileList.addAll(listFiles(file));
                continue;
            }
            fileList.add(file);
        }
        return fileList;
    }

    public static void updateBlockUseIdHashMap() {
        File ceFolder = new File("ce");

        for (File contentFolder : Objects.requireNonNull(ceFolder.listFiles())) {
            File configFolder = new File(contentFolder, "configuration");
            if (!configFolder.exists()) {
                continue;
            }

            for (File configFile : listFiles(configFolder)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                for (String parentKey : config.getKeys()) {
                    YamlConfiguration parent = config.getConfigurationSection(parentKey);
                    if (parent == null) {
                        continue;
                    }

                    if (parentKey.contains("items")) {
                        for (String key : parent.getKeys()) {
                            YamlConfiguration item = parent.getConfigurationSection(key);
                            if (item == null) {
                                continue;
                            }

                            YamlConfiguration block = item.getConfigurationSection("behavior.block");
                            if (block == null) {
                                continue;
                            }

                            HashMap<String, List<Integer>> blockStateIdHashMap = getBlockStateIdHashmap(block);

                            for (String id : blockStateIdHashMap.keySet()) {
                                List<Integer> blockUseIdList = blockUseIdHashMap.get(id) != null ? blockUseIdHashMap.get(id)
                                        : new ArrayList<>();
                                blockUseIdList.addAll(blockStateIdHashMap.get(id));

                                blockUseIdHashMap.put(id, blockUseIdList);
                            }
                        }
                    }

                    if (parentKey.contains("blocks")) {
                        for (String key : parent.getKeys()) {
                            YamlConfiguration block = parent.getConfigurationSection(key);
                            if (block == null) {
                                continue;
                            }

                            HashMap<String, List<Integer>> blockStateIdHashMap = getBlockStateIdHashmap(block);

                            for (String id : blockStateIdHashMap.keySet()) {
                                List<Integer> blockUseIdList = blockUseIdHashMap.get(id) != null ? blockUseIdHashMap.get(id)
                                        : new ArrayList<>();
                                blockUseIdList.addAll(blockStateIdHashMap.get(id));

                                blockUseIdHashMap.put(id, blockUseIdList);
                            }
                        }
                    }
                }
            }
        }
    }

    public static HashMap<String, List<Integer>> getBlockStateIdHashmap(YamlConfiguration config) {
        HashMap<String, List<Integer>> blockStateIdHashMap = new HashMap<>();

        YamlConfiguration stateConfig = config.getConfigurationSection("state");
        if (stateConfig != null) {
            String state = stateConfig.getString("state");
            if (state != null) {
                String[] stateData = state.split(":");

                if (stateData.length == 2) {
                    try {
                        List<Integer> blockStateIdList = blockStateIdHashMap.get(stateData[0]) != null ? blockStateIdHashMap.get(stateData[0]) : new ArrayList<>();
                        blockStateIdList.add(Integer.valueOf(stateData[1]));

                        blockStateIdHashMap.put(stateData[0], blockStateIdList);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        YamlConfiguration statesConfig = config.getConfigurationSection("states");
        if (statesConfig != null) {
            YamlConfiguration arguments = statesConfig.getConfigurationSection("arguments");
            if (arguments != null) {
                String block = arguments.getString("base_block");

                if (block != null) {
                    List<Integer> blockStateIdList = blockStateIdHashMap.get(block) != null ? blockStateIdHashMap.get(block) : new ArrayList<>();
                    YamlConfiguration vanillaId = arguments.getConfigurationSection("vanilla_id");
                    if (vanillaId != null) {
                        Integer from = vanillaId.getInt("from");
                        Integer to = vanillaId.getInt("to");

                        for (int i = from.intValue(); i < to.intValue(); i++) {
                            blockStateIdList.add(Integer.valueOf(i));
                        }
                        blockStateIdList.add(to);
                    }

                    blockStateIdHashMap.put(block, blockStateIdList);
                }
            }

            YamlConfiguration appearances = statesConfig.getConfigurationSection("appearances");
            if (appearances != null) {
                for (String key : appearances.getKeys()) {
                    String state = appearances.getString(key + ".state");
                    if (state != null) {
                        String[] stateData = state.split(":");
                        if (stateData.length == 2) {
                            try {
                                List<Integer> blockStateIdList = blockStateIdHashMap.get(stateData[0]) != null ? blockStateIdHashMap.get(stateData[0]) : new ArrayList<>();
                                blockStateIdList.add(Integer.valueOf(stateData[1]));

                                blockStateIdHashMap.put(stateData[0], blockStateIdList);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }

        return blockStateIdHashMap;
    }

    public static int getNextBlockId(String type) {
        int blockId = blockIdHashMap.get(type) != null ? blockIdHashMap.get(type) : 0;
        if (blockUseIdHashMap.get(type) != null && blockUseIdHashMap.get(type).contains(blockId)) {
            blockIdHashMap.put(type, blockId + 1);
            return getNextBlockId(type);
        }

        blockIdHashMap.put(type, blockId + 1);
        return blockId;
    }
}