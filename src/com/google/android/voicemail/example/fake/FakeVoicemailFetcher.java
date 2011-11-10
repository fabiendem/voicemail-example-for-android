/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */
package com.google.android.voicemail.example.fake;

import com.google.android.voicemail.example.callbacks.Callback;
import com.google.android.voicemail.example.core.VoicemailPayload;
import com.google.android.voicemail.example.core.VoicemailPayloadImpl;
import com.google.android.voicemail.example.sms.OmtpMessage;
import com.google.android.voicemail.example.sms.OmtpParseException;
import com.google.android.voicemail.example.sms.SmsParser;
import com.google.android.voicemail.example.sms.StatusMessage;
import com.google.android.voicemail.example.sms.SyncMessage;
import com.google.android.voicemail.example.util.VoicemailFetcher;

import android.os.Environment;

import com.example.android.voicemail.common.core.Voicemail;
import com.example.android.voicemail.common.core.VoicemailImpl;
import com.example.android.voicemail.common.logging.Logger;
import com.example.android.voicemail.common.utils.CloseUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A fake VoiemailFetcher implementation that picks the content from a voicemail folder in SD card.
 * <p>
 * For each message there are up to 3 files created under the voicemail folder. Each file starts
 * with the provider specific unique message-id. The suffix of the file identifies the file type.
 * <ul>
 * <li> <b>Message file</b>. This contains the OMTP VVM SYNC message that was sent when the message
 *  was deposited into user's account. ex: d1c68196_msg </li>
 * <li> <b>Audio file</b>. The audio content of the voicemail. ex: d1c68196_audio.mp3 </li>
 * <li> <b>Read status file</b>. The existence of this file indicates that the message has been
 *  marked as read. ex: d1c68196_read </li>
 * <ul>
 * <p>
 * The message and audio files must be created using an external mechanism. The read status file
 * is created by this class when the message is marked as read on the device.
 * <p>
 */
public class FakeVoicemailFetcher implements VoicemailFetcher {
    /** Voicemail folder name, assumed to be directly under sd card root. */
    public static final String VOICEMAIL_FOLDER_NAME = "voicemail";
    public static final String MESSSAGE_FILE_SUFFIX = "_msg";
    public static final String AUDIO_FILE_SUFFIX = "_audio";
    public static final String READ_STATUS_FILE_SUFFIX = "_read";
    /** Optional separator used in msgId to indicate the audio file prefix. */
    public static final String ALTERNATIVE_AUDIO_FILE_SEPARATOR = "~~";

    private static final Logger logger = Logger.getLogger(FakeVoicemailFetcher.class);

    private final Executor mExecutor;
    /** To parse OMTP message stored in the _msg file. */
    private final SmsParser mSmsParser;
    /** The folder where fake files are stored. */
    private final File mVoicemailFolder;

    public FakeVoicemailFetcher(Executor executor, SmsParser smsParser) {
        mExecutor = executor;
        mSmsParser = smsParser;
        mVoicemailFolder = new File(Environment.getExternalStorageDirectory(),
                VOICEMAIL_FOLDER_NAME);
        logger.i("Using fake VoicemailFetcher! Voicemails would be fetcehd & synced from "
                + mVoicemailFolder.getAbsolutePath() + " folder.");
    }

