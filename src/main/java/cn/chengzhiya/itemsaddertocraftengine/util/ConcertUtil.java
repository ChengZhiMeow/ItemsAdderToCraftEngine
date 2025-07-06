package cn.chengzhiya.itemsaddertocraftengine.util;

import cn.chengzhiya.itemsaddertocraftengine.Main;
import cn.chengzhiya.itemsaddertocraftengine.entity.YamlConfiguration;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ConcertUtil {
    @Getter
    private static final HashMap<String, String> namespaceLangHashMap = new HashMap<>();
    @Getter
    private static final HashMap<String, List<String>> namespaceItemListHashMap = new HashMap<>();

    /**
     * 转换语言配置
     *
     * @param inFile  输入配置实例
     * @param outFile 输出配置实例
     */
    public static void concertLangConfig(File inFile, File outFile) {
        YamlConfiguration in = YamlConfiguration.loadConfiguration(inFile);
        YamlConfiguration out = YamlConfiguration.loadConfiguration(outFile);

        String language = in.getString("info.dictionary-lang");

        // 语言配置
        YamlConfiguration dictionary = in.getConfigurationSection("dictionary");
        if (dictionary != null) {
            for (String key : dictionary.getKeys()) {
                String text = dictionary.getString(key);
                if (text == null) {
                    return;
                }

                out.set("i18n." + language + "." + key, text);
                getNamespaceLangHashMap().put(key, text);
            }
        }

        out.save(outFile);
    }

    /**
     * 转换配置
     *
     * @param inFile  输入配置实例
     * @param outFile 输出配置实例
     */
    public static void concertConfig(File inFile, File outFile) throws IOException {
        File idFile = new File("items_ids_cache.yml");
        File unicodeFile = new File("font_images_unicode_cache.yml");

        YamlConfiguration id = YamlConfiguration.loadConfiguration(idFile);
        YamlConfiguration unicode = YamlConfiguration.loadConfiguration(unicodeFile);

        YamlConfiguration in = YamlConfiguration.loadConfiguration(inFile);
        YamlConfiguration out = YamlConfiguration.loadConfiguration(outFile);

        String namespace = in.getString("info.namespace");

        // 物品（物品、方块、家具）
        YamlConfiguration items = in.getConfigurationSection("items");
        if (items != null) {
            List<String> itemList = getNamespaceItemListHashMap().get(namespace) != null
                    ? getNamespaceItemListHashMap().get(namespace) : new ArrayList<>();
            for (String key : items.getKeys()) {
                YamlConfiguration item = items.getConfigurationSection(key);
                if (item == null) {
                    continue;
                }

                System.out.println("开始转换物品 " + namespace + ":" + key);
                {
                    itemList.add(namespace + ":" + key);

                    String model = null;

                    // 物品基础属性
                    {
                        String displayName = item.getString("display_name");
                        if (displayName != null) {
                            for (String i18n : getNamespaceLangHashMap().keySet()) {
                                displayName = displayName.replace(i18n, "<i18n:" + i18n + ">");
                            }
                            out.set("items." + namespace + ":" + key + ".data.display-name", displayName);
                        }

                        List<String> lore = item.getStringList("lore");
                        if (!lore.isEmpty()) {
                            out.set("items." + namespace + ":" + key + ".data.lore", lore.stream()
                                    .map(s -> {
                                        for (String i18n : getNamespaceLangHashMap().keySet()) {
                                            s = s.replace(i18n, "<i18n:" + i18n + ">");
                                        }

                                        return s;
                                    })
                                    .toList()
                            );
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

                    // 方块属性
                    if (item.getConfigurationSection("specific_properties.block") != null) {
                        // 方块基础属性
                        {
                            out.set("items." + namespace + ":" + key + ".behavior.type", "block_item");

                            out.set("items." + namespace + ":" + key + ".behavior.block.loot.template", ConfigUtil.getConfig().getString("lootTable.basic"));
                            out.set("items." + namespace + ":" + key + ".behavior.block.loot.arguments.item", namespace + ":" + key);

                            out.set("items." + namespace + ":" + key + ".behavior.block.settings.item", namespace + ":" + key);
                        }

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

                        // 方块状态ID
                        {
                            String type = item.getString("specific_properties.block.placed_model.type") != null
                                    ? item.getString("specific_properties.block.placed_model.type") : "REAL_NOTE";

                            String craftengineType = type.equals("REAL_WIRE")
                                    ? "tripwire" : "note_block";

                            int blockId = Main.getBlockUseIdManger().getNextBlockId(craftengineType);
                            out.set("items." + namespace + ":" + key + ".behavior.block.state.id", blockId);
                            out.set("items." + namespace + ":" + key + ".behavior.block.state.state", craftengineType + ":" + blockId);
                            out.set("items." + namespace + ":" + key + ".behavior.block.state.model.path", model);
                        }
                    }

                    // 家具属性
                    if (item.getConfigurationSection("behaviours.furniture") != null) {
                        // 家具基本属性
                        {
                            out.set("items." + namespace + ":" + key + ".behavior.type", "furniture_item");
                            out.set("items." + namespace + ":" + key + ".behavior.furniture.settings.item", namespace + ":" + key);

                            out.set("items." + namespace + ":" + key + ".behavior.block.loot.template", ConfigUtil.getConfig().getString("lootTable.furniture"));
                            out.set("items." + namespace + ":" + key + ".behavior.block.loot.arguments.item", namespace + ":" + key);

                            out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.rules.rotation", "ANY");
                            out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.rules.alignment", "CENTER");
                        }

                        List<YamlConfiguration> hitboxs = new ArrayList<>();
                        double length = item.getDouble("behaviours.furniture.hitbox.length") != null
                                ? item.getDouble("behaviours.furniture.hitbox.length") : 1;
                        double width = item.getDouble("behaviours.furniture.hitbox.width") != null
                                ? item.getDouble("behaviours.furniture.hitbox.width") : 1;
                        double height = item.getDouble("behaviours.furniture.hitbox.height") != null
                                ? item.getDouble("behaviours.furniture.hitbox.height") : 1;

                        // 位置
                        {
                            double positionX = ConfigUtil.getConfig().getString("position.x") != null
                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("position.x"), length, width, height)) : 0;
                            double positionY = ConfigUtil.getConfig().getString("position.y") != null
                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("position.y"), length, width, height)) : 0;
                            double positionZ = ConfigUtil.getConfig().getString("position.z") != null
                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("position.z"), length, width, height)) : 0;

                            double translationX = ConfigUtil.getConfig().getString("translation.x") != null
                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("translation.x"), length, width, height)) : 0;
                            double translationY = ConfigUtil.getConfig().getString("translation.y") != null
                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("translation.y"), length, width, height)) : 0;
                            double translationZ = ConfigUtil.getConfig().getString("translation.z") != null
                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("translation.z"), length, width, height)) : 0;

                            List<YamlConfiguration> elements = new ArrayList<>();
                            {
                                YamlConfiguration element = new YamlConfiguration();
                                element.set("item", namespace + ":" + key);
                                element.set("display-transform", "NONE");
                                element.set("billboard", "FIXED");
                                element.set("position", positionX + "," + positionY + "," + positionZ);
                                element.set("translation", translationX + "," + translationY + "," + translationZ);
                                elements.add(element);
                            }

                            out.set("items." + namespace + ":" + key + ".behavior.furniture.placement.ground.elements", elements);
                        }

                        // 碰撞箱
                        {
                            boolean isRightModel = width > length;
                            int maxX = (int) Math.max(width, length);
                            for (int x = isRightModel ? -(maxX - 1) : 0; x < (isRightModel ? 1 : maxX); x++) {
                                for (int y = 0; y < (int) height; y++) {
                                    for (int z = 0; z < (int) Math.min(width, length); z++) {
                                        YamlConfiguration hitbox = new YamlConfiguration();
                                        hitbox.set("position", x + "," + y + "," + z);
                                        hitbox.set("width", 1);
                                        hitbox.set("height", 1);

                                        boolean interactive = false;

                                        // 椅子
                                        if (item.getConfigurationSection("behaviours.furniture_sit") != null) {
                                            double sitHeight = ConfigUtil.getConfig().getString("sitHeight") != null
                                                    ? MathUtil.calculate(applyHitboxString(ConfigUtil.getConfig().getString("sitHeight"), length, width, height)) : 0;

                                            List<String> seats = new ArrayList<>();
                                            seats.add(x + "," + sitHeight + "," + y);
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
                }
                System.out.println("转换物品 " + namespace + ":" + key + " 完成!");
            }

            getNamespaceItemListHashMap().put(namespace, itemList);
        }

        // 贴图
        YamlConfiguration images = in.getConfigurationSection("font_images");
        if (images != null) {
            for (String key : images.getKeys()) {
                YamlConfiguration image = images.getConfigurationSection(key);
                if (image == null) {
                    continue;
                }

                System.out.println("开始转换贴图 " + namespace + ":" + key);
                {
                    // 基础属性
                    {
                        out.set("images." + namespace + ":" + key + ".font", "minecraft:default");
                    }

                    // y轴偏移
                    {
                        Integer yPosition = image.getInt("y_position") != null
                                ? image.getInt("y_position") : 0;
                        out.set("images." + namespace + ":" + key + ".ascent", yPosition);
                    }

                    // 图片路径
                    String path = image.getString("path");
                    {
                        if (path != null && !path.endsWith(".png")) {
                            path = path + ".png";
                        }
                        out.set("images." + namespace + ":" + key + ".file", namespace + ":" + path);
                    }

                    // 缩放
                    {
                        Integer scale_ratio = image.getInt("scale_ratio");
                        if (scale_ratio == null && path != null) {
                            List<File> fileList = Main.getNamespaceResourceHashMap().get(namespace);
                            if (fileList != null) {
                                File imageFile = null;
                                for (File file : fileList) {
                                    String allPath = file.getPath();
                                    if (allPath.endsWith(path)) {
                                        imageFile = file;
                                        break;
                                    }
                                }

                                if (imageFile != null) {
                                    BufferedImage bufferedImage = ImageIO.read(imageFile);
                                    scale_ratio = bufferedImage.getHeight();
                                }
                            }
                        }

                        if (scale_ratio == null) {
                            scale_ratio = 256;
                        }
                        out.set("images." + namespace + ":" + key + ".height", scale_ratio);
                    }

                    // 文本
                    {
                        String text = unicode.getString(namespace + ":" + key);
                        if (text != null) {
                            out.set("images." + namespace + ":" + key + ".char", text);
                        }
                    }
                }
                System.out.println("转换贴图 " + namespace + ":" + key + " 完成!");
            }
        }

        // 语言文件覆盖
        YamlConfiguration langOverWrites = in.getConfigurationSection("minecraft_lang_overwrite");
        if (langOverWrites != null) {
            for (String key : langOverWrites.getKeys()) {
                YamlConfiguration langOverWrite = langOverWrites.getConfigurationSection(key);
                if (langOverWrite == null) {
                    continue;
                }

                System.out.println("开始转换语言文件覆盖 " + namespace + ":" + key);
                {
                    YamlConfiguration entries = langOverWrite.getConfigurationSection("entries");
                    if (entries == null) {
                        continue;
                    }

                    List<String> languages = langOverWrite.getStringList("languages");
                    if (languages.isEmpty() || languages.contains("all") || languages.contains("ALL")) {
                        languages = ConfigUtil.getConfig().getStringList("overwriteAllLang");
                    }

                    for (String language : languages) {
                        for (String languageKey : entries.getKeys()) {
                            String text = entries.getString(languageKey);

                            out.set("lang." + language + "." + languageKey, text);
                        }
                    }
                }
                System.out.println("转换语言文件覆盖 " + namespace + ":" + key + " 完成!");
            }
        }

        out.save(outFile);
    }

    /**
     * 处理碰撞箱文本
     *
     * @param string 文本
     * @param length 长度
     * @param width  宽度
     * @param height 高度
     * @return 处理后的文本
     */
    private static String applyHitboxString(String string, double length, double width, double height) {
        if (string == null) {
            return null;
        }

        return string
                .replace("{length}", String.valueOf(length))
                .replace("{width}", String.valueOf(width))
                .replace("{height}", String.valueOf(height));
    }
}
