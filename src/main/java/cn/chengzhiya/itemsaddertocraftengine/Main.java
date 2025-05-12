package cn.chengzhiya.itemsaddertocraftengine;

import cn.chengzhiya.itemsaddertocraftengine.entity.YamlConfiguration;
import cn.chengzhiya.itemsaddertocraftengine.manager.BlockIdManager;
import cn.chengzhiya.itemsaddertocraftengine.util.ConcertUtil;
import cn.chengzhiya.itemsaddertocraftengine.util.ConfigUtil;
import cn.chengzhiya.itemsaddertocraftengine.util.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;

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
    private static final BlockIdManager blockUseIdManger = new BlockIdManager();
    @Getter
    private static final List<String> configFolderNameList = ConfigUtil.getConfig().getStringList("configFolderNameList");
    @Getter
    private static final List<String> resourceFolderNameList = ConfigUtil.getConfig().getStringList("resourceFolderNameList");
    @Getter
    private static final HashMap<String, List<File>> namespaceConfigHashMap = new HashMap<>();
    @Getter
    private static final HashMap<String, List<File>> namespaceResourceHashMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        File inFolder = new File("in");
        // 初始化输入目录
        {
            if (!inFolder.exists()) {
                inFolder.mkdir();
            }
        }

        // 初始化输出目录
        File outFolder = new File("out");
        {
            if (outFolder.exists()) {
                FileUtil.removeFiles(outFolder);
            }
            outFolder.mkdir();
        }

        System.out.println("初始化完成!");
        System.out.println("方块状态ID占用情况: " + getBlockUseIdManger().getBlockIdListHashMap());

        {
            for (File contentFolder : Objects.requireNonNull(inFolder.listFiles())) {
                for (String configFolderName : getConfigFolderNameList()) {
                    File configFolder = new File(contentFolder, configFolderName);
                    if (!configFolder.exists()) {
                        continue;
                    }

                    for (File configFile : FileUtil.listFiles(configFolder)) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        String namespace = config.getString("info.namespace");
                        if (namespace == null) {
                            continue;
                        }

                        List<File> fileList = getNamespaceConfigHashMap().get(namespace) != null
                                ? getNamespaceConfigHashMap().get(namespace) : new ArrayList<>();
                        fileList.add(configFile);

                        getNamespaceConfigHashMap().put(namespace, fileList);
                    }
                }
            }
        }
        System.out.println("遍历到所有空间命名的配置文件 " + getNamespaceConfigHashMap());

        {
            for (File contentFolder : Objects.requireNonNull(inFolder.listFiles())) {
                for (String resourceFolderName : getResourceFolderNameList()) {
                    File resourceFolder = new File(contentFolder, resourceFolderName);
                    if (!resourceFolder.exists()) {
                        continue;
                    }

                    for (File resourceFile : FileUtil.listFiles(resourceFolder)) {
                        String allPath = resourceFile.getPath();
                        String[] path = allPath.split("\\\\");
                        if (path.length <= 4) {
                            continue;
                        }

                        String namespace = path[3].equals("assets") ? path[4] : path[3];

                        List<File> fileList = getNamespaceResourceHashMap().get(namespace) != null
                                ? getNamespaceResourceHashMap().get(namespace) : new ArrayList<>();
                        fileList.add(resourceFile);

                        getNamespaceResourceHashMap().put(namespace, fileList);
                    }
                }
            }
        }
        System.out.println("遍历到所有空间命名的资源文件 " + getNamespaceResourceHashMap());

        System.out.println("开始处理语言配置文件!");
        for (String namespace : getNamespaceConfigHashMap().keySet()) {
            System.out.println("开始处理空间ID为 " + namespace + " 的语言配置文件！");
            File namespaceFolder = new File(outFolder, namespace);
            namespaceFolder.mkdirs();

            File configFolder = new File(namespaceFolder, "configuration");
            configFolder.mkdirs();

            List<File> configList = getNamespaceConfigHashMap().get(namespace);
            for (File configFile : configList) {
                String allPath = configFile.getPath();
                String configFolderPath = allPath.split("\\\\")[2];
                String path = allPath.substring(allPath.lastIndexOf(configFolderPath) + configFolderPath.length());

                Path outPath = new File(configFolder, path).toPath();
                Files.createDirectories(outPath.getParent());

                File outFile = outPath.toFile();
                if (!outFile.exists()) {
                    outFile.createNewFile();
                    System.out.println("创建配置文件 " + outPath + " 完成!");
                }

                System.out.println("开始转换配置文件 " + configFile.getPath() + " 至 " + outFile.getPath());
                ConcertUtil.concertLangConfig(configFile, outFile);
                System.out.println("转换配置文件 " + configFile.getPath() + " 至 " + outFile.getPath() + " 完成!");
            }
        }
        System.out.println("语言配置文件处理完成!");

        System.out.println("开始处理配置文件!");
        for (String namespace : getNamespaceConfigHashMap().keySet()) {
            System.out.println("开始处理空间ID为 " + namespace + " 的配置文件！");
            File namespaceFolder = new File(outFolder, namespace);
            namespaceFolder.mkdirs();

            File configFolder = new File(namespaceFolder, "configuration");
            configFolder.mkdirs();

            List<File> configList = getNamespaceConfigHashMap().get(namespace);
            for (File configFile : configList) {
                String allPath = configFile.getPath();
                String configFolderPath = allPath.split("\\\\")[2];
                String path = allPath.substring(allPath.lastIndexOf(configFolderPath) + configFolderPath.length());

                Path outPath = new File(configFolder, path).toPath();
                Files.createDirectories(outPath.getParent());

                File outFile = outPath.toFile();
                if (!outFile.exists()) {
                    outFile.createNewFile();
                    System.out.println("创建配置文件 " + outPath + " 完成!");
                }

                System.out.println("开始转换配置文件 " + configFile.getPath() + " 至 " + outFile.getPath());
                ConcertUtil.concertConfig(configFile, outFile);
                System.out.println("转换配置文件 " + configFile.getPath() + " 至 " + outFile.getPath() + " 完成!");
            }
        }
        System.out.println("配置文件处理完成!");

        System.out.println("开始处理资源文件!");
        for (String namespace : getNamespaceResourceHashMap().keySet()) {
            System.out.println("开始处理空间ID为 " + namespace + " 的资源文件！");

            File namespaceFolder = new File(outFolder, namespace);
            namespaceFolder.mkdirs();

            File resourcepackFolder = new File(namespaceFolder, "resourcepack");
            resourcepackFolder.mkdirs();

            List<File> resourceList = getNamespaceResourceHashMap().get(namespace);
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

            for (String namespace : getNamespaceResourceHashMap().keySet()) {
                if (namespace.equals("minecraft")) {
                    continue;
                }

                System.out.println("开始处理空间ID为 " + namespace + " 的纹理图集！");
                List<File> resourceList = getNamespaceResourceHashMap().get(namespace);
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
                {
                    YamlConfiguration packInfo = YamlConfiguration.loadConfiguration(packInfoFile);
                    packInfo.set("author", ConfigUtil.getConfig().getString("packInfo.author"));
                    packInfo.set("version", ConfigUtil.getConfig().getString("packInfo.version"));
                    packInfo.set("description", "");
                    packInfo.set("namespace", contentFolder.getName());
                    packInfo.save(packInfoFile);
                }
                System.out.println("创建资源信息文件 " + packInfoFile.getPath() + " 完成!");
            }
        }
        System.out.println("创建资源信息文件完成!");

        System.out.println("开始创建物品列表文件!");
        {
            for (File contentFolder : Objects.requireNonNull(outFolder.listFiles())) {
                String namespace = contentFolder.getName();
                List<String> itemList = ConcertUtil.getNamespaceItemListHashMap().get(namespace);
                if (itemList == null || itemList.isEmpty()) {
                    continue;
                }

                File categoriesFile = new File(contentFolder, "configuration/categories.yml");
                categoriesFile.createNewFile();

                System.out.println("开始创建物品列表文件 " + categoriesFile.getPath());
                {
                    YamlConfiguration categories = YamlConfiguration.loadConfiguration(categoriesFile);
                    categories.set("categories." + namespace + ":default.name", namespace);
                    categories.set("categories." + namespace + ":default.lore", new ArrayList<>());
                    categories.set("categories." + namespace + ":default.icon", itemList.get(0));
                    categories.set("categories." + namespace + ":default.list", itemList);
                    categories.save(categoriesFile);
                }
                System.out.println("创建物品列表文件 " + categoriesFile.getPath() + " 完成!");
            }
            System.out.println("创建物品列表文件完成!");
        }
    }
}