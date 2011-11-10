/*
 * Copyright (C) 2011 Google Inc. All Rights Reserved.
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
package com.google.android.voicemail.example.dependency;

import com.google.android.voicemail.example.sms.OmtpMessageHandler;
import com.google.android.voicemail.example.sms.OmtpMessageSender;
import com.google.android.voicemail.example.sms.SmsParser;
import com.google.android.voicemail.example.sync.SyncResolver;
import com.google.android.voicemail.example.sync.VoicemailFetcherFactory;
import com.google.android.voicemail.example.sync.VvmStore;
import com.google.android.voicemail.example.util.AccountStoreWrapper;
import com.google.android.voicemail.example.util.UserSettings;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interface for creating objects used across the application.
 * <p>
 * It is expected that only activities, services and receivers, that can only have default
 * constructor (and hence cannot accept dependencies through constructor), should depend on
 * dependency resolver
 * <p>
 * Methods starting with the prefix "get" will always return the same instance. The instance may be
 * created lazily, i.e., when first requested. The "get" methods should not have any arguments. The
 * members created here will be singletons, no methods are provided for deleting the object and
 * consequently they will all persist for the lifetime of the app, they should only be added for
 * objects that need to be shared as a single instance across multiple activities or services.
 * <p>
 * Methods starting with the prefix "create" will always return a brand-new instance. The "create"
 * methods may have arguments.
 */
public interface DependencyResolver {
    /**
     * The {@link Context} associated with the application, set during application's onCreate().
     */
    public Context getApplicationContext();

    /**
     * Returns the singleton instance of {@link ExecutorService} held by dependency resolver.
     * <p>
     * This returns an {@link ExecutorService} returned by {@link Executors#newCachedThreadPool()},
     * that creates new threads as needed, but reuses previously created threads when available.
     */
    public ExecutorService getExecutorService();

    /** Returns the singleton instance of {@link AccountStoreWrapper} held by dependency resolver. */
    public AccountStoreWrapper getAccountsStore();

    /**
     * Returns the singleton instance of {@link VoicemailFetcherFactory} held by dependency
     * resolver.
     */
    public VoicemailFetcherFactory getVoicemailFetcherFactory();

    /** Returns the singleton instance of {@link UserSettings} held by dependency resolver. */
    public UserSettings getUserSettings();

    /** Returns the singleton instance of local {@link VvmStore} held by dependency resolver. */
    public VvmStore getLocalStore();

    /** Returns the singleton instance of remote {@link VvmStore} held by dependency resolver. */
    public VvmStore getRemoteStore();

    /**
     * Creates an instance of provider specific {@SmsParser}. The provider config to be
     * used is internally determined by the dependency resolver.
     */
    public SmsParser createSmsParser();

    /**
     * Creates an instance of provider specific {@OmtpMessageSender}. The
     * provider config to be used is internally determined by the dependency resolver.
     */
    public OmtpMessageSender createOmtpMessageSender();

    /**
     * Creates an instance of provider specific {@OmtpMessageHandler}.
     */
    public OmtpMessageHandler createOmtpMessageHandler();

    /**
     * Creates an instance of provider specific sync resolver that knows which local and remote
     * store needs to be synced.
     */
    public SyncResolver createSyncResolver();
}
