/*
 * Copyright (C) 2020 Muntashir Al-Islam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.muntashirakon.AppManager.servermanager;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;

import androidx.annotation.NonNull;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.server.common.ConfigParam;
import io.github.muntashirakon.AppManager.server.common.Constants;
import io.github.muntashirakon.AppManager.utils.IOUtils;
import io.github.muntashirakon.AppManager.utils.Utils;

@SuppressWarnings("ResultOfMethodCallIgnored")
class AssetsUtils {
    public static void copyFile(@NonNull Context context, String fileName, File destFile, boolean force) {
        try (AssetFileDescriptor openFd = context.getAssets().openFd(fileName)) {
            if (force) {
                destFile.delete();
            } else {
                if (destFile.exists()) {
                    if (destFile.length() != openFd.getLength()) {
                        destFile.delete();
                    } else {
                        return;
                    }
                }
            }

            try (FileInputStream open = openFd.createInputStream();
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buff = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
                int len;
                while ((len = open.read(buff)) != -1) {
                    fos.write(buff, 0, len);
                }
                fos.flush();
                fos.getFD().sync();
            }
            IOUtils.chmod644(destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeScript(@NonNull LocalServer.Config config) {
        try (AssetFileDescriptor openFd = config.context.getAssets().openFd(ServerConfig.EXECUTABLE_FILE_NAME);
             FileInputStream fdInputStream = openFd.createInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(fdInputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            File destFile = ServerConfig.getExecPath();
            if (destFile.exists()) {
                destFile.delete();
            }
            StringBuilder sb = new StringBuilder();
            if (config.allowBgRunning) {
                sb.append(',').append(ConfigParam.PARAM_RUN_IN_BACKGROUND).append(':').append(1);
            }
            if (BuildConfig.DEBUG) {
                sb.append(',').append(ConfigParam.PARAM_DEBUG).append(':').append(1);
            }

            String classpath = ServerConfig.getClassPath();
            String args = sb.toString();

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(destFile, false))) {
                // Set variables
                StringBuilder script = new StringBuilder();
                script.append("SERVER_NAME=").append(Constants.SERVER_NAME).append("\n")
                        .append("JAR_NAME=").append(Constants.JAR_NAME).append("\n")
                        .append("JAR_PATH=").append(classpath).append("\n")
                        .append("ARGS=").append(args).append("\n");
                String line = bufferedReader.readLine();
                while (line != null) {
                    String wl;
                    if ("%ENV_VARS%".equals(line.trim())) {
                        wl = script.toString();
                    } else wl = line;
                    bw.write(wl);
                    bw.newLine();
                    line = bufferedReader.readLine();
                }
                bw.flush();
            }
            IOUtils.chmod644(destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    static String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Utils.bytesToHex(bytes);
    }
}
