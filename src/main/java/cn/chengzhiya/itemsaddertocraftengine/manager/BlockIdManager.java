package cn.chengzhiya.itemsaddertocraftengine.manager;

import cn.chengzhiya.itemsaddertocraftengine.entity.YamlConfiguration;
import cn.chengzhiya.itemsaddertocraftengine.util.FileUtil;
import lombok.Getter;

import java.io.File;
import java.util.*;

@Getter
public final class BlockIdManager {
    private final HashMap<String, List<Integer>> blockIdListHashMap = new HashMap<>();
    private final HashMap<String, Integer> nextBlockIdHashMap = new HashMap<>();

    public BlockIdManager() {
        intiBlockIdHashMap();
    }

    /**
     * 初始化方块状态ID的使用列表
     */
    private void intiBlockIdHashMap() {
        File ceFolder = new File("ce");

        for (File contentFolder : Objects.requireNonNull(ceFolder.listFiles())) {
            File configFolder = new File(contentFolder, "configuration");
            if (!configFolder.exists()) {
                continue;
            }

            for (File configFile : FileUtil.listFiles(configFolder)) {
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
                            initBlockIdHashMap(block);
                        }
                    }

                    if (parentKey.contains("blocks")) {
                        for (String key : parent.getKeys()) {
                            YamlConfiguration block = parent.getConfigurationSection(key);
                            initBlockIdHashMap(block);
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化指定方块配置实例的方块状态ID的使用列表
     *
     * @param block 方块配置实例
     */
    private void initBlockIdHashMap(YamlConfiguration block) {
        if (block == null) {
            return;
        }

        HashMap<String, List<Integer>> blockIdHashMap = getBlockIdHashMap(block);
        for (String id : blockIdHashMap.keySet()) {
            List<Integer> blockUseIdList = getBlockIdListHashMap().get(id) != null ? getBlockIdListHashMap().get(id)
                    : new ArrayList<>();
            blockUseIdList.addAll(blockIdHashMap.get(id));

            getBlockIdListHashMap().put(id, blockUseIdList);
        }
    }

    /**
     * 获取指定方块配置实例的方块状态ID使用列表
     *
     * @param block 方块配置实例
     * @return 方块状态ID使用列表
     */
    private HashMap<String, List<Integer>> getBlockIdHashMap(YamlConfiguration block) {
        HashMap<String, List<Integer>> blockIdHashMap = new HashMap<>();

        // 处理 state 格式
        {
            YamlConfiguration config = block.getConfigurationSection("state");
            if (config != null) {
                String state = config.getString("state");
                for (Map.Entry<String, List<Integer>> entry : getBlockIdHashMapFormState(state).entrySet()) {
                    List<Integer> blockUseIdList = blockIdHashMap.get(entry.getKey()) != null
                            ? blockIdHashMap.get(entry.getKey()) : new ArrayList<>();
                    blockUseIdList.addAll(entry.getValue());

                    blockIdHashMap.put(entry.getKey(), blockUseIdList);
                }
            }
        }

        // 处理 states 格式
        {
            YamlConfiguration config = block.getConfigurationSection("states");
            if (config != null) {
                // 处理 states.arguments 格式
                {
                    YamlConfiguration arguments = config.getConfigurationSection("arguments");
                    for (Map.Entry<String, List<Integer>> entry : getBlockIdHashMapFormArguments(arguments).entrySet()) {
                        List<Integer> blockUseIdList = blockIdHashMap.get(entry.getKey()) != null
                                ? blockIdHashMap.get(entry.getKey()) : new ArrayList<>();
                        blockUseIdList.addAll(entry.getValue());

                        blockIdHashMap.put(entry.getKey(), blockUseIdList);
                    }
                }

                // 处理 states.appearances 格式
                {
                    YamlConfiguration appearances = config.getConfigurationSection("appearances");
                    if (appearances != null) {
                        for (String key : appearances.getKeys()) {
                            String state = appearances.getString(key + ".state");
                            for (Map.Entry<String, List<Integer>> entry : getBlockIdHashMapFormState(state).entrySet()) {
                                List<Integer> blockUseIdList = blockIdHashMap.get(entry.getKey()) != null
                                        ? blockIdHashMap.get(entry.getKey()) : new ArrayList<>();
                                blockUseIdList.addAll(entry.getValue());

                                blockIdHashMap.put(entry.getKey(), blockUseIdList);
                            }
                        }
                    }
                }
            }
        }

        return blockIdHashMap;
    }

    /**
     * 获取指定状态配置文本的方块状态ID使用列表
     *
     * @param state 状态配置文本
     * @return 方块状态ID使用列表
     */
    private Map<String, List<Integer>> getBlockIdHashMapFormState(String state) {
        if (state == null) {
            return new HashMap<>();
        }

        String[] stateData = state.split(":");
        if (stateData.length == 2) {
            try {
                String blockId = stateData[0];
                int blockUseId = Integer.parseInt(stateData[1]);

                return Map.of(blockId, List.of(blockUseId));
            } catch (NumberFormatException ignored) {
            }
        }

        return new HashMap<>();
    }

    /**
     * 获取指定arguments状态配置实例的方块状态ID使用列表
     *
     * @param arguments arguments状态配置实例
     * @return 方块状态ID使用列表
     */
    private Map<String, List<Integer>> getBlockIdHashMapFormArguments(YamlConfiguration arguments) {
        if (arguments == null) {
            return new HashMap<>();
        }

        String blockId = arguments.getString("base_block");
        if (blockId == null) {
            return new HashMap<>();
        }

        YamlConfiguration vanillaId = arguments.getConfigurationSection("vanilla_id");
        if (vanillaId == null) {
            return new HashMap<>();
        }

        List<Integer> blockIdList = new ArrayList<>();
        {
            int from = vanillaId.getInt("from");
            int to = vanillaId.getInt("to");

            for (int i = from; i < to; i++) {
                blockIdList.add(i);
            }
            blockIdList.add(to);
        }

        return Map.of(blockId, blockIdList);
    }

    /**
     * 让指定方块类型的方块状态ID进一位
     *
     * @param type 方块类型
     */
    public void nextBlockId(String type) {
        int blockId = getNextBlockIdHashMap().get(type) != null
                ? getNextBlockIdHashMap().get(type) : 0;

        getNextBlockIdHashMap().put(type, blockId + 1);
    }

    /**
     * 获取指定方块类型下一个可用的方块状态ID
     *
     * @param type 方块类型
     * @return 方块状态ID
     */
    public int getNextBlockId(String type) {
        int blockId = getNextBlockIdHashMap().get(type) != null
                ? getNextBlockIdHashMap().get(type) : 0;

        List<Integer> blockIdList = getBlockIdListHashMap().get(type);
        if (blockIdList != null && blockIdList.contains(blockId)) {
            nextBlockId(type);
            return getNextBlockId(type);
        }

        nextBlockId(type);
        return blockId;
    }
}
