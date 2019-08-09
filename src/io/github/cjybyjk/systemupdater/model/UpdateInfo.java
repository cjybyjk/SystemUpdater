/*
 * Copyright (C) 2017 The LineageOS Project
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

/*
 * Modify:
 *     Do not extend UpdateBaseInfo
 *     Move UpdateBaseInfo to this class
 * Add:
 *     function getDownloadProgress, getRequirement...
 *     get Update objects by SHA1
 */ 
package io.github.cjybyjk.systemupdater.model;

public interface UpdateInfo {
    String getName();

    String getDescription();

    long getTimestamp();

    String getType();

    String getVersion();

    long getRequirement();

    String getDownloadUrl();

    String getFileSHA1();

    long getFileSize();

    int getStatus();

    String getFilePath();

    int getInstallProgress();

    int getDownloadProgress();
}
