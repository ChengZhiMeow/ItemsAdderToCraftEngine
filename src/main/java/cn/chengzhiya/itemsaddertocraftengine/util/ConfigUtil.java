package cn.chengzhiya.itemsaddertocraftengine.util;

import cn.chengzhiya.itemsaddertocraftengine.entity.YamlConfiguration;
import lombok.Getter;

import java.io.File;

public final class ConfigUtil {
    private static final File file = new File("config.yml");
    @Getter
    private static final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
}