    // VoicemailFetcher interface.
    @Override
    public void fetchAllVoicemails(final Callback<List<Voicemail>> callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                internalFetchAllVoicemails(callback);
            }
        });
    }

    private void internalFetchAllVoicemails(Callback<List<Voicemail>> callback) {
        List<Voicemail> voicemails = new ArrayList<Voicemail>();
        for (File msgFile : getAllMessageFiles()) {
            String providerMsgId = determineMsgIdFromMsgFileName(msgFile.getName());
            Voicemail voicemail = getVoicemailForMsg(providerMsgId);
            if (voicemail != null) {
                voicemails.add(voicemail);
            } else {
                logger.w("voicemail: " + providerMsgId + " skipped due to an error in retrieval." );
            }
        }
        callback.onSuccess(voicemails);
    }

    private Voicemail getVoicemailForMsg(String providerMsgId) {
        File msgFile = getMsgFile(providerMsgId);
        try {
            // Read the message file.
            String msg = new String(getBytesFromFile(msgFile), "UTF-8");
            boolean isRead = getReadStatusFile(providerMsgId).exists();
            return buildVoicemailObject(mSmsParser.parse(msg), isRead);
        } catch (FileNotFoundException e) {
            logger.w("Msg file not found for: " + providerMsgId);
        } catch (IOException e) {
            logger.w("Failed to read msg file: " + msgFile.getAbsolutePath());
        } catch (OmtpParseException e) {
            logger.w("Failed to parse msg file: " + msgFile.getAbsolutePath());
        }
        return null;
    }

    private Voicemail buildVoicemailObject(OmtpMessage omtpMessage, final boolean isRead) {
        final VoicemailImpl.Builder voicemailBuilder = VoicemailImpl.createEmptyBuilder();
        omtpMessage.visit(new OmtpMessage.Visitor() {
            @Override
            public void visit(StatusMessage statusMessage) {
            }
            @Override
            public void visit(SyncMessage syncMessage) {
                String msgId = syncMessage.getId();
                voicemailBuilder
                        .setTimestamp(syncMessage.getTimestampMillis())
                        .setDuration(syncMessage.getLength())
                        .setSourceData(msgId)
                        .setNumber(syncMessage.getSender())
                        .setIsRead(isRead);
            }
        });
        return voicemailBuilder.build();
    }

    @Override
    public void fetchVoicemailPayload(final String providerMsgId,
            final Callback<VoicemailPayload> callback) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                internalFetchVoicemailPayload(providerMsgId, callback);
            }
        });
    }

    private void internalFetchVoicemailPayload(String providerMsgId,
            Callback<VoicemailPayload> callback) {
        try {
            File audioFile = getAudioFile(providerMsgId);
            callback.onSuccess(new VoicemailPayloadImpl(getMimeType(audioFile),
                    getBytesFromFile(audioFile)));
        } catch (FileNotFoundException e) {
            callback.onFailure(e);
        } catch (IOException e) {
            callback.onFailure(e);
        }
    }

    private String getMimeType(File audioFile) {
        String fileExtension = getFileExtension(audioFile);
        return "audio/ " + (fileExtension != null ? fileExtension : "unknown");
    }

    //  ImapHelper interface.
    @Override
    public void markMessagesAsRead(final Callback<Void> callback, final Voicemail... voicemails) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                internalMarkMessagesAsRead(callback, voicemails);
            }
        });
    }

    private void internalMarkMessagesAsRead(Callback<Void> callback, Voicemail... voicemails) {
        for (Voicemail voicemail : voicemails) {
            File readStatusFile = getReadStatusFile(voicemail.getSourceData());
            if (!readStatusFile.exists()) {
                try {
                    readStatusFile.createNewFile();
                } catch (IOException e) {
                    callback.onFailure(e);
                    return;
                }
            }
        }
        callback.onSuccess(null);
    }

    @Override
    public void markMessagesAsDeleted(final Callback<Void> callback,
            final Voicemail... voicemails) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                internalMarkMessagesAsDeleted(callback, voicemails);
            }
        });
    }

    private void internalMarkMessagesAsDeleted(Callback<Void> callback, Voicemail... voicemails) {
        for (Voicemail voicemail : voicemails) {
            // Delete all files associated to this voicemail.
            for (File file : getAllFilesForVoicemail(voicemail.getSourceData())) {
                file.delete();
            }
        }
        callback.onSuccess(null);
    }

    // Utility methods related to file handling.
    private byte[] getBytesFromFile(File file) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4 * 1000];
            int numBytesRead = 0;
            while ((numBytesRead = inputStream.read(buffer, 0, buffer.length)) > 0) {
                byteStream.write(buffer, 0, numBytesRead);
            }
            return byteStream.toByteArray();
        } finally {
            CloseUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Returns the audio file, if one exists for this message.
     * @throws FileNotFoundException if an audio file associated to this message was not
     *         found.
     */
    private File getAudioFile(final String providerMsgId) throws FileNotFoundException {
        File audioFile = getFirstVoicemailFileThatStartsWith(providerMsgId + AUDIO_FILE_SUFFIX);
        // Try alternative audio file name if an exact match was not found.
        if (audioFile == null) {
            String[] splits = providerMsgId.split(ALTERNATIVE_AUDIO_FILE_SEPARATOR);
            if (splits.length == 2) {
                audioFile = getFirstVoicemailFileThatStartsWith(splits[1]);
            }
        }

        if (audioFile == null) {
            throw new FileNotFoundException("No audio file found for msg: " + providerMsgId);
        } else {
            return audioFile;
        }
    }

    /** Returns the message file for the requested message. */
    private File getMsgFile(String providerMsgId) {
        return new File(mVoicemailFolder, providerMsgId + MESSSAGE_FILE_SUFFIX);
    }

    /** Returns the read status file for the requested message. */
    private File getReadStatusFile(String providerMsgId) {
        return new File(mVoicemailFolder, providerMsgId + READ_STATUS_FILE_SUFFIX);
    }

    /** Returns all files that are associated to this message. */
    private List<File> getAllFilesForVoicemail(String providerMsgId) {
        return toList(mVoicemailFolder.listFiles(new PrefixFileFilter(providerMsgId)));
    }

    /**
     * Returns all message files currently stored under voicemail folder. This is typically used to
     * find a complete list of messages stored in the sdcard folder.
     */
    private List<File> getAllMessageFiles() {
        return toList(mVoicemailFolder.listFiles(new SuffixFileFilter(MESSSAGE_FILE_SUFFIX)));
    }

    /** Converts an array of objs to List. Returns an empty list if the array is null. */
    private <T> List<T> toList(T[] fileArray) {
        if (fileArray == null) {
            return new ArrayList<T>();
        } else {
            return Arrays.asList(fileArray);
        }
    }

    /** Extract the provider specific message id from the msg file name. */
    private String determineMsgIdFromMsgFileName(String msgFileName) {
        return msgFileName.substring(0, msgFileName.indexOf(MESSSAGE_FILE_SUFFIX));
    }

    private File getFirstVoicemailFileThatStartsWith(final String startsWithFilter) {
        File[] files = mVoicemailFolder.listFiles(new PrefixFileFilter(startsWithFilter));
        if (files != null && files.length != 0) {
            return files[0];
        } else {
            return null;
        }
    }

    private String getFileExtension(File file) {
        String[] parts = file.getName().split("\\.");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        } else {
            return null;
        }
    }

    // TODO: These two file filters should ideally live at a common place so that others can use
    // them too.
    /** A FileFilter that matches suffix of the file. */
    public static class SuffixFileFilter implements FileFilter {
        private final String mSuffix;
        public SuffixFileFilter(String suffix) {
            mSuffix = suffix;
        }
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(mSuffix);
        }
    }

    /** A FileFilter that matches prefix of the file. */
    public static class PrefixFileFilter implements FileFilter {
        private final String mPrefix;
        public PrefixFileFilter(String prefix) {
            mPrefix = prefix;
        }
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().startsWith(mPrefix);
        }
    }
}
