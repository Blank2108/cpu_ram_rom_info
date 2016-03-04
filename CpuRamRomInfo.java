/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.settings.InstrumentedPreferenceActivity;
import com.android.settings.R;
import com.android.settings.Utils;

import java.lang.ref.WeakReference;
import java.lang.Runtime;
import java.lang.Long;
import java.lang.Math;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
/**
 * Display the following information
 * # CPU info
 * # RAM info
 * # ROM info
 * @author n006950@archermind.com
 */
public class CpuRamRomInfo extends InstrumentedPreferenceActivity {

    private static final String KEY_CPU_INFO = "cpu_info";
    private static final String KEY_RAM_INFO = "ram_info";
    private static final String KEY_ROM_INFO = "rom_info";

    private Resources mRes;

    private String mUnknown;
    private String mUnavailable;

    private Preference mCPUInfo;
    private Preference mRAMInfo;
    private Preference mROMInfo;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.cpu_ram_rom_info);
        mCPUInfo = findPreference(KEY_CPU_INFO);
        mRAMInfo = findPreference(KEY_RAM_INFO);
        mROMInfo = findPreference(KEY_ROM_INFO);
        mRes = getResources();
        mUnknown = mRes.getString(R.string.device_info_default);
        mUnavailable = mRes.getString(R.string.status_unavailable);
        String cpuInfo = getCoreNum() + mRes.getString(R.string.core_num) + getMaxCpuFreq() +mRes.getString(R.string.cpu_freq_hz); 
        mCPUInfo.setSummary(cpuInfo);
        mRAMInfo.setSummary(getMemorySize());
        String romInfo = mRes.getString(R.string.total_capacity) + getTotalInternalMemorySize();
        mROMInfo.setSummary(romInfo);
        getListView().setOnItemLongClickListener(
            new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {
                ListAdapter listAdapter = (ListAdapter) parent.getAdapter();
                Preference pref = (Preference) listAdapter.getItem(position);

                ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(pref.getSummary());
                Toast.makeText(
                    CpuRamRomInfo.this,
                    com.android.internal.R.string.text_copied,
                    Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO_STATUS;
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
               text = mUnknown;
             }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    public static int getCoreNum() {
       return Runtime.getRuntime().availableProcessors();
    }
    public static String getMaxCpuFreq() {
        String result = "";
        String res = "";
        ProcessBuilder cmd;
        try {
            String[] args = {
                "/system/bin/cat","/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result = result + new String(re);
            }
            //res = convertFileSize(Long.parseLong(result));
            res = (double)(Math.round(Integer.parseInt(result.trim())/10000)/100.0) + "";
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            res = "N/A";
        }
        return res;
    }

    public String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return convertFileSize(blockSize * totalBlocks);
    }

    public String getMemorySize() {
        long mTotal;
        // /proc/meminfo读出的内核信息进行解释
        String path = "/proc/meminfo";
        String content = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path), 8);
            String line;
            if ((line = br.readLine()) != null) {
                content = line;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // beginIndex
        int begin = content.indexOf(':');
        // endIndex
        int end = content.indexOf('k');
        // 截取字符串信息
        content = content.substring(begin + 1, end).trim();
        return convertFileSize(Long.parseLong(content) * 1024);
    }
    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ?"%.0f MB":"%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ?"%.0f KB":"%.1f KB", f);
        } else
            return String.format("%d B", size);
    }
}
