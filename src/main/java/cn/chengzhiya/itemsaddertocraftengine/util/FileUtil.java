package cn.chengzhiya.itemsaddertocraftengine.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FileUtil {
    /**
     * 删除指定目录文件实例
     *
     * @param directory 目录文件实例
     */
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

    /**
     * 获取指定目录文件实例下所有文件实例列表
     *
     * @param directory 目录文件实例
     * @return 文件实例列表
     */
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
}
