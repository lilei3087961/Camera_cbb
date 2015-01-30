/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.util;

import com.android.gallery3d.common.OurEnvironment;

import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;

import java.util.Comparator;

public class MediaSetUtils {
    public static final Comparator<MediaSet> NAME_COMPARATOR = new NameComparator();

    public static final int CAMERA_BUCKET_ID = GalleryUtils.getBucketId(
            OurEnvironment.getSdcardStorageDirectory().toString() + "/DCIM/Camera");
    public static final int CAMERA_INTERNAL_BUCKET_ID = GalleryUtils.getBucketId(
            OurEnvironment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
    public static final int DOWNLOAD_BUCKET_ID = GalleryUtils.getBucketId(
            OurEnvironment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.DOWNLOAD);
    public static final int EDITED_ALBUM_BUCKET_ID = GalleryUtils.getBucketId(
            OurEnvironment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.EDITED_ALBUM);
    public static final int IMPORTED_BUCKET_ID = GalleryUtils.getBucketId(
            OurEnvironment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.IMPORTED);
    public static final int SNAPSHOT_BUCKET_ID = GalleryUtils.getBucketId(
            OurEnvironment.getExternalStorageDirectory().toString() +
            "/Pictures/Screenshots");

    private static final Path[] CAMERA_PATHS = {
            Path.fromString("/local/all/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/image/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/video/" + CAMERA_BUCKET_ID),
            Path.fromString("/local/all/" + CAMERA_INTERNAL_BUCKET_ID),
            Path.fromString("/local/image/" + CAMERA_INTERNAL_BUCKET_ID),
            Path.fromString("/local/video/" + CAMERA_INTERNAL_BUCKET_ID)
            };

    public static boolean isCameraSource(Path path) {
        return CAMERA_PATHS[0] == path || CAMERA_PATHS[1] == path
                || CAMERA_PATHS[2] == path || CAMERA_PATHS[3] == path
                || CAMERA_PATHS[4] == path || CAMERA_PATHS[5] == path;
    }

    // Sort MediaSets by name
    public static class NameComparator implements Comparator<MediaSet> {
        public int compare(MediaSet set1, MediaSet set2) {
            int result = set1.getName().compareToIgnoreCase(set2.getName());
            if (result != 0) return result;
            return set1.getPath().toString().compareTo(set2.getPath().toString());
        }
    }
}
